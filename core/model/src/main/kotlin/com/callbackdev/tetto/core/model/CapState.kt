package com.callbackdev.tetto.core.model

import java.math.BigInteger
import java.time.LocalDate

/**
 * What a cap says today (PLANNING.md §4.6), computed from the cap, its open period's spends and
 * today; never stored.
 *
 * @property period the open period.
 * @property amount what the cap gives this period: its amount, or, for a period cut short by a
 *   change, its share of the days ([partial]; owner, 7 Nov 2026). An ended one-off's, its line's.
 * @property partial the open period's days over the whole period's, where a change cut it short;
 *   null for a whole period.
 * @property left this period's [amount] and the reserve less what was spent: below zero, the cap
 *   is [over].
 * @property daysLeft the days from today to the period's end, today counted: at least 1.
 * @property bar what was spent of [amount] and the reserve, held to 0..1; [over] says the rest.
 * @property todayMark where today falls in the period, today counted; none for a day.
 * @property aheadOfDays spending runs ahead of the days: not over, and the bar past the mark by
 *   more than a step of the period that fades by its end (PLANNING.md §4.6).
 * @property pace about how much a day is left until the end, in whole units of the currency, or
 *   less than one ([Pace.lessThan]); none for a day, when over or with nothing left, on the last day.
 * @property renewedToday a period closed today and this one began with it.
 * @property renewal how the period that closed today ended, said on the day it renews (owner,
 *   6 Nov 2026); null on any other day.
 * @property ended a one-off cap whose dates are over (owner, 6 Nov 2026): [period] is the dates it
 *   had, [spent] what they spent and [left] what they left of the cap; no mark, no pace, no news
 *   but [endedToday].
 * @property endedToday a one-off cap whose last day was yesterday: the sentence says how it ended.
 */
data class CapState(
    val cap: Cap,
    val period: DateRange,
    val amount: Money,
    val spent: Money,
    val left: Money,
    val daysLeft: Int,
    val bar: Share,
    val todayMark: Share?,
    val aheadOfDays: Boolean,
    val pace: Pace?,
    val renewedToday: Boolean,
    val renewal: Renewal? = null,
    val ended: Boolean = false,
    val endedToday: Boolean = false,
    val partial: Share? = null,
) {
    val over: Boolean get() = left.isNegative

    /** How far past the cap and its reserve: zero unless [over]. Said "€30 over", never "−€30". */
    val overBy: Money get() = if (over) -left else Money.ZERO

    /** The open period's last day: "€12 until Sunday" (owner, 5 Nov 2026: the day included). */
    val lastDay: LocalDate get() = period.lastDay

    /**
     * The first day of the next period: "it starts again on Monday". A one-off cap has none: over,
     * it says when it ends, the day after its last ([PeriodKind.ONE_OFF]).
     */
    val startsAgain: LocalDate get() = period.endExclusive

    /** Whether a new period follows this one: false for a one-off cap. */
    val renews: Boolean get() = cap.kind.renews
}

/**
 * How much a day is left until a period's last day (PLANNING.md §4.6): about [perDay], in whole
 * units of the currency, rounded down; or, where that is under one unit and something is left,
 * [lessThan] one unit, [perDay] then that unit ("less than €1 a day", owner, 7 Nov 2026).
 */
data class Pace(val perDay: Money, val lessThan: Boolean = false)

/**
 * A share of a whole, kept as the two whole numbers it is made of so that comparing two shares is
 * exact: "spending runs ahead of the days" is a comparison, and a rounded one could flip it.
 * The screen turns it into a length when it draws it.
 */
data class Share(val part: Long, val whole: Long) : Comparable<Share> {
    init {
        require(whole > 0) { "A share of nothing" }
        require(part >= 0) { "A negative share" }
    }

    override fun compareTo(other: Share): Int = (BigInteger.valueOf(part) * BigInteger.valueOf(other.whole))
        .compareTo(BigInteger.valueOf(other.part) * BigInteger.valueOf(whole))

    val isWhole: Boolean get() = part >= whole
}

/** How near a day is to today, so the words can say "today", a weekday, or a date. */
enum class NearDay {
    TODAY,
    TOMORROW,

    /** Two to six days ahead: a weekday names one day only. */
    WEEKDAY,

    /** A week or more ahead, or behind today: a date. */
    DATE,
}

/**
 * How a period ended, said the day the next one begins (owner, 6 Nov 2026): the moment Tetto is
 * kind, said when it happens rather than left to a line in the cap's sheet. One [kind], the one
 * that matters most, and its [amount].
 */
data class Renewal(val kind: Kind, val amount: Money) {
    enum class Kind {
        /** Past the cap and the reserve: [amount] forgiven, a clean start. */
        FORGIVEN,

        /** Over the cap, and the reserve covered [amount] of it. */
        COVERED,

        /** Under the cap: [amount] went to the reserve. */
        SAVED,

        /** Under the cap, with the reserve already at its limit: nothing more to keep. */
        RESERVE_FULL,

        /** Right on the cap. */
        EVEN,
    }
}
