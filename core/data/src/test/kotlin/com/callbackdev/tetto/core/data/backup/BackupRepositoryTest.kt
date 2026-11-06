package com.callbackdev.tetto.core.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.callbackdev.tetto.core.data.FakePhone
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.TestDataStore
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.date
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.euros
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.domain.backup.BackupCodec
import com.callbackdev.tetto.core.domain.backup.BackupRead
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon.COFFEE
import com.callbackdev.tetto.core.model.CapIcon.GROCERIES
import com.callbackdev.tetto.core.model.CapIcon.TRAVEL
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Your data (PLANNING.md §11 Phase 4) on real Room databases and settings files: a backup made on
 * one phone and restored on a clean install of another, the restore replacing and failing whole,
 * the CSV from the same snapshot, and erasing everything. Unless a test moves it, the clock
 * stands at Thursday 5 Nov 2026, 10:00 in Rome.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    /** One installation: its database, its settings file, its repositories, on [clock] and [phone]. */
    private inner class Phone(name: String, val clock: FakeToday, phone: FakePhone) {
        val database: TettoDatabase =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TettoDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val store = TestDataStore(File(folder.root, name).apply { mkdirs() })
        val preferences = UserPreferencesDataSource(store.dataStore)
        val caps = CapsRepository(database, preferences, clock, phone)
        val data = BackupRepository(database, preferences, caps, clock, phone)

        fun close() {
            database.close()
            store.scope.cancel()
        }

        /** Every cap's state, open spends and closed lines, without the ids a restore renews. */
        suspend fun everything(): List<Triple<CapState, List<Spend>, List<ClosedPeriod>>> =
            caps.currentStates().map { state ->
                Triple(
                    state.copy(cap = state.cap.copy(id = 0)),
                    caps.openSpends(state.cap.id).map { it.copy(id = 0, capId = 0) },
                    caps.closedPeriods(state.cap.id).map { it.copy(capId = 0) },
                )
            }
    }

    private val rome = ZoneId.of("Europe/Rome")
    private lateinit var old: Phone
    private lateinit var new: Phone

    @Before
    fun setUp() {
        old = Phone("old", FakeToday(rome).apply { at("2026-11-05T10:00") }, FakePhone())
        // The new phone is in Switzerland, its weeks start on Sunday: neither may leak in.
        new =
            Phone(
                "new",
                FakeToday(rome).apply {
                    at("2026-11-05T10:00")
                },
                FakePhone(AppCurrency("CHF", 2), DayOfWeek.SUNDAY),
            )
    }

    @After
    fun tearDown() {
        old.close()
        new.close()
    }

    /**
     * A reader's month on the old phone: Coffee weekly since October, with a week under its cap and
     * one over it; Groceries monthly from the 27th; Travel yearly; spends in the open periods too.
     */
    private suspend fun aMonthOfSpending() {
        old.preferences.update {
            it.copy(
                currency = "EUR",
                monthStartDay = 27,
                weekStart = DayOfWeek.MONDAY,
                theme = ThemeMode.DARK,
                palette = AppPalette.PAPER,
                reminderOn = true,
                reminderTime = LocalTime.of(21, 0),
                onboardingCompleted = true,
            )
        }
        old.clock.at("2026-10-19T09:00")
        val coffee = old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
        val groceries = old.caps.addCap("Groceries", GROCERIES, euros(300), MONTH)
        old.caps.addCap("Travel", TRAVEL, euros(1_500), YEAR)
        old.caps.addSpend(coffee, euros(12, 40))
        old.caps.addSpend(groceries, euros(80))
        old.clock.at("2026-10-27T08:00")
        old.caps.addSpend(coffee, euros(31))
        old.caps.addSpend(groceries, euros(42, 5))
        old.clock.at("2026-11-04T19:30")
        old.caps.addSpend(coffee, euros(3, 20))
        old.clock.at("2026-11-05T10:00")
    }

    @Test
    fun `a backup restored on a clean install reproduces the caps, reserves, open spends and closed periods exactly`() =
        runTest {
            aMonthOfSpending()
            val before = old.everything()
            // The story is there to be carried: two weeks of coffee closed, one with a reserve.
            assertThat(before[0].third).hasSize(2)
            assertThat(before[0].first.cap.reserve).isEqualTo(euros(0))
            assertThat(before[1].third.single().reserveAfter).isEqualTo(euros(220))

            val text = BackupCodec.encode(old.data.backup(appVersion = "0.1.0"))
            val read = BackupCodec.decode(text) as BackupRead.Ok
            new.data.restore(read.backup)

            assertThat(new.everything()).isEqualTo(before)
            val settings = new.preferences.current()
            assertThat(settings.currency).isEqualTo("EUR")
            assertThat(settings.monthStartDay).isEqualTo(27)
            assertThat(settings.weekStart).isEqualTo(DayOfWeek.MONDAY)
            assertThat(settings.theme).isEqualTo(ThemeMode.DARK)
            assertThat(settings.palette).isEqualTo(AppPalette.PAPER)
            assertThat(settings.reminderOn).isTrue()
            assertThat(settings.reminderTime).isEqualTo(LocalTime.of(21, 0))
            assertThat(settings.onboardingCompleted).isTrue()
            // A backup of the restored phone is the same file but for when and by what it was written.
            assertThat(new.data.backup("0.1.0").copy(exportedAt = read.backup.exportedAt)).isEqualTo(read.backup)
        }

    @Test
    fun `the currency the old phone counted in travels, even where the reader never chose one`() = runTest {
        old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
        val backup = old.data.backup("0.1.0")
        assertThat(backup.currency).isEqualTo(AppCurrency("EUR", 2))
        new.data.restore(backup)
        assertThat(new.preferences.current().currency).isEqualTo("EUR")
    }

    @Test
    fun `the week the open periods were made by travels, and a phone whose weeks start elsewhere moves them`() =
        runTest {
            // The reader's week follows the phone: Monday here, Sunday on the new phone.
            val coffee = old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
            old.caps.addSpend(coffee, euros(4))
            val backup = old.data.backup("0.1.0")
            assertThat(backup.settings.weekStart).isNull()
            assertThat(backup.weekInForce).isEqualTo(DayOfWeek.MONDAY)

            new.data.restore((BackupCodec.decode(BackupCodec.encode(backup)) as BackupRead.Ok).backup)

            val state = new.caps.currentStates().single()
            assertThat(state.period).isEqualTo(DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8)))
            assertThat(state.spent).isEqualTo(euros(4))
            assertThat(new.preferences.current().weekStart).isNull()
            assertThat(new.preferences.current().weekInForce).isEqualTo(DayOfWeek.SUNDAY)
        }

    @Test
    fun `a backup closes what has ended before it is taken`() = runTest {
        old.clock.at("2026-11-02T09:00")
        val coffee = old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
        old.caps.addSpend(coffee, euros(5))
        old.clock.at("2026-11-10T09:00")
        val cap = old.data.backup("0.1.0").caps.single()
        assertThat(cap.openStart).isEqualTo(date(2026, 11, 9))
        assertThat(cap.reserve).isEqualTo(euros(15))
        assertThat(cap.closed.single().range).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 9)))
    }

    @Test
    fun `a restore replaces what the phone had, and closes what ended since the backup`() = runTest {
        old.clock.at("2026-10-26T09:00")
        val coffee = old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
        old.caps.addSpend(coffee, euros(8))
        val lastWeek = old.data.backup("0.1.0")
        new.caps.addCap("Shopping", COFFEE, euros(100), MONTH)

        new.data.restore(lastWeek)

        val states = new.caps.currentStates()
        assertThat(states.map { it.cap.name }).containsExactly("Coffee")
        // The week begun on Monday 26 Oct closes as the Monday week it was made as, with €12 left
        // over for the reserve. The reader never chose a week start, so the new phone's Sunday
        // weeks hold from then on, as a change in Settings would make them hold: the open week,
        // begun on Monday 2 Nov after a closed one, runs to the Sunday week's end, and the next is
        // whole.
        val closed = new.caps.closedPeriods(states.single().cap.id).single()
        assertThat(closed.range).isEqualTo(DateRange(date(2026, 10, 26), date(2026, 11, 2)))
        assertThat(closed.spent).isEqualTo(euros(8))
        assertThat(states.single().cap.reserve).isEqualTo(euros(12))
        assertThat(states.single().period).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 8)))
        assertThat(new.preferences.current().weekInForce).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `a restore that fails leaves the phone as it was`() = runTest {
        aMonthOfSpending()
        val backup = old.data.backup("0.1.0")
        new.preferences.update { it.copy(currency = "CHF", onboardingCompleted = true) }
        new.caps.addCap("Shopping", COFFEE, euros(100), MONTH)
        val before = new.everything()
        // A planted trigger refuses the restore's spends, halfway through its transaction.
        new.database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER planted BEFORE INSERT ON spends BEGIN SELECT RAISE(ABORT, 'planted'); END",
        )

        val failure = runCatching { new.data.restore(backup) }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(new.everything()).isEqualTo(before)
        assertThat(new.preferences.current()).isEqualTo(
            UserSettings(currency = "CHF", onboardingCompleted = true, weekInForce = DayOfWeek.SUNDAY),
        )
    }

    @Test
    fun `what belongs to the phone stays with it`() = runTest {
        new.preferences.update { it.copy(askedNotifications = true, onboardingCompleted = true, capsTipSeen = true) }
        old.preferences.update { it.copy(capsTipSeen = false) }
        new.data.restore(old.data.backup("0.1.0"))
        assertThat(new.preferences.current().askedNotifications).isTrue()
        assertThat(new.preferences.current().capsTipSeen).isTrue()
    }

    @Test
    fun `a backup does not carry what the phone has shown`() = runTest {
        old.preferences.update { it.copy(capsTipSeen = true, askedNotifications = true) }
        val settings = old.data.backup("0.1.0").settings
        assertThat(settings.capsTipSeen).isFalse()
        assertThat(settings.askedNotifications).isFalse()
    }

    @Test
    fun `the spends' table comes from the same snapshot, one row each`() = runTest {
        aMonthOfSpending()
        val table = old.data.spendsTable()
        assertThat(table.rows).isEqualTo(5)
        val lines = table.csv.removePrefix(Char(0xFEFF).toString()).split("\r\n").filter { it.isNotEmpty() }
        assertThat(lines).hasSize(6)
        assertThat(lines[1]).isEqualTo("2026-10-19,09:00,Coffee,12.40,EUR,2026-10-19,2026-10-25")
        assertThat(lines.last()).isEqualTo("2026-11-04,19:30,Coffee,3.20,EUR,2026-11-02,2026-11-08")
    }

    @Test
    fun `erasing everything leaves no cap, the first run, and the phone's own facts`() = runTest {
        aMonthOfSpending()
        old.preferences.update { it.copy(askedNotifications = true, capsTipSeen = true) }
        val lastId = old.caps.currentStates().maxOf { it.cap.id }

        old.data.erase()

        assertThat(old.caps.currentStates()).isEmpty()
        assertThat(old.database.capsDao().spendCount()).isEqualTo(0)
        assertThat(old.database.capsDao().allClosedPeriods()).isEmpty()
        // The week noted by the read after the erase is the engine's, not the reader's.
        assertThat(old.preferences.current()).isEqualTo(
            UserSettings(askedNotifications = true, capsTipSeen = true, weekInForce = DayOfWeek.MONDAY),
        )
        // An id is never given twice: a cap made after the erase is a new one.
        assertThat(old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)).isGreaterThan(lastId)
    }

    @Test
    fun `the contents count the caps and the spends as they change`() = runTest {
        old.data.contents.test {
            assertThat(awaitItem()).isEqualTo(DataContents(caps = 0, spends = 0))
            val coffee = old.caps.addCap("Coffee", COFFEE, euros(20), WEEK)
            assertThat(awaitItem()).isEqualTo(DataContents(caps = 1, spends = 0))
            old.caps.addSpend(coffee, euros(2))
            assertThat(awaitItem()).isEqualTo(DataContents(caps = 1, spends = 1))
            old.data.erase()
            assertThat(awaitItem()).isEqualTo(DataContents(caps = 0, spends = 0))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
