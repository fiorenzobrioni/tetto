package com.callbackdev.tetto.feature.settings.data

import android.icu.text.ListFormatter
import android.net.Uri
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.data.backup.DataContents
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.components.StatusCard
import com.callbackdev.tetto.core.designsystem.components.StatusTone
import com.callbackdev.tetto.core.designsystem.components.ValueRow
import com.callbackdev.tetto.core.designsystem.format.Currencies
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.domain.backup.Backup
import com.callbackdev.tetto.feature.settings.R
import com.callbackdev.tetto.core.designsystem.R as DesignR

/** What Your data's rows can ask for. */
class DataActions(
    val writeBackup: (Uri) -> Unit = {},
    val writeSpends: (Uri) -> Unit = {},
    val readBackup: (Uri) -> Unit = {},
    val confirmRestore: () -> Unit = {},
    val cancelRestore: () -> Unit = {},
    val erase: () -> Unit = {},
    val dismissOutcome: () -> Unit = {},
    val backupFileName: () -> String = { "tetto-backup.json" },
    val spendsFileName: () -> String = { "tetto-spends.csv" },
)

/**
 * The file pickers and the erase's confirmation, held by the screen rather than by the rows: the
 * rows live in a lazy list and leave the composition when scrolled away, and a picker's answer or
 * a dialog must not leave with them.
 */
@Stable
internal class DataFiles(
    val saveBackup: () -> Unit,
    val saveSpends: () -> Unit,
    val openBackup: () -> Unit,
    internal val erasing: MutableState<Boolean>,
)

@Composable
internal fun rememberDataFiles(actions: DataActions): DataFiles {
    val erasing = rememberSaveable { mutableStateOf(false) }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(JSON)) { uri ->
        if (uri != null) actions.writeBackup(uri)
    }
    val createSpends = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV)) { uri ->
        if (uri != null) actions.writeSpends(uri)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) actions.readBackup(uri)
    }
    return remember(actions) {
        DataFiles(
            // No file app to take the request is no crash: the row simply does nothing.
            saveBackup = { runCatching { createBackup.launch(actions.backupFileName()) } },
            saveSpends = { runCatching { createSpends.launch(actions.spendsFileName()) } },
            // Some file apps call a .json file text, or nothing at all: all three are offered.
            openBackup = { runCatching { openBackup.launch(arrayOf(JSON, "text/plain", "application/octet-stream")) } },
            erasing = erasing,
        )
    }
}

/**
 * Your data's rows (PLANNING.md §11 Phase 4): the backup, the restore and the spends for a
 * spreadsheet, each saying what it carries before it is touched; erasing everything apart, below.
 * The system's file picker chooses where a file goes or comes from, so no storage permission is
 * asked. What an export or a restore came to is a card over the rows, until it is put away.
 */
@Composable
internal fun DataGroup(state: DataUiState, files: DataFiles, onDismissOutcome: () -> Unit) {
    val idle = state.task == null
    val contents = state.contents
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.outcome?.let { OutcomeCard(it, onDismissOutcome) }
        SettingsGroup {
            ValueRow(
                label = stringResource(R.string.data_backup),
                value = when (state.task) {
                    DataTask.WRITING_BACKUP -> stringResource(R.string.data_busy_writing)
                    else -> backupNote(contents)
                },
                enabled = idle,
                onClick = files.saveBackup,
                modifier = Modifier.testTag(DataTags.BACKUP),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.data_restore),
                value = when (state.task) {
                    DataTask.READING -> stringResource(R.string.data_busy_reading)
                    DataTask.RESTORING -> stringResource(R.string.data_busy_restoring)
                    else -> stringResource(R.string.data_restore_note)
                },
                enabled = idle,
                onClick = files.openBackup,
                modifier = Modifier.testTag(DataTags.RESTORE),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.data_spends),
                value = when {
                    state.task == DataTask.WRITING_SPENDS -> stringResource(R.string.data_busy_writing)
                    contents.spends == 0 -> stringResource(R.string.data_spends_none)
                    else -> stringResource(R.string.data_spends_note, spendsWords(contents.spends))
                },
                // A table with nothing in it is no file to offer.
                enabled = idle && contents.spends > 0,
                onClick = files.saveSpends,
                modifier = Modifier.testTag(DataTags.SPENDS),
            )
        }
        SettingsGroup {
            ValueRow(
                label = stringResource(R.string.data_erase),
                value = if (state.task == DataTask.ERASING) {
                    stringResource(R.string.data_busy_erasing)
                } else {
                    stringResource(R.string.data_erase_note)
                },
                enabled = idle,
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { files.erasing.value = true },
                modifier = Modifier.testTag(DataTags.ERASE),
            )
        }
    }
}

/** Your data's dialogs, drawn by the screen whatever the list is showing. */
@Composable
internal fun DataDialogs(state: DataUiState, files: DataFiles, actions: DataActions) {
    state.preview?.let { preview ->
        RestoreDialog(
            backup = preview.backup,
            here = state.contents,
            onRestore = actions.confirmRestore,
            onDismiss = actions.cancelRestore,
        )
    }
    if (files.erasing.value) {
        EraseDialog(
            here = state.contents,
            onErase = {
                files.erasing.value = false
                actions.erase()
            },
            onDismiss = { files.erasing.value = false },
        )
    }
}

@Composable
private fun backupNote(contents: DataContents): String = if (contents.isEmpty) {
    stringResource(R.string.data_backup_note_empty)
} else {
    stringResource(R.string.data_backup_note, capsWords(contents.caps), spendsWords(contents.spends))
}

@Composable
private fun capsWords(count: Int): String = pluralStringResource(R.plurals.data_caps, count, count)

@Composable
private fun spendsWords(count: Int): String = pluralStringResource(R.plurals.data_spends, count, count)

/**
 * What the file holds and what restoring it does, before anything is written: when and by which
 * Tetto it was made, its caps by name, its spends and their currency, and what it replaces here.
 * The button that replaces is in the over ink: it cannot be undone.
 */
@Composable
private fun RestoreDialog(backup: Backup, here: DataContents, onRestore: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val written = DateUtils.formatDateTime(
        context,
        backup.exportedAt.toEpochMilli(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_SHOW_TIME,
    )
    val names = backup.caps.map { it.name }
    val shown = if (names.size > MAX_NAMES) {
        names.take(MAX_NAMES - 1) + stringResource(R.string.data_restore_more, names.size - (MAX_NAMES - 1))
    } else {
        names
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.data_restore_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // At twice the text size, on a small phone, the dialog scrolls rather than hiding its buttons.
                modifier = Modifier.verticalScroll(rememberScrollState()).testTag(DataTags.PREVIEW),
            ) {
                Text(
                    if (backup.appVersion.isNotEmpty()) {
                        stringResource(R.string.data_restore_written, written, backup.appVersion)
                    } else {
                        stringResource(R.string.data_restore_written_unversioned, written)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (names.isEmpty()) {
                            stringResource(R.string.data_restore_no_caps)
                        } else {
                            pluralStringResource(
                                R.plurals.data_restore_caps,
                                names.size,
                                names.size,
                                ListFormatter.getInstance(locale).format(shown),
                            )
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    if (names.isNotEmpty()) {
                        Text(spendsWords(backup.spendCount), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(
                                R.string.data_restore_currency,
                                remember(backup.currency, locale) {
                                    Currencies.inSentence(backup.currency.code, locale)
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    if (here.isEmpty) {
                        stringResource(R.string.data_restore_settings)
                    } else {
                        pluralStringResource(R.plurals.data_restore_replaces, here.caps, here.caps)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRestore, modifier = Modifier.testTag(DataTags.CONFIRM_RESTORE)) {
                Text(
                    stringResource(R.string.data_restore_confirm),
                    color = if (here.isEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) } },
    )
}

/** Erasing everything, said in full before it is done: what goes, what stays, and that it is for good. */
@Composable
private fun EraseDialog(here: DataContents, onErase: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(TettoIcons.Trash, contentDescription = null) },
        title = { Text(stringResource(R.string.data_erase_title)) },
        text = {
            Text(
                if (here.isEmpty) {
                    stringResource(R.string.data_erase_body_empty)
                } else {
                    pluralStringResource(R.plurals.data_erase_body, here.caps, here.caps)
                },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onErase, modifier = Modifier.testTag(DataTags.CONFIRM_ERASE)) {
                Text(stringResource(R.string.data_erase_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(DesignR.string.action_cancel)) } },
    )
}

/** What the last export or restore came to, until it is put away. */
@Composable
private fun OutcomeCard(outcome: DataOutcome, onDismiss: () -> Unit) {
    val failed = outcome is DataOutcome.Failed
    val (title, body) = outcomeText(outcome)
    StatusCard(
        icon = if (failed) TettoIcons.Warning else TettoIcons.Check,
        title = title,
        body = body,
        tone = if (failed) StatusTone.PROBLEM else StatusTone.NOTE,
        dismiss = stringResource(R.string.data_ok),
        onDismiss = onDismiss,
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(DataTags.OUTCOME),
    )
}

@Composable
private fun outcomeText(outcome: DataOutcome): Pair<String, String> = when (outcome) {
    is DataOutcome.BackupWritten -> {
        val caps = capsWords(outcome.caps)
        val spends = spendsWords(outcome.spends)
        stringResource(R.string.data_saved_title) to if (outcome.file != null) {
            stringResource(R.string.data_saved_body, caps, spends, outcome.file)
        } else {
            stringResource(R.string.data_saved_body_unnamed, caps, spends)
        }
    }

    is DataOutcome.SpendsWritten -> stringResource(R.string.data_spends_saved_title) to if (outcome.file != null) {
        stringResource(R.string.data_spends_saved_body, spendsWords(outcome.rows), outcome.file)
    } else {
        stringResource(R.string.data_spends_saved_body_unnamed, spendsWords(outcome.rows))
    }

    is DataOutcome.Restored -> stringResource(R.string.data_restored_title) to if (outcome.caps == 0) {
        stringResource(R.string.data_restored_body_empty)
    } else {
        stringResource(R.string.data_restored_body, capsWords(outcome.caps), spendsWords(outcome.spends))
    }

    is DataOutcome.Failed -> when (outcome.reason) {
        DataFailure.WRITE ->
            stringResource(R.string.data_failed_write_title) to stringResource(R.string.data_failed_write_body)

        DataFailure.READ ->
            stringResource(R.string.data_failed_read_title) to stringResource(R.string.data_failed_read_body)

        DataFailure.NOT_TETTO ->
            stringResource(R.string.data_failed_not_tetto_title) to stringResource(R.string.data_failed_not_tetto_body)

        DataFailure.TOO_NEW ->
            stringResource(R.string.data_failed_too_new_title) to stringResource(R.string.data_failed_too_new_body)

        DataFailure.DAMAGED ->
            stringResource(R.string.data_failed_damaged_title) to stringResource(R.string.data_failed_damaged_body)

        DataFailure.RESTORE ->
            stringResource(R.string.data_failed_restore_title) to stringResource(R.string.data_failed_restore_body)
    }
}

/** A list of names stays a glance: up to four, and past that three and how many more. */
private const val MAX_NAMES = 4
private const val JSON = "application/json"
private const val CSV = "text/csv"

/** Hooks for the UI tests. */
object DataTags {
    const val BACKUP = "data_backup"
    const val RESTORE = "data_restore"
    const val SPENDS = "data_spends"
    const val ERASE = "data_erase"
    const val OUTCOME = "data_outcome"
    const val PREVIEW = "data_preview"
    const val CONFIRM_RESTORE = "data_confirm_restore"
    const val CONFIRM_ERASE = "data_confirm_erase"
}
