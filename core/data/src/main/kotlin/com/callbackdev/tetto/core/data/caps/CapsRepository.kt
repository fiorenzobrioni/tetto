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
import com.callbackdev.tetto.core.domain.cap.MonthSums
import com.callbackdev.tetto.core.domain.cap.YourMonth
import com.callbackdev.tetto.core.domain.money.Rescale
import com.callbackdev.tetto.core.domain.period.Days
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    /**
     * Each change of the caps, their spends or their closed periods, once its transaction is
     * committed, from the first after collection: the home screen's cards and the launcher's
     * shortcuts redraw on it (PLANNING.md §7, §8). Whatever wrote it, this repository's writes, a
     * restore or an erase.
     */
    val changes: Flow<Unit> =
        database.invalidationTracker.createFlow(*TettoDatabase.CAP_TABLES, emitInitialState = false).map { }

    /** The open period's spends of the cap [capId], newest first, as they change: the cap's sheet. */
    fun observeOpenSpends(capId: Long): Flow<List<Spend>> = changes().map { openSpends(capId) }.distinctUntilChanged()

    /** The cap [capId]'s latest closed lines, at most [limit], the latest first, as they change. */
    fun observeClosedPeriods(capId: Long, limit: Int): Flow<List<ClosedPeriod>> =
        changes().map { closedPeriods(capId).take(limit) }.distinctUntilChanged()

    private fun changes(): Flow<Set<String>> =
        database.invalidationTracker.createFlow(*TettoDatabase.CAP_TABLES).conflate()

    /**
     * The reader's month at the end of the caps screen (owner, 6 Nov 2026), computed again whenever
     * a spend, a cap or a setting changes; a page open across midnight asks again at its tick, as
     * for [states].
     */
    val yourMonth: Flow<YourMonth> =
        combine(database.invalidationTracker.createFlow(*TettoDatabase.CAP_TABLES), preferences.settings) { _, _ -> }
            .conflate()
            .map { currentMonth() }
            .distinctUntilChanged()

    /**
     * What was added to the caps in the reader's month (from the month start day of Settings) and
     * in the one before, in the zone of the moment of asking, and what the caps are worth in a
     * month. Spends of closed periods count: they were added in the month all the same.
     */
    suspend fun currentMonth(): YourMonth = afterCatchUp { settings, moment ->
        val zone = moment.zone
        val month = MonthSums.month(moment.today, settings.monthStartDay)
        val last = MonthSums.previous(month, settings.monthStartDay)
        val caps = dao.allCaps().map { it.toModel() }
        YourMonth(
            month = month,
            added = added(month, zone),
            lastMonth = last,
            addedLastMonth = added(last, zone),
            worth = MonthSums.worth(caps, month, settings.resolvedCurrency(phone)),
        )
    }

    private suspend fun added(range: DateRange, zone: ZoneId): Money {
        val (from, until) = Days.instantsOf(range, zone)
        return Money(dao.addedBetween(from.toEpochMilli(), until.toEpochMilli()))
    }

    /** The caps' states once, for a card's render, the reminder, the numpad over the home screen. */
    suspend fun currentStates(): List<CapState> = afterCatchUp { settings, moment ->
        val today = moment.today
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
        afterCatchUp { _, _ -> }
    }

    /** A new cap, its open period the one that holds today, last in the reader's order; its id. */
    suspend fun addCap(name: String, icon: CapIcon, amount: Money, kind: PeriodKind): Long =
        addCaps(listOf(CapDraft(name, amount, kind, icon))).single()

    /**
     * New caps, in the order of [drafts], after the reader's last, in one transaction: the first
     * run's areas become caps together or not at all. Their ids, in that order.
     */
    suspend fun addCaps(drafts: List<CapDraft>): List<Long> {
        drafts.forEach { draft ->
            require(draft.name.isNotBlank()) { "A cap needs a name" }
            require(draft.amount.isPositive) { "A cap needs an amount above zero" }
        }
        return afterCatchUp { settings, moment ->
            val today = moment.today
            val createdAt = moment.now.toEpochMilli()
            val first = dao.lastPosition() + 1
            drafts.mapIndexed { index, draft ->
                dao.insertCap(
                    CapEntity(
                        name = draft.name.trim(),
                        icon = draft.icon.key,
                        amount = draft.amount.minor,
                        kind = draft.kind.name,
                        reserve = 0,
                        openStart = CapChanges.openStartForNew(settings.rule(draft.kind, phone), today).toEpochDay(),
                        position = first + index,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }

    /**
     * Adds [amount] to the cap [capId], now, in its open period (§4.3); the spend added, for Undo,
     * or null when the cap is gone.
     */
    suspend fun addSpend(capId: Long, amount: Money): Spend? {
        require(amount.isPositive) { "A spend is an amount above zero" }
        return afterCatchUp { _, moment ->
            val cap = dao.cap(capId) ?: return@afterCatchUp null
            val spend = SpendEntity(
                capId = capId,
                amount = amount.minor,
                // The instant the catch-up judged today by: a spend at midnight counts in the
                // period that holds that instant's day, never in the one closing behind it.
                addedAt = moment.now.toEpochMilli(),
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
        return afterCatchUp { _, _ ->
            val spend = openSpend(spendId) ?: return@afterCatchUp null
            dao.setSpendAmount(spendId, amount.minor)
            Money(spend.amount)
        }
    }

    /** Deletes the spend [spendId]; the spend deleted, for Undo, or null when it is gone or its period has closed. */
    suspend fun deleteSpend(spendId: Long): Spend? = afterCatchUp { _, _ ->
        val spend = openSpend(spendId) ?: return@afterCatchUp null
        dao.deleteSpend(spendId)
        spend.toModel()
    }

    /** Undoes [addSpend]: deletes that spend, by its id, while its period is open. */
    suspend fun undoAdd(spend: Spend): Boolean = deleteSpend(spend.id) != null

    /** Undoes [editSpend]: writes [previous] back, while the spend's period is open. */
    suspend fun undoEdit(spendId: Long, previous: Money): Boolean = editSpend(spendId, previous) != null

    /** Undoes [deleteSpend]: the spend again, with its id and fields, while its period is open. */
    suspend fun undoDelete(spend: Spend): Boolean = afterCatchUp { _, _ ->
        val cap = dao.cap(spend.capId) ?: return@afterCatchUp false
        if (cap.openStart != spend.periodStart.toEpochDay() || dao.spend(spend.id) != null) return@afterCatchUp false
        dao.insertSpend(spend.toEntity())
        true
    }

    /** The open period's spends of the cap [capId], newest first. */
    suspend fun openSpends(capId: Long): List<Spend> = afterCatchUp { _, _ ->
        val cap = dao.cap(capId) ?: return@afterCatchUp emptyList()
        dao.spends(capId, cap.openStart).map { it.toModel() }
    }

    /** The cap [capId]'s closed periods, the latest first: the reserve's story. */
    suspend fun closedPeriods(capId: Long): List<ClosedPeriod> = afterCatchUp { _, _ ->
        dao.closedPeriods(capId).map { it.toModel() }
    }

    /**
     * Changes the cap [capId] (§4.5). Its name and icon change nothing else. A new amount applies
     * to the open period at once, and the reserve is held to it, the limit the next close would
     * hold it to. A new [kind] replaces the open period by the new one that holds today
     * ([CapChanges.replace]): the days already past are kept as a closed line where the new period
     * starts later, and the rest of the open spends move into it; closed periods and their spends
     * stay as they are. False when the cap is gone.
     */
    suspend fun changeCap(capId: Long, name: String, icon: CapIcon, amount: Money, kind: PeriodKind): Boolean {
        require(name.isNotBlank()) { "A cap needs a name" }
        require(amount.isPositive) { "A cap needs an amount above zero" }
        return afterCatchUp { settings, moment ->
            val cap = dao.cap(capId)?.toModel() ?: return@afterCatchUp false
            dao.setCapLooks(capId, name.trim(), icon.key, amount.minor)
            if (kind != cap.kind) {
                val lastClosedEnd = dao.lastClosedEnd(capId)?.let(LocalDate::ofEpochDay)
                val oldRule = settings.rule(cap.kind, phone)
                replaceOpenLocked(cap, oldRule, settings.rule(kind, phone), amount, lastClosedEnd, moment)
            } else {
                val held = CapChanges.heldReserve(cap.reserve, amount)
                if (held != cap.reserve) dao.setReserve(capId, held.minor)
            }
            true
        }
    }

    /** Clears the cap [capId]'s reserve; what it was, for Undo, or null when the cap is gone or had none. */
    suspend fun clearReserve(capId: Long): ClearedReserve? = afterCatchUp { _, _ ->
        val cap = dao.cap(capId) ?: return@afterCatchUp null
        if (cap.reserve == 0L) return@afterCatchUp null
        dao.setReserve(capId, 0)
        ClearedReserve(capId, Money(cap.reserve), LocalDate.ofEpochDay(cap.openStart))
    }

    /**
     * Undoes [clearReserve], while the period it was cleared in is still open: after a close, the
     * reserve is the one that close carried, and the old one no longer means anything.
     */
    suspend fun undoClearReserve(cleared: ClearedReserve): Boolean = afterCatchUp { _, _ ->
        val cap = dao.cap(cleared.capId) ?: return@afterCatchUp false
        if (cap.openStart != cleared.openStart.toEpochDay() || cap.reserve != 0L) return@afterCatchUp false
        dao.setReserve(cleared.capId, cleared.reserve.minor)
        true
    }

    /** Deletes the cap [capId], its spends and its closed periods. */
    suspend fun deleteCap(capId: Long): Boolean = afterCatchUp { _, _ -> dao.deleteCap(capId) > 0 }

    /** Puts the caps in the order of [capIds], in one transaction. */
    suspend fun reorder(capIds: List<Long>) {
        afterCatchUp { _, _ -> capIds.forEachIndexed { position, id -> dao.setPosition(id, position) } }
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

    /**
     * The app's currency becomes [code] (§4.1): the amounts are relabelled, never converted. Where
     * the two currencies' decimals differ, every stored amount is rescaled to keep its figure
     * ([Rescale]: €12.50 becomes ¥13, an amount above zero never zero), in one transaction: the
     * caps and their reserves, the spends of every period, the closed periods' lines.
     *
     * The setting is written first, then the amounts, under [lock], so nothing reads in between;
     * should the transaction fail, the setting is put back, and no figure is ever read in a
     * currency it was not counted in. Only a process killed between the two writes could leave
     * them apart, as with [changeWeekStart].
     */
    suspend fun changeCurrency(code: String): UserSettings {
        val to = requireNotNull(AppCurrency.ofOrNull(code)) { "Not a currency a cap can count in: $code" }
        return lock.withLock {
            val before = preferences.current()
            val from = before.resolvedCurrency(phone)
            val after = preferences.update { it.copy(currency = to.code) }
            if (from.fractionDigits != to.fractionDigits) {
                try {
                    database.withTransaction {
                        catchUpLocked(after, moment())
                        rescaleLocked(from.fractionDigits, to.fractionDigits)
                    }
                } catch (failure: Throwable) {
                    withContext(NonCancellable) { preferences.update { it.copy(currency = before.currency) } }
                    throw failure
                }
                keepWeekInForce(after)
            } else {
                after
            }
        }
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
            val moment = moment()
            catchUpLocked(before, moment)
            replaceOpenPeriodsLocked(before, after, moment)
        }
        keepWeekInForce(after)
    }

    /**
     * Runs inside a transaction: the open periods of the caps whose rule differs between [before]
     * and [after] (a week's start, a month's start day) replaced by the new rule's that holds today
     * (§4.5). The periods that ended under [before] must be closed first.
     */
    private suspend fun replaceOpenPeriodsLocked(before: UserSettings, after: UserSettings, moment: Moment) {
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
            replaceOpenLocked(cap, oldRule, newRule, cap.amount, lastClosedEnd, moment)
        }
    }

    /**
     * Runs inside a transaction: [cap]'s open period, made by [oldRule], replaced by [newRule]'s
     * that holds today, with [newAmount] the cap from then on (§4.5, [CapChanges.replace]). The
     * days kept, where the new period starts later, are those before its first instant in the zone
     * of asking: a spend added before it stays in them, one added from it on moves.
     */
    private suspend fun replaceOpenLocked(
        cap: Cap,
        oldRule: PeriodRule,
        newRule: PeriodRule,
        newAmount: Money,
        lastClosedEnd: LocalDate?,
        moment: Moment,
    ) {
        val start = CapChanges.replacedOpenStart(newRule, moment.today, lastClosedEnd)
        if (start == cap.openStart && newRule.kind == cap.kind && newAmount == cap.amount) return
        val from = cap.openStart.toEpochDay()
        val cut = Days.instantsOf(start, moment.zone).first.toEpochMilli()
        val spentBefore = if (start > cap.openStart) Money(dao.spentBefore(cap.id, from, cut)) else Money.ZERO
        val plan = CapChanges.replace(cap, oldRule, start, newAmount, spentBefore)
        plan.closed?.let { dao.insertClosedPeriods(listOf(it.toEntity())) }
        dao.moveSpends(cap.id, from, start.toEpochDay(), since = if (plan.closed != null) cut else Long.MIN_VALUE)
        dao.setCapPeriod(cap.id, newRule.kind.name, start.toEpochDay())
        dao.setReserve(cap.id, plan.reserve.minor)
    }

    /**
     * Notes the week the weekly caps' open periods now run on ([UserSettings.weekInForce]), once a
     * transaction that closed and moved them by it has committed; the settings as stored after.
     * Written after the caps, never before: should the process die between the two, the next read
     * finds the old note and moves nothing a second time (a period already moved starts where the
     * new rule puts it).
     */
    internal suspend fun keepWeekInForce(settings: UserSettings): UserSettings {
        val week = settings.weekStart ?: phone.weekStart()
        return if (settings.weekInForce == week) settings else preferences.update { it.copy(weekInForce = week) }
    }

    /** Now, and the zone in effect now, read once for a whole read or write (§4.2). */
    internal fun moment(): Moment = Moment(clock.now(), clock.zone())

    /**
     * [block] under [lock], for this module's other repositories whose work spans the caps and the
     * settings (a restore, an erase: `BackupRepository`): nothing reads or writes the caps meanwhile.
     */
    internal suspend fun <T> exclusively(block: suspend () -> T): T = lock.withLock { block() }

    /** A read or a write, under [lock], after the catch-up, in the same transaction. */
    private suspend fun <T> afterCatchUp(block: suspend (UserSettings, Moment) -> T): T = lock.withLock {
        val settings = preferences.current()
        // Read once: the catch-up and the work after it see the same day, so a spend added on the
        // stroke of midnight never lands in the period that the same read has just judged open.
        val moment = moment()
        val result = database.withTransaction {
            catchUpLocked(settings, moment)
            block(settings, moment)
        }
        keepWeekInForce(settings)
        result
    }

    /** The spend [spendId], if it counts in its cap's open period: the only one that can change. */
    private suspend fun openSpend(spendId: Long): SpendEntity? {
        val spend = dao.spend(spendId) ?: return null
        val cap = dao.cap(spend.capId) ?: return null
        return spend.takeIf { it.periodStart == cap.openStart }
    }

    /** Runs inside a transaction: every stored amount from [fromDigits] decimals to [toDigits] (§4.1). */
    private suspend fun rescaleLocked(fromDigits: Int, toDigits: Int) {
        fun rescaled(minor: Long): Long = Rescale.amount(Money(minor), fromDigits, toDigits).minor

        // What the numpad typed stays an amount it can type back (Rescale.typed).
        fun typed(minor: Long): Long = Rescale.typed(Money(minor), fromDigits, toDigits).minor
        for (cap in dao.allCaps()) dao.setCapAmounts(cap.id, typed(cap.amount), typed(cap.reserve))
        for (spend in dao.allSpends()) dao.setSpendAmount(spend.id, typed(spend.amount))
        dao.updateClosedPeriods(
            dao.allClosedPeriods().map {
                it.copy(
                    cap = rescaled(it.cap),
                    spent = rescaled(it.spent),
                    reserveBefore = rescaled(it.reserveBefore),
                    reserveAfter = rescaled(it.reserveAfter),
                    forgiven = rescaled(it.forgiven),
                )
            },
        )
    }

    /**
     * Runs inside a transaction, under [lock]: closes what ended for every cap (§4.4), each by the
     * rule its open period was made by. Where the week in force is no longer the one the weekly
     * caps were made by (the phone's week changed under a reader's week that follows it), their
     * open periods are then moved to it, as a change in Settings moves them (§4.5); the caller
     * notes the new week once the transaction commits ([keepWeekInForce]).
     */
    internal suspend fun catchUpLocked(settings: UserSettings, moment: Moment) {
        val week = settings.weekStart ?: phone.weekStart()
        val madeBy = settings.copy(weekStart = settings.weekInForce ?: week)
        for (row in dao.capRows()) {
            val cap = row.cap.toModel()
            val plan = CatchUp.plan(cap, Money(row.spent), madeBy.rule(cap.kind, phone), moment.today) ?: continue
            dao.insertClosedPeriods(plan.closed.map { it.toEntity() })
            dao.setOpenPeriod(cap.id, plan.openStart.toEpochDay(), plan.reserve.minor)
        }
        if (madeBy.weekStart != week) replaceOpenPeriodsLocked(madeBy, settings, moment)
    }
}

/** One moment of asking: [now], the zone in effect then, and the local date the two make (§4.2). */
data class Moment(val now: Instant, val zone: ZoneId) {
    val today: LocalDate get() = Days.dateOf(now, zone)
}

/** A reserve cleared, for Undo: the cap, what its reserve was, and the open period it was cleared in. */
data class ClearedReserve(val capId: Long, val reserve: Money, val openStart: LocalDate)
