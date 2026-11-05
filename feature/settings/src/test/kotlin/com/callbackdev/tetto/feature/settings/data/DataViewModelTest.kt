package com.callbackdev.tetto.feature.settings.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.backup.BackupRepository
import com.callbackdev.tetto.core.data.backup.DataContents
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.Executors

/**
 * Your data's ViewModel on a real database and real files (PLANNING.md §11 Phase 4): a backup
 * written and read back into a preview, nothing restored before the reader says yes, a file
 * that is not a backup refused, the spends' table, and erasing everything. On real threads, as
 * the app runs it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DataViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: TettoDatabase
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var caps: CapsRepository
    private lateinit var viewModel: DataViewModel

    private val clock = object : TodaySource {
        override fun now(): Instant = Instant.parse("2026-11-05T20:14:00Z")

        override fun zone(): ZoneId = ZoneId.of("Europe/Rome")
    }

    private val phone = object : PhoneDefaults {
        override fun currency() = AppCurrency("EUR", 2)

        override fun weekStart(): DayOfWeek = DayOfWeek.MONDAY
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        database = Room.inMemoryDatabaseBuilder(context, TettoDatabase::class.java).build()
        val store = PreferenceDataStoreFactory.create(scope = io) { File(folder.root, "settings.preferences_pb") }
        preferences = UserPreferencesDataSource(store)
        caps = CapsRepository(database, preferences, clock, phone)
        viewModel = DataViewModel(context, BackupRepository(database, preferences, caps, clock, phone), clock)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
        io.cancel()
        main.close()
    }

    private fun file(name: String): File = File(folder.root, name)

    /** The first state that [holds]: specific, so a state from before the task cannot pass for its end. */
    private suspend fun await(holds: (DataUiState) -> Boolean): DataUiState =
        withTimeout(TIMEOUT) { viewModel.state.first(holds) }

    private suspend fun awaitOutcome(outcome: DataOutcome): DataUiState =
        await { it.task == null && it.outcome == outcome }

    private suspend fun aCoffee(): Long {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        caps.addSpend(coffee, Money(350))
        caps.addSpend(coffee, Money(120))
        return coffee
    }

    @Test
    fun `the files are offered dated today`() {
        assertThat(viewModel.backupFileName()).isEqualTo("tetto-backup-2026-11-05.json")
        assertThat(viewModel.spendsFileName()).isEqualTo("tetto-spends-2026-11-05.csv")
    }

    @Test
    fun `a backup written, read back, and restored only once the reader says yes`() = runBlocking<Unit> {
        aCoffee()
        val saved = file("tetto-backup-2026-11-05.json")
        viewModel.writeBackup(Uri.fromFile(saved))
        awaitOutcome(DataOutcome.BackupWritten("tetto-backup-2026-11-05.json", caps = 1, spends = 2))
        assertThat(saved.readText()).contains("\"format\": \"tetto-backup\"")

        caps.addCap("Shopping", CapIcon.SHOPPING, Money(10_000), PeriodKind.MONTH)
        viewModel.readBackup(Uri.fromFile(saved))
        val preview = checkNotNull(await { it.task == null && it.preview != null }.preview)
        assertThat(preview.backup.caps.map { it.name }).containsExactly("Coffee")
        // Read and checked, nothing written yet: the phone still has both caps.
        assertThat(caps.currentStates()).hasSize(2)

        viewModel.confirmRestore()
        val restored = awaitOutcome(DataOutcome.Restored(caps = 1, spends = 2))
        assertThat(restored.preview).isNull()
        assertThat(caps.currentStates().map { it.cap.name }).containsExactly("Coffee")
        assertThat(caps.currentStates().single().left).isEqualTo(Money(1530))
    }

    @Test
    fun `a restore put away changes nothing`() = runBlocking<Unit> {
        aCoffee()
        val saved = file("backup.json")
        viewModel.writeBackup(Uri.fromFile(saved))
        awaitOutcome(DataOutcome.BackupWritten("backup.json", caps = 1, spends = 2))
        caps.addCap("Shopping", CapIcon.SHOPPING, Money(10_000), PeriodKind.MONTH)
        viewModel.readBackup(Uri.fromFile(saved))
        await { it.preview != null }

        viewModel.cancelRestore()

        await { it.preview == null }
        assertThat(caps.currentStates()).hasSize(2)
    }

    @Test
    fun `a file that is not a backup, or is damaged, is said so and changes nothing`() = runBlocking<Unit> {
        aCoffee()
        val other = file("notes.txt").apply { writeText("Buy milk") }
        viewModel.readBackup(Uri.fromFile(other))
        awaitOutcome(DataOutcome.Failed(DataFailure.NOT_TETTO))

        val cut = file("cut.json").apply { writeText("""{"format": "tetto-backup", "version": 1, "caps": [{"na""") }
        viewModel.readBackup(Uri.fromFile(cut))
        awaitOutcome(DataOutcome.Failed(DataFailure.DAMAGED))

        val newer = file("newer.json").apply { writeText("""{"format": "tetto-backup", "version": 9}""") }
        viewModel.readBackup(Uri.fromFile(newer))
        awaitOutcome(DataOutcome.Failed(DataFailure.TOO_NEW))

        val missing = file("gone.json")
        viewModel.readBackup(Uri.fromFile(missing))
        awaitOutcome(DataOutcome.Failed(DataFailure.READ))

        assertThat(caps.currentStates()).hasSize(1)
        viewModel.dismissOutcome()
        await { it.outcome == null }
    }

    @Test
    fun `the spends for a spreadsheet, one row each`() = runBlocking<Unit> {
        aCoffee()
        val saved = file("tetto-spends-2026-11-05.csv")
        viewModel.writeSpends(Uri.fromFile(saved))
        awaitOutcome(DataOutcome.SpendsWritten("tetto-spends-2026-11-05.csv", rows = 2))
        val lines = saved.readText().removePrefix("﻿").split("\r\n").filter { it.isNotEmpty() }
        assertThat(lines).containsExactly(
            "date,time,cap,amount,currency,period_start,period_end",
            "2026-11-05,21:14,Coffee,3.50,EUR,2026-11-02,2026-11-08",
            "2026-11-05,21:14,Coffee,1.20,EUR,2026-11-02,2026-11-08",
        ).inOrder()
    }

    @Test
    fun `erasing everything leaves no cap and the first run to come`() = runBlocking<Unit> {
        aCoffee()
        preferences.update { it.copy(onboardingCompleted = true, currency = "EUR") }
        await { it.contents == DataContents(caps = 1, spends = 2) }
        viewModel.erase()
        await { it.task == null && it.contents == DataContents(caps = 0, spends = 0) }
        assertThat(caps.currentStates()).isEmpty()
        assertThat(preferences.current().onboardingCompleted).isFalse()
        assertThat(preferences.current().currency).isNull()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}
