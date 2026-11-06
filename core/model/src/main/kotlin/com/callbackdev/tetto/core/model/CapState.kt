package com.callbackdev.tetto.core.model

import java.math.BigInteger
import java.time.LocalDate

/**
 * What a cap says today (PLANNING.md §4.6), computed from the cap, its open period's spends and
 * today; never stored.
 *
 * @property period the open period.
 * @property left the cap and its reserve less what was spent: below zero, the cap is [over].
 * @property daysLeft the days from today to the period's end, today counted: at least 1.
 * @property bar what was spent of the cap and its reserve, held to 0..1; [over] says the rest.
 * @property todayMark where today falls in the period, today counted; none for a day.
 * @property aheadOfDays spending runs ahead of the days: not over, and the bar past the mark by
 *   more than a step of the period that fades by its end (PLANNING.md §4.6).
 * @property pace about how much a day is left until the end, in whole units of the currency;
 *   none for a day, when over, on the last day, or under one unit.
 * @property renewedToday a period closed today and this one began with it.
 */
data class CapState(
    val cap: Cap,
    val period: DateRange,
    val spent: Money,
    val left: Money,
    val daysLeft: Int,
    val bar: Share,
    val todayMark: Share?,
    val aheadOfDays: Boolean,
    val pace: Money?,
    val renewedToday: Boolean,
) {
    val over: Boolean get() = left.isNegative

    /** How far past the cap and its reserve: zero unless [over]. Said "€30 over", never "−€30". */
    val overBy: Money get() = if (over) -left else Money.ZERO

    /** The open period's last day: "€12 until Sunday" (owner, 5 Nov 2026: the day included). */
    val lastDay: LocalDate get() = period.lastDay

    /** The first day of the next period: "it starts again on Monday". */
    val startsAgain: LocalDate get() = period.endExclusive
}

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
