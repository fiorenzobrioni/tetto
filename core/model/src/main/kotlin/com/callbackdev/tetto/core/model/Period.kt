package com.callbackdev.tetto.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A cap's cycle (PLANNING.md §4.2). [ONE_OFF] is no cycle (owner, 6 Nov 2026): a cap of its own
 * dates, a trip or a festival, which ends once and never renews; its dates are the cap's own
 * ([Cap.endExclusive]), never a rule's.
 */
enum class PeriodKind {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ONE_OFF,
    ;

    /** A period that renews: every kind but [ONE_OFF]. */
    val renews: Boolean get() = this != ONE_OFF
}

/**
 * What decides a period's dates: its kind, the day a week starts (the setting, or the locale's)
 * and the day a month starts (1 to 31; a year always starts on 1 January).
 */
data class PeriodRule(val kind: PeriodKind, val weekStart: DayOfWeek, val monthStartDay: Int) {
    init {
        require(monthStartDay in 1..31) { "A month starts on day 1 to 31, not $monthStartDay" }
    }
}

/**
 * A range of local dates, [start] included and [endExclusive] excluded (PLANNING.md §4.2). Local
 * dates, never instants: a daylight-saving night or a flight moves instants, not dates.
 */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    init {
        require(endExclusive > start) { "An empty or reversed range: $start to $endExclusive" }
    }

    /** How many days it holds. */
    val length: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()

    /** Its last day, the one "until" names. */
    val lastDay: LocalDate get() = endExclusive.minusDays(1)

    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive
}
