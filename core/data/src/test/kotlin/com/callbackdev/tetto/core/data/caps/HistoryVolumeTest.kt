package com.callbackdev.tetto.core.data.caps

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.FakePhone
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.TestDataStore
import com.callbackdev.tetto.core.data.date
import com.callbackdev.tetto.core.data.db.ClosedPeriodEntity
import com.callbackdev.tetto.core.data.db.SpendEntity
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.euros
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.model.CapIcon.COFFEE
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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
 * Ten years of a daily cap, three spends a day (PLANNING.md §5, «Size»; §15, 7 Nov 2026): every
 * spend and closed line is kept, for the CSV and the backup, so what the screens, the cards and the
 * reminder read must never grow with them. Each of their queries goes straight to its rows through
 * a key or an index, and the cap's sheet reads its three lines, not ten years of them.
 */
@RunWith(AndroidJUnit4::class)
class HistoryVolumeTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: TettoDatabase
    private lateinit var store: TestDataStore
    private lateinit var repository: CapsRepository

    private val clock = FakeToday(ZoneId.of("Europe/Rome"))
    private val dao get() = database.capsDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TettoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        repository = CapsRepository(database, UserPreferencesDataSource(store.dataStore), clock, FakePhone())
        clock.at("2026-11-05T10:00")
    }

    @After
    fun tearDown() {
        database.close()
        store.scope.cancel()
    }

    /** A daily cap of €10 with a closed line and three spends for each of the 3,652 days before today. */
    private suspend fun tenYears(): Long {
        val coffee = repository.addCap("Coffee", COFFEE, euros(10), DAY)
        val today = date(2026, 11, 5)
        val first = today.minusYears(10)
        val days = generateSequence(first) { it.plusDays(1) }.takeWhile { it < today }.toList()
        val zone = ZoneId.of("Europe/Rome")
        dao.insertClosedPeriods(
            days.map { day ->
                ClosedPeriodEntity(
                    capId = coffee,
                    start = day.toEpochDay(),
                    endExclusive = day.plusDays(1).toEpochDay(),
                    cap = 1_000,
                    spent = 900,
                    reserveBefore = 0,
                    reserveAfter = 100,
                    forgiven = 0,
                    periods = 1,
                )
            },
        )
        dao.insertSpends(
            days.flatMap { day ->
                (0 until 3).map { i ->
                    SpendEntity(
                        capId = coffee,
                        amount = 300,
                        addedAt = day.atTime(8 + 4 * i, 0).atZone(zone).toInstant().toEpochMilli(),
                        periodStart = day.toEpochDay(),
                    )
                }
            },
        )
        return coffee
    }

    /** SQLite's plan for [sql], one step a line. */
    private fun plan(sql: String, vararg args: Any): List<String> =
        database.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN $sql", args).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("detail"))) }
        }

    @Test
    fun `ten years of history read as one day does`() = runTest {
        val coffee = tenYears()
        assertThat(dao.spendCount()).isEqualTo(3 * 3_652)

        val state = repository.currentStates().single()
        assertThat(state.spent).isEqualTo(euros(0))
        assertThat(dao.lastClosedEnd(coffee)).isEqualTo(date(2026, 11, 5).toEpochDay())

        val lines = repository.latestClosedPeriods(coffee, 3)
        assertThat(lines.map { it.range.start }).containsExactly(
            date(2026, 11, 4),
            date(2026, 11, 3),
            date(2026, 11, 2),
        ).inOrder()
        assertThat(repository.recentAmounts(coffee)).containsExactly(euros(3))
        // The reader's month and the one before: the month's own spends, not ten years of them.
        val month = repository.currentMonth()
        assertThat(month.added).isEqualTo(euros(3 * 4 * 3))
        assertThat(month.addedLastMonth).isEqualTo(euros(3 * 31 * 3))
    }

    @Test
    fun `every query the screens repeat goes straight to its rows`() = runTest {
        val coffee = tenYears()
        val today = date(2026, 11, 5).toEpochDay()
        val plans = mapOf(
            "the closed lines' latest" to plan(
                "SELECT * FROM closed_periods WHERE cap_id = ? ORDER BY start DESC LIMIT 3",
                coffee,
            ),
            "the last closed end" to plan(
                "SELECT end_exclusive FROM closed_periods WHERE cap_id = ? ORDER BY start DESC LIMIT 1",
                coffee,
            ),
            "the open period's spends" to plan(
                "SELECT * FROM spends WHERE cap_id = ? AND period_start = ? ORDER BY added_at DESC, id DESC",
                coffee,
                today,
            ),
            "a day's or a month's spends" to plan(
                "SELECT COALESCE(SUM(amount), 0) FROM spends WHERE added_at >= ? AND added_at < ?",
                0,
                1,
            ),
        )
        plans.forEach { (what, steps) ->
            // A SEARCH by a key or an index, never a SCAN of a whole table.
            assertWithMessage(what).that(steps.joinToString(" | ")).contains("SEARCH")
            steps.forEach { assertWithMessage(what).that(it).doesNotContain("SCAN") }
        }
        // The latest lines come in the key's own order: no sort of a cap's ten years.
        listOf("the closed lines' latest", "the last closed end").forEach { what ->
            plans.getValue(what).forEach { assertWithMessage(what).that(it).doesNotContain("TEMP B-TREE") }
        }
        // The caps' rows: per cap, one search for its open period's sum and one for its last
        // closed end (the caps themselves, a handful, are read whole).
        val rows = plan(
            """
            SELECT c.*,
                (SELECT SUM(s.amount) FROM spends s WHERE s.cap_id = c.id AND s.period_start = c.open_start) AS spent,
                (SELECT p.end_exclusive FROM closed_periods p WHERE p.cap_id = c.id ORDER BY p.start DESC LIMIT 1)
                    AS last_closed_end
            FROM caps c
            ORDER BY c.position, c.id
            """.trimIndent(),
        )
        assertThat(rows.any { it.startsWith("SEARCH s ") }).isTrue()
        assertThat(rows.any { it.startsWith("SEARCH p ") }).isTrue()
        assertThat(rows.none { it.startsWith("SCAN s") || it.startsWith("SCAN p") }).isTrue()
    }
}
