package com.callbackdev.tetto.core.data.caps

import androidx.room.withTransaction
import com.callbackdev.tetto.core.data.db.CapEntity
import com.callbackdev.tetto.core.data.db.SpendEntity
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.db.toEntity
import com.callbackdev.tetto.core.data.db.toModel
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.settings.resolvedCurrency
import com.callbackdev.tetto.core.data.settings.rule
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.domain.cap.CatchUp
import com.callbackdev.tetto.core.domain.period.Days
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The caps, their spends and their closed periods, as the screens, the cards and the reminder read
 * and change them (PLANNING.md §4).
 *
 * **The catch-up before every read** (§4.4): every read and every write first closes, in the same
 * Room transaction, each period that ended on or before today, so nothing ever sees, or adds to, a
 * period that should have closed. Idempotent: a second run in a row writes nothing.
 *
 * **Only the open period can change**: a spend is added to its cap's open period; a correction,
 * a deletion and their undos are refused once that period has closed (§4.3). A closed period is
 * never changed and never reopened.
 *
 * **One at a time**: every read and write takes [lock], and reads the settings inside it. A change
 * of week or month start writes the settings and then moves the open periods, two stores that no
 * transaction spans; a read between the two would judge an open period by a rule it was not made
 * for, and could close it by mistake.
 */
@Singleton
class CapsRepository
@Inject
constructor(
    private val database: TettoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val clock: TodaySource,
    private val phone: PhoneDefaults,
) {
    private val dao = database.capsDao()

    private val lock = Mutex()

    /**
     * Every cap in the reader's order, with what it says today: computed again whenever a cap, a
     * spend, a closed period or a setting changes. Today is read at each computation; a page open
     * across midnight asks for a new one with [catchUp] at its own midnight tick (§4.4, Phase 2).
     */
    val states: Flow<List<CapState>> =
        combine(database.invalidationTracker.createFlow(*TettoDatabase.CAP_TABLES), preferences.settings) { _, _ -> }
            .conflate()
            .map { currentStates() }
            .distinctUntilChanged()

    /** The caps' states once, for a card's render, the reminder, the numpad over the home screen. */
    suspend fun currentStates(): List<CapState> = afterCatchUp { settings ->
        val today = clock.today()
        val currency = settings.resolvedCurrency(phone)
        dao.capRows().map { row ->
            val cap = row.cap.toModel()
            CapStates.of(
                cap = cap,
                spent = Money(row.spent),
                rule = settings.rule(cap.kind, phone),
                today = today,
                currency = currency,
                lastClosedEnd = row.lastClosedEnd?.let(LocalDate::ofEpochDay),
            )
        }
    }

    /** Closes every period that ended, in one transaction; nothing when none did. */
    suspend fun catchUp() {
        afterCatchUp { }
    }

    /** A new cap, its open period the one that holds today, last in the reader's order; its id. */
    suspend fun addCap(name: String, icon: CapIcon, amount: Money, kind: PeriodKind): Long {
        require(name.isNotBlank()) { "A cap needs a name" }
        require(amount.isPositive) { "A cap needs an amount above zero" }
        return afterCatchUp { settings ->
            dao.insertCap(
                CapEntity(
                    name = name.trim(),
                    icon = icon.key,
                    amount = amount.minor,
                    kind = kind.name,
                    reserve = 0,
                    openStart = CapChanges.openStartForNew(settings.rule(kind, phone), clock.today()).toEpochDay(),
                    position = dao.lastPosition() + 1,
                    createdAt = clock.now().toEpochMilli(),
                ),
            )
        }
    }

    /**
     * Adds [amount] to the cap [capId], now, in its open period (§4.3); the spend added, for Undo,
     * or null when the cap is gone.
     */
    suspend fun addSpend(capId: Long, amount: Money): Spend? {
        require(amount.isPositive) { "A spend is an amount above zero" }
        return afterCatchUp { _ ->
            val cap = dao.cap(capId) ?: return@afterCatchUp null
            val spend = SpendEntity(
                capId = capId,
                amount = amount.minor,
                addedAt = clock.now().toEpochMilli(),
                periodStart = cap.openStart,
            )
            spend.copy(id = dao.insertSpend(spend)).toModel()
        }
    }

    /**
     * Corrects the amount of the spend [spendId]; the amount it had, for Undo, or null when it is
     * gone or its period has closed.
     */
    suspend fun editSpend(spendId: Long, amount: Money): Money? {
        require(amount.isPositive) { "A spend is an amount above zero" }
        return afterCatchUp { _ ->
            val spend = openSpend(spendId) ?: return@afterCatchUp null
            dao.setSpendAmount(spendId, amount.minor)
            Money(spend.amount)
        }
    }

    /** Deletes the spend [spendId]; the spend deleted, for Undo, or null when it is gone or its period has closed. */
    suspend fun deleteSpend(spendId: Long): Spend? = afterCatchUp { _ ->
        val spend = openSpend(spendId) ?: return@afterCatchUp null
        dao.deleteSpend(spendId)
        spend.toModel()
    }

    /** Undoes [addSpend]: deletes that spend, by its id, while its period is open. */
    suspend fun undoAdd(spend: Spend): Boolean = deleteSpend(spend.id) != null

    /** Undoes [editSpend]: writes [previous] back, while the spend's period is open. */
    suspend fun undoEdit(spendId: Long, previous: Money): Boolean = editSpend(spendId, previous) != null

    /** Undoes [deleteSpend]: the spend again, with its id and fields, while its period is open. */
    suspend fun undoDelete(spend: Spend): Boolean = afterCatchUp { _ ->
        val cap = dao.cap(spend.capId) ?: return@afterCatchUp false
        if (cap.openStart != spend.periodStart.toEpochDay() || dao.spend(spend.id) != null) return@afterCatchUp false
        dao.insertSpend(spend.toEntity())
        true
    }

    /** The open period's spends of the cap [capId], newest first. */
    suspend fun openSpends(capId: Long): List<Spend> = afterCatchUp { _ ->
        val cap = dao.cap(capId) ?: return@afterCatchUp emptyList()
        dao.spends(capId, cap.openStart).map { it.toModel() }
    }

    /** The cap [capId]'s closed periods, the latest first: the reserve's story. */
    suspend fun closedPeriods(capId: Long): List<ClosedPeriod> = afterCatchUp { _ ->
        dao.closedPeriods(capId).map { it.toModel() }
    }

    /**
     * Changes the cap [capId] (§4.5). Its name, icon and amount change nothing else: a new amount
     * applies to the open period at once, and the reserve stays. A new [kind] replaces the open
     * period by the new one that holds today, and moves the open period's spends into it; closed
     * periods and their spends stay as they are. False when the cap is gone.
     */
    suspend fun changeCap(capId: Long, name: String, icon: CapIcon, amount: Money, kind: PeriodKind): Boolean {
        require(name.isNotBlank()) { "A cap needs a name" }
        require(amount.isPositive) { "A cap needs an amount above zero" }
        return afterCatchUp { settings ->
            val cap = dao.cap(capId)?.toModel() ?: return@afterCatchUp false
            dao.setCapLooks(capId, name.trim(), icon.key, amount.minor)
            if (kind != cap.kind) {
                val today = clock.today()
                val lastClosedEnd = dao.lastClosedEnd(capId)?.let(LocalDate::ofEpochDay)
                val start = CapChanges.replacedOpenStart(settings.rule(kind, phone), today, lastClosedEnd)
                dao.moveSpends(capId, cap.openStart.toEpochDay(), start.toEpochDay())
                dao.setCapPeriod(capId, kind.name, start.toEpochDay())
            }
            true
        }
    }

    /** Clears the cap [capId]'s reserve; what it was, for Undo, or null when the cap is gone or had none. */
    suspend fun clearReserve(capId: Long): ClearedReserve? = afterCatchUp { _ ->
        val cap = dao.cap(capId) ?: return@afterCatchUp null
        if (cap.reserve == 0L) return@afterCatchUp null
        dao.setReserve(capId, 0)
        ClearedReserve(capId, Money(cap.reserve), LocalDate.ofEpochDay(cap.openStart))
    }

    /**
     * Undoes [clearReserve], while the period it was cleared in is still open: after a close, the
     * reserve is the one that close carried, and the old one no longer means anything.
     */
    suspend fun undoClearReserve(cleared: ClearedReserve): Boolean = afterCatchUp { _ ->
        val cap = dao.cap(cleared.capId) ?: return@afterCatchUp false
        if (cap.openStart != cleared.openStart.toEpochDay() || cap.reserve != 0L) return@afterCatchUp false
        dao.setReserve(cleared.capId, cleared.reserve.minor)
        true
    }

    /** Deletes the cap [capId], its spends and its closed periods. */
    suspend fun deleteCap(capId: Long): Boolean = afterCatchUp { _ -> dao.deleteCap(capId) > 0 }

    /** Puts the caps in the order of [capIds], in one transaction. */
    suspend fun reorder(capIds: List<Long>) {
        afterCatchUp { _ -> capIds.forEachIndexed { position, id -> dao.setPosition(id, position) } }
    }

    /**
     * The first day of the week, the reader's ([weekStart]) or the locale's (null): the weekly caps'
     * open periods are replaced by the new rule's that hold today (§4.5).
     */
    suspend fun changeWeekStart(weekStart: DayOfWeek?): UserSettings = changeCalendar { it.copy(weekStart = weekStart) }

    /** The day a month starts, 1 to 31: the monthly and yearly caps' open periods are replaced (§4.5). */
    suspend fun changeMonthStartDay(day: Int): UserSettings {
        require(day in 1..31) { "A month starts on day 1 to 31" }
        return changeCalendar { it.copy(monthStartDay = day) }
    }

    /** Whether a spend was added today, in the zone of the moment of asking (§4.3; the reminder). */
    suspend fun anySpendToday(): Boolean {
        val zone = clock.zone()
        val (from, until) = Days.instantsOf(Days.dateOf(clock.now(), zone), zone)
        return dao.anySpendBetween(from.toEpochMilli(), until.toEpochMilli())
    }

    /**
     * Writes the setting, then, in one transaction, closes what ended under the old rule and moves
     * the open periods to the new one. Under [lock], so nothing reads in between. Should the process
     * die between the two writes, the next read judges the old open periods by the new rule: one
     * that the new rule has already ended closes at its boundary, the others carry on until theirs.
     */
    private suspend fun changeCalendar(transform: (UserSettings) -> UserSettings): UserSettings = lock.withLock {
        val before = preferences.current()
        val after = preferences.update(transform)
        database.withTransaction {
            val today = clock.today()
            catchUpLocked(before, today)
            for (row in dao.capRows()) {
                val cap = row.cap.toModel()
                val oldRule = before.rule(cap.kind, phone)
                val newRule = after.rule(cap.kind, phone)
                val changed = when (cap.kind) {
                    PeriodKind.DAY -> false
                    PeriodKind.WEEK -> newRule.weekStart != oldRule.weekStart
                    PeriodKind.MONTH, PeriodKind.YEAR -> newRule.monthStartDay != oldRule.monthStartDay
                }
                if (!changed) continue
                val lastClosedEnd = row.lastClosedEnd?.let(LocalDate::ofEpochDay)
                val start = CapChanges.replacedOpenStart(newRule, today, lastClosedEnd)
                if (start == cap.openStart) continue
                dao.moveSpends(cap.id, cap.openStart.toEpochDay(), start.toEpochDay())
                dao.setCapPeriod(cap.id, cap.kind.name, start.toEpochDay())
            }
        }
        after
    }

    /** A read or a write, under [lock], after the catch-up, in the same transaction. */
    private suspend fun <T> afterCatchUp(block: suspend (UserSettings) -> T): T = lock.withLock {
        val settings = preferences.current()
        database.withTransaction {
            catchUpLocked(settings, clock.today())
            block(settings)
        }
    }

    /** The spend [spendId], if it counts in its cap's open period: the only one that can change. */
    private suspend fun openSpend(spendId: Long): SpendEntity? {
        val spend = dao.spend(spendId) ?: return null
        val cap = dao.cap(spend.capId) ?: return null
        return spend.takeIf { it.periodStart == cap.openStart }
    }

    /** Runs inside a transaction: closes what ended for every cap (§4.4). */
    private suspend fun catchUpLocked(settings: UserSettings, today: LocalDate) {
        for (row in dao.capRows()) {
            val cap = row.cap.toModel()
            val plan = CatchUp.plan(cap, Money(row.spent), settings.rule(cap.kind, phone), today) ?: continue
            dao.insertClosedPeriods(plan.closed.map { it.toEntity() })
            dao.setOpenPeriod(cap.id, plan.openStart.toEpochDay(), plan.reserve.minor)
        }
    }
}

/** A reserve cleared, for Undo: the cap, what its reserve was, and the open period it was cleared in. */
data class ClearedReserve(val capId: Long, val reserve: Money, val openStart: LocalDate)
