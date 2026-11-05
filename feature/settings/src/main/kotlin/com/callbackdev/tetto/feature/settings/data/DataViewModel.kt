package com.callbackdev.tetto.feature.settings.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.backup.BackupRepository
import com.callbackdev.tetto.core.data.backup.DataContents
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.domain.backup.Backup
import com.callbackdev.tetto.core.domain.backup.BackupCodec
import com.callbackdev.tetto.core.domain.backup.BackupRead
import com.callbackdev.tetto.core.domain.backup.CsvExport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** What Your data is doing right now; each disables the rows until it is done. */
enum class DataTask {
    WRITING_BACKUP,
    WRITING_SPENDS,
    READING,
    RESTORING,
    ERASING,
}

/** Why a file could not be written or taken. */
enum class DataFailure {
    /** The file could not be written: the place refused it, or ran out of room. */
    WRITE,

    /** The file could not be opened or read, or is far too large to be a backup. */
    READ,

    /** Not a file Tetto wrote. */
    NOT_TETTO,

    /** A backup from a newer Tetto: this one needs an update to read it. */
    TOO_NEW,

    /** A Tetto backup cut short, or changed by hand into something no Tetto writes. */
    DAMAGED,

    /** The backup was read and checked, and writing it failed: nothing of it was kept. */
    RESTORE,
}

/** What the last export or restore came to, said on a card until the reader puts it away. */
sealed interface DataOutcome {
    data class BackupWritten(val file: String?, val caps: Int, val spends: Int) : DataOutcome

    data class SpendsWritten(val file: String?, val rows: Int) : DataOutcome

    data class Restored(val caps: Int, val spends: Int) : DataOutcome

    data class Failed(val reason: DataFailure) : DataOutcome
}

/** A backup read and checked, waiting for the reader to say yes. */
data class RestorePreview(val backup: Backup, val file: String?)

data class DataUiState(
    val contents: DataContents = DataContents(0, 0),
    val task: DataTask? = null,
    val outcome: DataOutcome? = null,
    val preview: RestorePreview? = null,
)

/**
 * Your data (PLANNING.md §11 Phase 4): a backup to a file, the spends for a spreadsheet, a backup
 * restored, everything erased. Files come from the Storage Access Framework, so Tetto needs no
 * storage permission and sends nothing: the reader picks where a file goes and what to open. A
 * restore is read whole and checked first, and shown, and nothing is written before the reader
 * says yes (§5).
 */
@HiltViewModel
class DataViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupRepository,
    private val clock: TodaySource,
) : ViewModel() {
    private val task = MutableStateFlow<DataTask?>(null)
    private val outcome = MutableStateFlow<DataOutcome?>(null)
    private val preview = MutableStateFlow<RestorePreview?>(null)

    val state: StateFlow<DataUiState> = combine(backups.contents, task, outcome, preview, ::DataUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DataUiState())

    /** The names the system's file picker offers, dated today. */
    fun backupFileName(): String = BackupCodec.fileName(clock.today())

    fun spendsFileName(): String = CsvExport.fileName(clock.today())

    /** Writes the whole backup to [uri], a file the reader just created. */
    fun writeBackup(uri: Uri) = run(DataTask.WRITING_BACKUP) {
        val backup = backups.backup(appVersion())
        if (write(uri, BackupCodec.encode(backup))) {
            DataOutcome.BackupWritten(displayName(uri), backup.caps.size, backup.spendCount)
        } else {
            DataOutcome.Failed(DataFailure.WRITE)
        }
    }

    /** Writes every spend, one row each, to [uri]. */
    fun writeSpends(uri: Uri) = run(DataTask.WRITING_SPENDS) {
        val table = backups.spendsTable()
        if (write(uri, table.csv)) {
            DataOutcome.SpendsWritten(displayName(uri), table.rows)
        } else {
            DataOutcome.Failed(DataFailure.WRITE)
        }
    }

    /** Reads [uri] whole and, if it is a backup this version can take, shows what it holds. */
    fun readBackup(uri: Uri) = run(DataTask.READING) {
        val text = read(uri) ?: return@run DataOutcome.Failed(DataFailure.READ)
        when (val read = BackupCodec.decode(text)) {
            is BackupRead.Ok -> {
                preview.value = RestorePreview(read.backup, displayName(uri))
                null
            }

            BackupRead.NotTetto -> DataOutcome.Failed(DataFailure.NOT_TETTO)

            is BackupRead.TooNew -> DataOutcome.Failed(DataFailure.TOO_NEW)

            BackupRead.Damaged -> DataOutcome.Failed(DataFailure.DAMAGED)
        }
    }

    /** The reader said yes: this phone's caps and settings are replaced by the backup's. */
    fun confirmRestore() {
        val pending = preview.value ?: return
        preview.value = null
        run(DataTask.RESTORING) {
            try {
                backups.restore(pending.backup)
                DataOutcome.Restored(pending.backup.caps.size, pending.backup.spendCount)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The repository put everything back: say so, rather than leave the reader guessing.
                DataOutcome.Failed(DataFailure.RESTORE)
            }
        }
    }

    fun cancelRestore() {
        preview.value = null
    }

    /**
     * Erases everything, confirmed by the reader on the screen: the first run takes over once the
     * settings say it is not done, so there is no outcome to show.
     */
    fun erase() = run(DataTask.ERASING) {
        backups.erase()
        null
    }

    fun dismissOutcome() {
        outcome.value = null
    }

    /** One task at a time; its outcome replaces the last one. */
    private fun run(kind: DataTask, work: suspend () -> DataOutcome?) {
        if (task.value != null) return
        task.value = kind
        outcome.value = null
        // Off the main thread, and to its end even if Settings is left meanwhile: a file half
        // written, or a restore stopped between its settings and its caps, would be worse than
        // one finished.
        viewModelScope.launch(Dispatchers.Default) {
            try {
                outcome.value = withContext(NonCancellable) { work() }
            } finally {
                task.value = null
            }
        }
    }

    private suspend fun write(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // "wt": a file picked again is replaced, not written over its old end.
            context.contentResolver.openOutputStream(uri, "wt")
                ?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private suspend fun read(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // Far past any reader's years of spends (a year is some hundreds of kilobytes): a
                // bigger file is not a backup, and is not read whole into memory to find that out.
                val bytes = input.readNBytes(MAX_BACKUP_BYTES + 1)
                if (bytes.size > MAX_BACKUP_BYTES) null else bytes.toString(Charsets.UTF_8)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    /** The file's name as the reader's file app shows it, for the card that says where it went. */
    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment?.takeIf { uri.scheme == "file" }

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_BACKUP_BYTES = 32 * 1024 * 1024
    }
}
