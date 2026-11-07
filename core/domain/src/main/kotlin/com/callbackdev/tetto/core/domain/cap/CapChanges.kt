package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * What replacing a cap's open period writes (PLANNING.md §4.5): the new open period's [openStart],
 * the [reserve] it starts with, and the line that closes the days kept, [closed], when the new
 * period starts after the old one began.
 */
data class Replacement(val openStart: LocalDate, val reserve: Money, val closed: ClosedPeriod?)

/** The dates and figures a change of cap or of settings decides (PLANNING.md §4.5). */
object CapChanges {
    /**
     * Where a new cap's open period starts: the start of the rule's period that holds today, so a
     * weekly cap made on a Thursday counts the whole week.
     */
    fun openStartForNew(rule: PeriodRule, today: LocalDate): LocalDate = PeriodCalendar.containing(today, rule).start

    /**
     * Where a cap's open period starts once its rule changes (its kind, or the week's or the
     * month's start day): the new rule's period that holds today. Never before [lastClosedEnd]: a
     * closed period is never changed, so a new period that would reach back into one starts where
     * it ended (a daily cap turned weekly on a Thursday, after closing Monday to Wednesday, counts
     * Thursday to Sunday).
     */
    fun replacedOpenStart(rule: PeriodRule, today: LocalDate, lastClosedEnd: LocalDate?): LocalDate {
        val start = PeriodCalendar.containing(today, rule).start
        return if (lastClosedEnd != null && lastClosedEnd > start) lastClosedEnd else start
    }

    /**
     * The open period of [cap], made by [oldRule], replaced by one that starts on [start]
     * ([replacedOpenStart]), with [newAmount] the cap from then on (owner, 6 Nov 2026):
     *
     * - **A new period that holds the old one's start** (a longer one, or the same days): the open
     *   spends move into it, nothing closes, and the reserve is held to [newAmount], the limit a
     *   close would hold it to: a month's €300 of reserve never rides on a week's €50 cap.
     * - **A new period that starts later** (a shorter one: a week turned into days on a Thursday,
     *   a month start moved forward): the days before [start] are kept, as a line of their own,
     *   with what was added in them ([spentBefore]) and their share of the old cap, its days over
     *   the old open period's, rounded down; it closes as any period does ([Rollover]), the reserve
     *   held to [newAmount]. What was added from [start] on moves into the new period. So a spend
     *   of Monday never makes Thursday's day look over, and the CSV keeps the week it was added in.
     *
     * A one-off cap (owner, 6 Nov 2026) takes the same rules, to it, from it and between two of its
     * dates; it keeps no reserve, so [reserveLimit] is then nothing. Its new dates can start after
     * today: the days kept are only those already past, until [keptUntil], and what was added
     * from today on goes with the new dates.
     */
    fun replace(
        cap: Cap,
        oldRule: PeriodRule,
        start: LocalDate,
        newAmount: Money,
        spentBefore: Money,
        keptUntil: LocalDate = start,
        reserveLimit: Money = newAmount,
    ): Replacement {
        if (keptUntil <= cap.openStart) return Replacement(start, minOf(cap.reserve, reserveLimit), null)
        val open = PeriodCalendar.openPeriod(cap, oldRule)
        val days = ChronoUnit.DAYS.between(cap.openStart, keptUntil).coerceAtMost(open.length.toLong())
        val share = Money(Math.multiplyExact(cap.amount.minor, days) / open.length)
        val outcome = Rollover.close(share, cap.reserve, spentBefore, limit = reserveLimit)
        val kept = ClosedPeriod(
            capId = cap.id,
            range = DateRange(cap.openStart, keptUntil),
            cap = share,
            spent = spentBefore,
            reserveBefore = cap.reserve,
            reserveAfter = outcome.reserveAfter,
            forgiven = outcome.forgiven,
            periods = 1,
        )
        return Replacement(start, outcome.reserveAfter, kept)
    }

    /**
     * The reserve once a cap's amount becomes [newAmount] with its period unchanged: held to it,
     * the limit the next close would hold it to (owner, 6 Nov 2026). A cap raised keeps it all.
     */
    fun heldReserve(reserve: Money, newAmount: Money): Money = minOf(reserve, newAmount)

    /**
     * The dates a one-off cap runs on, from the [chosen] ones (owner, 6 Nov 2026): never before
     * [lastClosedEnd], since a closed period is never changed (its earlier dates, a cap it was
     * before), and never ending before [today], since an end already past would close at once.
     * The reader's choice where neither applies.
     */
    fun oneOffDates(chosen: DateRange, today: LocalDate, lastClosedEnd: LocalDate?): DateRange {
        val end = maxOf(chosen.endExclusive, today.plusDays(1))
        val start = maxOf(chosen.start, lastClosedEnd ?: chosen.start)
        return DateRange(minOf(start, end.minusDays(1)), end)
    }
}
