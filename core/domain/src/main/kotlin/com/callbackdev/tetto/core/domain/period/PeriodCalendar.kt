package com.callbackdev.tetto.core.domain.period

import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Periods as ranges of local dates (PLANNING.md §4.2). Pure: a date and a rule in, a range out.
 *
 * - A day is that day.
 * - A week runs seven days from the week's first day on or before the date.
 * - A month runs from its start day `d` to the next month's: each month starts on `min(d, its
 *   length)`, so with `d = 31` the months start on 31 Jan, 28 (or 29) Feb, 31 Mar, 30 Apr.
 * - A year runs from `d` January to the next.
 */
object PeriodCalendar {
    fun containing(date: LocalDate, rule: PeriodRule): DateRange = when (rule.kind) {
        PeriodKind.DAY -> DateRange(date, date.plusDays(1))

        PeriodKind.WEEK -> {
            val start = date.with(TemporalAdjusters.previousOrSame(rule.weekStart))
            DateRange(start, start.plusDays(7))
        }

        PeriodKind.MONTH -> {
            val month = YearMonth.from(date)
            val thisStart = monthStart(month, rule.monthStartDay)
            if (date >= thisStart) {
                DateRange(thisStart, monthStart(month.plusMonths(1), rule.monthStartDay))
            } else {
                DateRange(monthStart(month.minusMonths(1), rule.monthStartDay), thisStart)
            }
        }

        PeriodKind.YEAR -> {
            val thisStart = LocalDate.of(date.year, 1, rule.monthStartDay)
            if (date >= thisStart) {
                DateRange(thisStart, thisStart.plusYears(1))
            } else {
                DateRange(thisStart.minusYears(1), thisStart)
            }
        }
    }

    /** The period after [period]: the one its end falls in. */
    fun next(period: DateRange, rule: PeriodRule): DateRange = containing(period.endExclusive, rule)

    /**
     * A cap's open period: from its [openStart] to the end of the rule's period that holds it.
     * Usually [openStart] is that period's own start; where it is not (a period replaced mid-way,
     * after closed ones, §4.5), the open period is the rest of it, and the next one is whole.
     */
    fun openPeriod(openStart: LocalDate, rule: PeriodRule): DateRange =
        DateRange(openStart, containing(openStart, rule).endExclusive)

    private fun monthStart(month: YearMonth, day: Int): LocalDate = month.atDay(minOf(day, month.lengthOfMonth()))
}
