package com.callbackdev.tetto.core.data.caps

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.callbackdev.tetto.core.data.FakePhone
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.TestDataStore
import com.callbackdev.tetto.core.data.date
import com.callbackdev.tetto.core.data.db.ClosedPeriodEntity
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.euros
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.domain.period.Days
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon.COFFEE
import com.callbackdev.tetto.core.model.CapIcon.GROCERIES
import com.callbackdev.tetto.core.model.CapIcon.TRAVEL
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.Duration
import java.time.ZoneId

/**
 * The caps' storage on a real Room database (PLANNING.md §11 Phase 1; §4.4, §4.5, §4.7). Unless a
 * test moves it, the clock stands at Thursday 5 Nov 2026, 10:00 in Rome.
 */
@RunWith(AndroidJUnit4::class)
class CapsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: TettoDatabase
    private lateinit var store: TestDataStore
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var repository: CapsRepository

    private val clock = FakeToday(ZoneId.of("Europe/Rome"))
    private val phone = FakePhone()
    private val dao get() = database.capsDao()

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

    private suspend fun spend(capId: Long, amount: Money) = checkNotNull(repository.addSpend(capId, amount))

    @Test
    fun `a new cap counts the period that holds today, last in the reader's order`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val groceries = repository.addCap(" Groceries ", GROCERIES, euros(300), MONTH)
        val states = repository.currentStates()
        assertThat(states.map { it.cap.id }).containsExactly(coffee, groceries).inOrder()
        assertThat(states[0].period).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 9)))
        assertThat(states[0].left).isEqualTo(euros(20))
        assertThat(states[1].cap.name).isEqualTo("Groceries")
        assertThat(states[1].period.start).isEqualTo(date(2026, 11, 1))
    }

    @Test
    fun `each committed write is a change the cards hear, a read that closes nothing is none`() = runBlocking {
        val heard = Channel<Unit>(Channel.UNLIMITED)
        val listening = launch(Dispatchers.IO) { repository.changes.collect { heard.send(Unit) } }
        delay(LISTEN_MILLIS)
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        withTimeout(WAIT_MILLIS) { heard.receive() }
        repository.currentStates()
        assertThat(withTimeoutOrNull(QUIET_MILLIS) { heard.receive() }).isNull()
        spend(coffee, euros(3))
        withTimeout(WAIT_MILLIS) { heard.receive() }
        listening.cancel()
    }

    @Test
    fun `a spend counts now, in the open period`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val added = spend(coffee, euros(3, 50))
        assertThat(added.periodStart).isEqualTo(date(2026, 11, 2))
        assertThat(added.addedAt).isEqualTo(clock.now())
        assertThat(repository.currentStates().single().left).isEqualTo(euros(16, 50))
        assertThat(repository.addSpend(capId = 999, euros(1))).isNull()
    }

    @Test
    fun `the catch-up is idempotent`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        spend(coffee, euros(2))
        clock.at("2026-11-08T09:00")
        repository.catchUp()
        val once = repository.closedPeriods(coffee)
        repository.catchUp()
        repository.currentStates()
        repository.catchUp()
        assertThat(repository.closedPeriods(coffee)).isEqualTo(once)
        // The latest first: the 6th and 7th folded, then the 5th with its spend.
        assertThat(once.map { it.periods }).containsExactly(2, 1).inOrder()
        val state = repository.currentStates().single()
        assertThat(state.period.start).isEqualTo(date(2026, 11, 8))
        assertThat(state.cap.reserve).isEqualTo(euros(3))
    }

    /** Phase 1's acceptance: one transaction (the next test), two rows, the reserve at its limit. */
    @Test
    fun `400 days without a read, on a daily cap, write two rows and leave the reserve at its limit`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        spend(coffee, euros(1))
        clock.instant = clock.instant.plus(Duration.ofDays(401))

        val state = repository.currentStates().single()

        val closed = repository.closedPeriods(coffee)
        assertThat(closed).hasSize(2)
        assertThat(closed.sumOf { it.periods }).isEqualTo(401)
        assertThat(closed.first().range.endExclusive).isEqualTo(clock.today())
        assertThat(state.period.start).isEqualTo(clock.today())
        assertThat(state.cap.reserve).isEqualTo(euros(3))
        assertThat(state.left).isEqualTo(euros(6))
    }

    @Test
    fun `the catch-up runs in one transaction, so a failure leaves nothing half written`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        val created = clock.today()
        // A line in the way of the second one the catch-up writes.
        val planted = ClosedPeriodEntity(
            capId = coffee,
            start = created.plusDays(1).toEpochDay(),
            endExclusive = created.plusDays(2).toEpochDay(),
            cap = 300,
            spent = 0,
            reserveBefore = 0,
            reserveAfter = 0,
            forgiven = 0,
            periods = 1,
        )
        dao.insertClosedPeriods(listOf(planted))
        clock.instant = clock.instant.plus(Duration.ofDays(401))

        val failure = runCatching { repository.catchUp() }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(dao.closedPeriods(coffee)).containsExactly(planted)
        assertThat(dao.cap(coffee)?.openStart).isEqualTo(created.toEpochDay())
    }

    @Test
    fun `the kind mid-period, month to week, keeps the day before the week, moves the rest, leaves the closed`() =
        runTest {
            clock.at("2026-10-20T12:00")
            val groceries = repository.addCap("Groceries", GROCERIES, euros(300), MONTH)
            val octoberSpend = spend(groceries, euros(40))
            clock.at("2026-11-03T12:00")
            val first = spend(groceries, euros(15))
            clock.at("2026-11-05T12:00")
            val second = spend(groceries, euros(10))
            val closedBefore = repository.closedPeriods(groceries)
            assertThat(closedBefore.single().reserveAfter).isEqualTo(euros(260))

            assertThat(repository.changeCap(groceries, "Groceries", GROCERIES, euros(80), WEEK)).isTrue()

            val state = repository.currentStates().single()
            assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 9)))
            assertThat(state.spent).isEqualTo(euros(25))
            // The reserve is held to the new cap at once, the limit a close would hold it to: a
            // month's €260 never rides on a week's €80 (owner, 6 Nov 2026).
            assertThat(state.cap.reserve).isEqualTo(euros(80))
            assertThat(repository.openSpends(groceries).map { it.id }).containsExactly(second.id, first.id).inOrder()
            // Sunday 1 Nov, the month's day before the week, is kept as a line of its own (a
            // thirtieth of €300), and the closed month stays as it was.
            val (kept, october) = repository.closedPeriods(groceries)
            assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 2)))
            assertThat(kept.cap).isEqualTo(euros(10))
            assertThat(kept.spent).isEqualTo(Money.ZERO)
            assertThat(october).isEqualTo(closedBefore.single())
            assertThat(dao.spend(octoberSpend.id)?.periodStart).isEqualTo(date(2026, 10, 1).toEpochDay())
        }

    @Test
    fun `the kind mid-period, week to month, with nothing closed takes the whole month`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val added = spend(coffee, euros(4))
        repository.changeCap(coffee, "Coffee", COFFEE, euros(80), MONTH)
        val state = repository.currentStates().single()
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 12, 1)))
        assertThat(repository.openSpends(coffee).single().id).isEqualTo(added.id)
        assertThat(state.spent).isEqualTo(euros(4))
    }

    @Test
    fun `the kind mid-period, to a day, keeps the days past and moves only today's spends`() = runTest {
        clock.at("2026-11-03T12:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val tuesday = spend(coffee, euros(4))
        clock.at("2026-11-05T12:00")
        val thursday = spend(coffee, euros(2))
        repository.changeCap(coffee, "Coffee", COFFEE, euros(3), DAY)

        val state = repository.currentStates().single()
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 5), date(2026, 11, 6)))
        // Today's spend only: Tuesday's never makes Thursday's day look over.
        assertThat(state.spent).isEqualTo(euros(2))
        assertThat(state.over).isFalse()
        assertThat(repository.openSpends(coffee).map { it.id }).containsExactly(thursday.id)
        // Monday to Wednesday kept as a line: three sevenths of €20, €8.57, with Tuesday's €4;
        // €4.57 left over, held to the day's €3.
        val kept = repository.closedPeriods(coffee).single()
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 5)))
        assertThat(kept.cap).isEqualTo(euros(8, 57))
        assertThat(kept.spent).isEqualTo(euros(4))
        assertThat(kept.reserveAfter).isEqualTo(euros(3))
        assertThat(kept.forgiven).isEqualTo(Money.ZERO)
        assertThat(state.cap.reserve).isEqualTo(euros(3))
        assertThat(dao.spend(tuesday.id)?.periodStart).isEqualTo(date(2026, 11, 2).toEpochDay())
    }

    @Test
    fun `the days kept close as any period, and an overspend the reserve cannot cover is forgiven`() = runTest {
        clock.at("2026-11-02T12:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(15))
        clock.at("2026-11-05T12:00")
        repository.changeCap(coffee, "Coffee", COFFEE, euros(3), DAY)
        val kept = repository.closedPeriods(coffee).single()
        assertThat(kept.cap).isEqualTo(euros(8, 57))
        assertThat(kept.forgiven).isEqualTo(euros(6, 43))
        assertThat(kept.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(repository.currentStates().single().left).isEqualTo(euros(3))
    }

    @Test
    fun `a month start moved forward keeps the days before it`() = runTest {
        clock.at("2026-11-01T10:00")
        val groceries = repository.addCap("Groceries", GROCERIES, euros(300), MONTH)
        val sunday = spend(groceries, euros(50))
        clock.at("2026-11-04T10:00")
        val wednesday = spend(groceries, euros(20))
        clock.at("2026-11-05T10:00")

        repository.changeMonthStartDay(3)

        val state = repository.currentStates().single()
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 3), date(2026, 12, 3)))
        assertThat(state.spent).isEqualTo(euros(20))
        assertThat(repository.openSpends(groceries).map { it.id }).containsExactly(wednesday.id)
        // The 1st and 2nd: two thirtieths of €300, €20, with Sunday's €50; €30 over, forgiven.
        val kept = repository.closedPeriods(groceries).single()
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 3)))
        assertThat(kept.cap).isEqualTo(euros(20))
        assertThat(kept.spent).isEqualTo(euros(50))
        assertThat(kept.forgiven).isEqualTo(euros(30))
        assertThat(dao.spend(sunday.id)?.periodStart).isEqualTo(date(2026, 11, 1).toEpochDay())
    }

    @Test
    fun `the amount lowered holds the reserve to it, raised keeps it`() = runTest {
        clock.at("2026-10-28T09:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        clock.at("2026-11-05T09:00")
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(20))

        repository.changeCap(coffee, "Coffee", COFFEE, euros(12), WEEK)
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(12))
        repository.changeCap(coffee, "Coffee", COFFEE, euros(30), WEEK)
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(12))
    }

    @Test
    fun `a day turned week after closed days starts where they ended`() = runTest {
        clock.at("2026-11-02T09:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        spend(coffee, euros(2))
        clock.at("2026-11-05T09:00")
        val today = spend(coffee, euros(1))

        repository.changeCap(coffee, "Coffee", COFFEE, euros(15), WEEK)

        val state = repository.currentStates().single()
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 5), date(2026, 11, 9)))
        assertThat(repository.openSpends(coffee).map { it.id }).containsExactly(today.id)
        assertThat(repository.closedPeriods(coffee).first().range.endExclusive).isEqualTo(date(2026, 11, 5))
        // It closes on the rule's boundary, and the next week is whole.
        clock.at("2026-11-09T09:00")
        val next = repository.currentStates().single()
        assertThat(next.period).isEqualTo(DateRange(date(2026, 11, 9), date(2026, 11, 16)))
        assertThat(repository.closedPeriods(coffee).first().range)
            .isEqualTo(DateRange(date(2026, 11, 5), date(2026, 11, 9)))
    }

    @Test
    fun `the amount mid-period applies at once, and a reserve under it stays`() = runTest {
        clock.at("2026-10-28T09:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(12))
        clock.at("2026-11-05T09:00")
        spend(coffee, euros(5))
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(8))

        repository.changeCap(coffee, "Coffee and tea", COFFEE, euros(30), WEEK)

        val state = repository.currentStates().single()
        assertThat(state.cap.name).isEqualTo("Coffee and tea")
        assertThat(state.cap.reserve).isEqualTo(euros(8))
        assertThat(state.left).isEqualTo(euros(33))
        assertThat(state.period.start).isEqualTo(date(2026, 11, 2))
    }

    @Test
    fun `the week start changed replaces the weekly caps' open periods, and only theirs`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        repository.addCap("Groceries", GROCERIES, euros(300), MONTH)
        val added = spend(coffee, euros(4))

        repository.changeWeekStart(SUNDAY)

        val (coffeeState, groceriesState) = repository.currentStates()
        assertThat(coffeeState.period).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 8)))
        assertThat(coffeeState.spent).isEqualTo(euros(4))
        assertThat(repository.openSpends(coffee).single().id).isEqualTo(added.id)
        assertThat(repository.closedPeriods(coffee)).isEmpty()
        assertThat(groceriesState.period.start).isEqualTo(date(2026, 11, 1))
        assertThat(preferences.current().weekStart).isEqualTo(SUNDAY)
    }

    @Test
    fun `the phone's week changing under a week that follows it moves the weekly open periods, once`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(4))
        assertThat(preferences.current().weekInForce).isEqualTo(MONDAY)

        // The phone's language changed, and its weeks now start on Sunday; the reader never chose.
        phone.weekStart = SUNDAY
        val state = repository.currentStates().single()
        // Moved as a change in Settings moves it, not judged by a week it was not made for (which
        // would have ended it on Sunday, a six-day week).
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 8)))
        assertThat(state.spent).isEqualTo(euros(4))
        assertThat(repository.closedPeriods(coffee)).isEmpty()
        assertThat(preferences.current().weekInForce).isEqualTo(SUNDAY)
        assertThat(preferences.current().weekStart).isNull()
        // Noted once: a second read moves nothing.
        assertThat(repository.currentStates().single().period).isEqualTo(state.period)
    }

    @Test
    fun `a spend on the stroke of midnight counts in the day its instant falls on`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        clock.at("2026-11-05T23:59:59.999")
        // The clock moves on between two readings, as a real one does.
        clock.step = Duration.ofMillis(1)
        val added = spend(coffee, euros(1))
        clock.step = Duration.ZERO
        assertThat(Days.dateOf(added.addedAt, clock.zone)).isEqualTo(added.periodStart)
    }

    @Test
    fun `the month start day changed replaces the monthly and yearly caps' open periods`() = runTest {
        val groceries = repository.addCap("Groceries", GROCERIES, euros(300), MONTH)
        repository.addCap("Travel", TRAVEL, euros(2_000), YEAR)
        repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(groceries, euros(30))

        repository.changeMonthStartDay(27)

        val (month, year, week) = repository.currentStates()
        assertThat(month.period).isEqualTo(DateRange(date(2026, 10, 27), date(2026, 11, 27)))
        assertThat(month.spent).isEqualTo(euros(30))
        assertThat(year.period).isEqualTo(DateRange(date(2026, 1, 27), date(2027, 1, 27)))
        assertThat(week.period.start).isEqualTo(date(2026, 11, 2))
        assertThat(repository.closedPeriods(groceries)).isEmpty()
        assertThat(preferences.current().monthStartDay).isEqualTo(27)
    }

    @Test
    fun `clearing the reserve, and its undo while the period is open`() = runTest {
        clock.at("2026-10-28T09:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(12))
        clock.at("2026-11-05T09:00")

        val cleared = checkNotNull(repository.clearReserve(coffee))
        assertThat(cleared.reserve).isEqualTo(euros(8))
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(Money.ZERO)
        assertThat(repository.clearReserve(coffee)).isNull()

        assertThat(repository.undoClearReserve(cleared)).isTrue()
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(8))

        // Once the period has closed, the old reserve means nothing any more.
        val again = checkNotNull(repository.clearReserve(coffee))
        clock.at("2026-11-09T09:00")
        assertThat(repository.undoClearReserve(again)).isFalse()
        assertThat(repository.currentStates().single().cap.reserve).isEqualTo(euros(20))
    }

    @Test
    fun `deleting a cap with closed periods takes its spends and lines with it`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        val tea = repository.addCap("Tea", COFFEE, euros(3), DAY)
        val added = spend(coffee, euros(2))
        spend(tea, euros(1))
        clock.at("2026-11-07T09:00")
        repository.catchUp()
        assertThat(dao.closedPeriods(coffee)).isNotEmpty()

        assertThat(repository.deleteCap(coffee)).isTrue()

        assertThat(dao.spend(added.id)).isNull()
        assertThat(dao.closedPeriods(coffee)).isEmpty()
        assertThat(repository.currentStates().map { it.cap.id }).containsExactly(tea)
        assertThat(dao.closedPeriods(tea)).isNotEmpty()
        assertThat(repository.deleteCap(coffee)).isFalse()
    }

    @Test
    fun `undo of an add deletes that spend, by its id`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val kept = spend(coffee, euros(1))
        val added = spend(coffee, euros(5))
        assertThat(repository.undoAdd(added)).isTrue()
        assertThat(repository.openSpends(coffee)).containsExactly(kept)
        assertThat(repository.undoAdd(added)).isFalse()
    }

    @Test
    fun `undo of a delete puts the spend back with its id and fields`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val added = spend(coffee, euros(5))
        val deleted = checkNotNull(repository.deleteSpend(added.id))
        assertThat(deleted).isEqualTo(added)
        assertThat(repository.openSpends(coffee)).isEmpty()

        assertThat(repository.undoDelete(deleted)).isTrue()

        assertThat(repository.openSpends(coffee)).containsExactly(added)
        assertThat(repository.undoDelete(deleted)).isFalse()
    }

    @Test
    fun `undo of an edit writes the old amount back`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val added = spend(coffee, euros(5))
        val previous = checkNotNull(repository.editSpend(added.id, euros(50)))
        assertThat(previous).isEqualTo(euros(5))
        assertThat(repository.currentStates().single().spent).isEqualTo(euros(50))

        assertThat(repository.undoEdit(added.id, previous)).isTrue()

        assertThat(repository.openSpends(coffee).single().amount).isEqualTo(euros(5))
    }

    @Test
    fun `an edit refused in a closed period, and a delete, and their undos`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        val added = spend(coffee, euros(2))
        val other = spend(coffee, euros(1))
        val deleted = checkNotNull(repository.deleteSpend(other.id))
        clock.at("2026-11-06T09:00")

        assertThat(repository.editSpend(added.id, euros(1))).isNull()
        assertThat(repository.deleteSpend(added.id)).isNull()
        assertThat(repository.undoAdd(added)).isFalse()
        // Yesterday's deletion cannot be undone into a period that has closed.
        assertThat(repository.undoDelete(deleted)).isFalse()

        assertThat(dao.spend(added.id)?.amount).isEqualTo(200)
        assertThat(dao.spend(other.id)).isNull()
        assertThat(repository.closedPeriods(coffee).single().spent).isEqualTo(euros(2))
    }

    @Test
    fun `today before the open period's start, a new spend still counts in the open period`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        clock.at("2026-11-07T09:00")
        repository.catchUp()
        // A clock set back a day: nothing reopens.
        clock.at("2026-11-06T09:00")
        val added = spend(coffee, euros(1))
        assertThat(added.periodStart).isEqualTo(date(2026, 11, 7))
        assertThat(repository.closedPeriods(coffee)).hasSize(2)
        assertThat(repository.currentStates().single().period.start).isEqualTo(date(2026, 11, 7))
    }

    @Test
    fun `a spend today across midnight and after a change of zone`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        assertThat(repository.anySpendToday()).isFalse()
        clock.at("2026-11-05T23:30")
        spend(coffee, euros(2))
        assertThat(repository.anySpendToday()).isTrue()

        clock.at("2026-11-06T00:10")
        assertThat(repository.anySpendToday()).isFalse()

        // Flown to New York: there it is still the 5th, and the spend was that day's (17:30).
        clock.zone = ZoneId.of("America/New_York")
        assertThat(clock.today()).isEqualTo(date(2026, 11, 5))
        assertThat(repository.anySpendToday()).isTrue()
    }

    @Test
    fun `the reader's month, what was added in it and in the one before, closed periods' spends too`() = runTest {
        clock.at("2026-10-20T10:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        repository.addCap("Groceries", COFFEE, euros(300), MONTH)
        spend(coffee, euros(5))
        clock.at("2026-10-31T23:30")
        spend(coffee, euros(1))

        clock.at("2026-11-05T10:00")
        spend(coffee, euros(3))
        spend(coffee, euros(4))

        val month = repository.currentMonth()
        assertThat(month.month).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 12, 1)))
        assertThat(month.added).isEqualTo(euros(7))
        assertThat(month.lastMonth).isEqualTo(DateRange(date(2026, 10, 1), date(2026, 11, 1)))
        // The October spends belong to weeks that have closed since: counted all the same.
        assertThat(month.addedLastMonth).isEqualTo(euros(6))
        // €300, and €20 a week over November's 30 days (85.71): about €386.
        assertThat(month.worth).isEqualTo(euros(386))
    }

    @Test
    fun `the reader's month follows the month start day, the salary's`() = runTest {
        clock.at("2026-10-26T10:00")
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(5))
        clock.at("2026-10-27T10:00")
        spend(coffee, euros(2))
        clock.at("2026-11-05T10:00")
        repository.changeMonthStartDay(27)

        val month = repository.currentMonth()
        assertThat(month.month).isEqualTo(DateRange(date(2026, 10, 27), date(2026, 11, 27)))
        assertThat(month.added).isEqualTo(euros(2))
        assertThat(month.addedLastMonth).isEqualTo(euros(5))
    }

    @Test
    fun `the reader's month hears a spend as it is added`() = runBlocking {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val heard = Channel<Money>(Channel.UNLIMITED)
        val listening = launch(Dispatchers.IO) { repository.yourMonth.collect { heard.send(it.added) } }
        assertThat(withTimeout(WAIT_MILLIS) { heard.receive() }).isEqualTo(Money.ZERO)
        spend(coffee, euros(3))
        assertThat(withTimeout(WAIT_MILLIS) { heard.receive() }).isEqualTo(euros(3))
        listening.cancel()
    }

    @Test
    fun `the reader's order`() = runTest {
        val a = repository.addCap("A", COFFEE, euros(1), DAY)
        val b = repository.addCap("B", COFFEE, euros(1), DAY)
        val c = repository.addCap("C", COFFEE, euros(1), DAY)
        repository.reorder(listOf(c, a, b))
        assertThat(repository.currentStates().map { it.cap.id }).containsExactly(c, a, b).inOrder()
        val d = repository.addCap("D", COFFEE, euros(1), DAY)
        assertThat(repository.currentStates().map { it.cap.id }).containsExactly(c, a, b, d).inOrder()
    }

    @Test
    fun `the first run's caps arrive together, in its order, after the reader's`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val ids = repository.addCaps(
            listOf(
                CapDraft("Groceries", euros(300), MONTH, GROCERIES),
                CapDraft(" Travel ", euros(1_200), YEAR, TRAVEL),
            ),
        )
        val states = repository.currentStates()
        assertThat(states.map { it.cap.id }).containsExactly(coffee, ids[0], ids[1]).inOrder()
        assertThat(states.map { it.cap.name }).containsExactly("Coffee", "Groceries", "Travel").inOrder()
        assertThat(states[2].period.start).isEqualTo(date(2026, 1, 1))
        assertThat(repository.addCaps(emptyList())).isEmpty()
    }

    @Test
    fun `a change of currency with the same decimals relabels and changes no figure`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        spend(coffee, euros(3, 50))
        val settings = repository.changeCurrency("CHF")
        assertThat(settings.currency).isEqualTo("CHF")
        val state = repository.currentStates().single()
        assertThat(state.cap.amount).isEqualTo(euros(20))
        assertThat(state.spent).isEqualTo(euros(3, 50))
    }

    @Test
    fun `a change of currency to one without decimals rescales every amount, and none becomes zero`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(3), DAY)
        spend(coffee, euros(1, 25))
        clock.at("2026-11-06T10:00")
        // The 5th closed: €3 spent €1.25, €1.75 to the reserve.
        val small = spend(coffee, euros(0, 40))

        repository.changeCurrency("JPY")

        assertThat(preferences.current().currency).isEqualTo("JPY")
        val cap = checkNotNull(dao.cap(coffee))
        assertThat(cap.amount).isEqualTo(3)
        assertThat(cap.reserve).isEqualTo(2)
        assertThat(dao.spend(small.id)?.amount).isEqualTo(1)
        val line = repository.closedPeriods(coffee).single()
        assertThat(listOf(line.cap, line.spent, line.reserveBefore, line.reserveAfter, line.forgiven))
            .containsExactly(Money(3), Money(1), Money.ZERO, Money(2), Money.ZERO)
            .inOrder()

        repository.changeCurrency("EUR")
        assertThat(dao.cap(coffee)?.amount).isEqualTo(300)
        assertThat(dao.spend(small.id)?.amount).isEqualTo(100)
    }

    @Test
    fun `a change of currency that cannot rescale puts the setting back and changes no figure`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        val added = spend(coffee, euros(3, 50))
        // A planted trigger refuses the rescale's writes to the spends.
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER planted BEFORE UPDATE ON spends BEGIN SELECT RAISE(ABORT, 'planted'); END",
        )

        val failure = runCatching { repository.changeCurrency("JPY") }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(preferences.current().currency).isNull()
        assertThat(dao.cap(coffee)?.amount).isEqualTo(2_000)
        assertThat(dao.spend(added.id)?.amount).isEqualTo(350)
    }

    @Test
    fun `the states follow every write and every setting`() = runTest {
        val coffee = repository.addCap("Coffee", COFFEE, euros(20), WEEK)
        repository.states.test {
            assertThat(awaitItem().single().spent).isEqualTo(Money.ZERO)
            repository.addSpend(coffee, euros(4))
            assertThat(awaitItem().single().spent).isEqualTo(euros(4))
            repository.changeWeekStart(SUNDAY)
            val moved = awaitItem().single()
            assertThat(moved.period).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 8)))
            assertThat(moved.spent).isEqualTo(euros(4))
            assertThat(repository.closedPeriods(coffee)).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        /** Room registers a new listener on its own thread: a moment before the first write. */
        const val LISTEN_MILLIS = 300L
        const val WAIT_MILLIS = 5_000L
        const val QUIET_MILLIS = 600L
    }
}
