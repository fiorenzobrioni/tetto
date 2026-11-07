package com.callbackdev.tetto.core.data.caps

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.FakePhone
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.TestDataStore
import com.callbackdev.tetto.core.data.backup.BackupRepository
import com.callbackdev.tetto.core.data.date
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.euros
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.domain.backup.BackupCodec
import com.callbackdev.tetto.core.domain.backup.BackupRead
import com.callbackdev.tetto.core.model.CapIcon.COFFEE
import com.callbackdev.tetto.core.model.CapIcon.TRAVEL
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.ONE_OFF
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.ZoneId

/**
 * The one-off cap on a real Room database (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): made with
 * its dates, closed once at its end, ended and never added to, started again with new dates,
 * changed to and from another kind, rescaled, counted in the month, carried by a backup. Unless a
 * test moves it, the clock stands at Thursday 5 Nov 2026, 10:00 in Rome.
 */
@RunWith(AndroidJUnit4::class)
class OneOffRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: TettoDatabase
    private lateinit var store: TestDataStore
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var repository: CapsRepository

    private val clock = FakeToday(ZoneId.of("Europe/Rome"))
    private val phone = FakePhone()

    /** Lisbon, Tuesday 10 to Friday 20 Nov 2026, the 21st excluded. */
    private val lisbon = DateRange(date(2026, 11, 10), date(2026, 11, 21))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TettoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        preferences = UserPreferencesDataSource(store.dataStore)
        repository = CapsRepository(database, preferences, clock, phone)
        clock.at("2026-11-05T10:00")
    }

    @After
    fun tearDown() {
        database.close()
        store.scope.cancel()
    }

    private suspend fun state(capId: Long) = repository.currentStates().single { it.cap.id == capId }

    @Test
    fun `a one-off is made with its own dates, and what is added now counts in them`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        val spend = checkNotNull(repository.addSpend(trip, euros(240)))
        assertThat(spend.periodStart).isEqualTo(date(2026, 11, 10))
        val now = state(trip)
        assertThat(now.period).isEqualTo(lisbon)
        assertThat(now.left).isEqualTo(euros(560))
        assertThat(now.cap.reserve).isEqualTo(Money.ZERO)
    }

    @Test
    fun `it closes once at its end, then has ended and takes no spend`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        clock.at("2026-11-12T12:00")
        repository.addSpend(trip, euros(720))
        clock.at("2026-11-20T23:00")
        assertThat(state(trip).ended).isFalse()

        clock.at("2026-11-21T08:00")
        val dayAfter = state(trip)
        assertThat(dayAfter.ended).isTrue()
        assertThat(dayAfter.endedToday).isTrue()
        assertThat(dayAfter.spent).isEqualTo(euros(720))
        assertThat(dayAfter.period).isEqualTo(lisbon)
        assertThat(repository.closedPeriods(trip)).hasSize(1)
        assertThat(repository.addSpend(trip, euros(5))).isNull()
        assertThat(repository.addRefund(trip, euros(5))).isNull()
        assertThat(repository.openSpends(trip)).isEmpty()

        clock.at("2027-02-01T08:00")
        val later = state(trip)
        assertThat(later.ended).isTrue()
        assertThat(later.endedToday).isFalse()
        assertThat(repository.closedPeriods(trip)).hasSize(1)
    }

    @Test
    fun `an end already past when it is made is held to today`() = runTest {
        val trip = repository.addCap(
            "Fair",
            TRAVEL,
            euros(100),
            ONE_OFF,
            DateRange(date(2026, 11, 1), date(2026, 11, 3)),
        )
        val now = state(trip)
        assertThat(now.ended).isFalse()
        assertThat(now.period).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 6)))
    }

    @Test
    fun `again with new dates, its line stays and it runs again`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        clock.at("2026-11-12T12:00")
        repository.addSpend(trip, euros(700))
        clock.at("2026-12-01T09:00")
        assertThat(state(trip).ended).isTrue()

        val spring = DateRange(date(2027, 4, 2), date(2027, 4, 10))
        assertThat(repository.changeCap(trip, "Lisbon trip", TRAVEL, euros(900), ONE_OFF, spring)).isTrue()
        val again = state(trip)
        assertThat(again.ended).isFalse()
        assertThat(again.period).isEqualTo(spring)
        assertThat(again.left).isEqualTo(euros(900))
        assertThat(repository.closedPeriods(trip).single().spent).isEqualTo(euros(700))
        assertThat(repository.addSpend(trip, euros(50))).isNotNull()
    }

    @Test
    fun `an ended one-off turned monthly starts after its line, never before`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        clock.at("2026-11-25T09:00")
        assertThat(state(trip).ended).isTrue()
        repository.changeCap(trip, "Travel", TRAVEL, euros(100), MONTH)
        val month = state(trip)
        assertThat(month.cap.kind).isEqualTo(MONTH)
        assertThat(month.cap.endExclusive).isNull()
        assertThat(month.period).isEqualTo(DateRange(date(2026, 11, 21), date(2026, 12, 1)))
    }

    @Test
    fun `a weekly cap turned into a one-off from today keeps the days past, without its reserve`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(70), WEEK)
        clock.at("2026-11-03T09:00")
        // Monday the 2nd to Sunday the 8th; €10 on Tuesday.
        repository.addSpend(coffee, euros(10))
        clock.at("2026-11-05T10:00")
        repository.addSpend(coffee, euros(4))
        val dates = DateRange(date(2026, 11, 5), date(2026, 11, 12))
        repository.changeCap(coffee, "Coffee", COFFEE, euros(50), ONE_OFF, dates)

        val now = state(coffee)
        assertThat(now.period).isEqualTo(dates)
        assertThat(now.spent).isEqualTo(euros(4))
        assertThat(now.cap.reserve).isEqualTo(Money.ZERO)
        val kept = repository.closedPeriods(coffee).single()
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 5)))
        assertThat(kept.spent).isEqualTo(euros(10))
        // Three days of seven of €70.
        assertThat(kept.cap).isEqualTo(euros(30))
    }

    @Test
    fun `a one-off's dates moved later keep only the days past, and today's spends go with them`() = runTest {
        clock.at("2026-11-10T09:00")
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        repository.addSpend(trip, euros(100))
        clock.at("2026-11-12T10:00")
        repository.addSpend(trip, euros(30))
        val later = DateRange(date(2026, 11, 20), date(2026, 11, 28))
        repository.changeCap(trip, "Lisbon trip", TRAVEL, euros(800), ONE_OFF, later)
        val now = state(trip)
        assertThat(now.period).isEqualTo(later)
        assertThat(now.spent).isEqualTo(euros(30))
        val kept = repository.closedPeriods(trip).single()
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 10), date(2026, 11, 12)))
        assertThat(kept.spent).isEqualTo(euros(100))
    }

    @Test
    fun `a one-off whose end alone moves keeps everything`() = runTest {
        clock.at("2026-11-12T10:00")
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        repository.addSpend(trip, euros(30))
        val longer = DateRange(date(2026, 11, 10), date(2026, 11, 25))
        repository.changeCap(trip, "Lisbon trip", TRAVEL, euros(800), ONE_OFF, longer)
        assertThat(state(trip).period).isEqualTo(longer)
        assertThat(state(trip).spent).isEqualTo(euros(30))
        assertThat(repository.closedPeriods(trip)).isEmpty()
    }

    @Test
    fun `a one-off turned weekly keeps the days past as a line on its share`() = runTest {
        clock.at("2026-11-10T09:00")
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(770), ONE_OFF, lisbon)
        repository.addSpend(trip, euros(100))
        clock.at("2026-11-12T10:00")
        repository.addSpend(trip, euros(5))
        repository.changeCap(trip, "Coffee", COFFEE, euros(20), WEEK)
        val week = state(trip)
        // The week holds the one-off's start (Monday the 9th): every spend moves, nothing closes.
        assertThat(week.period).isEqualTo(DateRange(date(2026, 11, 9), date(2026, 11, 16)))
        assertThat(week.spent).isEqualTo(euros(105))
        assertThat(week.cap.endExclusive).isNull()
        assertThat(repository.closedPeriods(trip)).isEmpty()
    }

    @Test
    fun `a change of currency rescales a one-off with the rest`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800, 40), ONE_OFF, lisbon)
        repository.addSpend(trip, euros(12, 50))
        repository.changeCurrency("JPY")
        val now = state(trip)
        assertThat(now.cap.amount).isEqualTo(Money(800))
        assertThat(now.spent).isEqualTo(Money(13))
        assertThat(now.period).isEqualTo(lisbon)
    }

    @Test
    fun `the month counts a one-off's share, ended or open`() = runTest {
        val trip = repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        assertThat(repository.currentMonth().worth).isEqualTo(euros(800))
        clock.at("2026-11-25T09:00")
        assertThat(state(trip).ended).isTrue()
        assertThat(repository.currentMonth().worth).isEqualTo(euros(800))
    }

    @Test
    fun `a backup carries a one-off, open and ended, to a clean install`() = runTest {
        val data = BackupRepository(database, preferences, repository, clock, phone)
        repository.addCap("Lisbon trip", TRAVEL, euros(800), ONE_OFF, lisbon)
        val paris = repository.addCap(
            "Paris",
            TRAVEL,
            euros(300),
            ONE_OFF,
            DateRange(date(2026, 11, 5), date(2026, 11, 7)),
        )
        repository.addSpend(paris, euros(320))
        clock.at("2026-11-08T09:00")
        val text = BackupCodec.encode(data.backup(appVersion = "test"))
        val read = BackupCodec.decode(text) as BackupRead.Ok
        assertThat(read.backup.caps.map { it.endExclusive }).containsExactly(date(2026, 11, 21), date(2026, 11, 7))
        data.restore(read.backup)
        val states = repository.currentStates()
        assertThat(states.map { it.ended }).containsExactly(false, true).inOrder()
        assertThat(states[1].spent).isEqualTo(euros(320))
        assertThat(states[1].overBy).isEqualTo(euros(20))
    }
}
