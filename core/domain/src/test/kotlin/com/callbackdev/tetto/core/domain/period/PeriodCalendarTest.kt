package com.callbackdev.tetto.core.domain.period

import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY

/** Periods as local dates (PLANNING.md §4.2; §4.7, Periods). 5 Nov 2026 is a Thursday. */
class PeriodCalendarTest {
    private fun range(start: java.time.LocalDate, end: java.time.LocalDate) = DateRange(start, end)

    @Test
    fun `a day is that day`() {
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), rule(DAY)))
            .isEqualTo(range(date(2026, 11, 5), date(2026, 11, 6)))
    }

    @Test
    fun `a week starting on Monday`() {
        val week = range(date(2026, 11, 2), date(2026, 11, 9))
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), rule(WEEK, weekStart = MONDAY))).isEqualTo(week)
        assertThat(PeriodCalendar.containing(date(2026, 11, 2), rule(WEEK, weekStart = MONDAY))).isEqualTo(week)
        assertThat(PeriodCalendar.containing(date(2026, 11, 8), rule(WEEK, weekStart = MONDAY))).isEqualTo(week)
    }

    @Test
    fun `a week starting on Sunday`() {
        val week = range(date(2026, 11, 1), date(2026, 11, 8))
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), rule(WEEK, weekStart = SUNDAY))).isEqualTo(week)
        assertThat(PeriodCalendar.containing(date(2026, 11, 1), rule(WEEK, weekStart = SUNDAY))).isEqualTo(week)
        assertThat(PeriodCalendar.containing(date(2026, 11, 7), rule(WEEK, weekStart = SUNDAY))).isEqualTo(week)
    }

    @Test
    fun `a month starting on the 1st`() {
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), rule(MONTH)))
            .isEqualTo(range(date(2026, 11, 1), date(2026, 12, 1)))
        assertThat(PeriodCalendar.containing(date(2026, 12, 31), rule(MONTH)))
            .isEqualTo(range(date(2026, 12, 1), date(2027, 1, 1)))
    }

    @Test
    fun `a month starting on the 15th`() {
        val month = rule(MONTH, monthStartDay = 15)
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), month))
            .isEqualTo(range(date(2026, 10, 15), date(2026, 11, 15)))
        assertThat(PeriodCalendar.containing(date(2026, 11, 15), month))
            .isEqualTo(range(date(2026, 11, 15), date(2026, 12, 15)))
        // Across the year's end.
        assertThat(PeriodCalendar.containing(date(2027, 1, 3), month))
            .isEqualTo(range(date(2026, 12, 15), date(2027, 1, 15)))
    }

    @Test
    fun `a month starting on the 29th, across February in a leap year and not`() {
        val month = rule(MONTH, monthStartDay = 29)
        // 2027: February has 28 days, so its month starts on the 28th.
        assertThat(PeriodCalendar.containing(date(2027, 2, 10), month))
            .isEqualTo(range(date(2027, 1, 29), date(2027, 2, 28)))
        assertThat(PeriodCalendar.containing(date(2027, 2, 28), month))
            .isEqualTo(range(date(2027, 2, 28), date(2027, 3, 29)))
        // 2028: a leap year, February's 29th exists.
        assertThat(PeriodCalendar.containing(date(2028, 2, 28), month))
            .isEqualTo(range(date(2028, 1, 29), date(2028, 2, 29)))
        assertThat(PeriodCalendar.containing(date(2028, 2, 29), month))
            .isEqualTo(range(date(2028, 2, 29), date(2028, 3, 29)))
    }

    @Test
    fun `a month starting on the 30th, across February in a leap year and not`() {
        val month = rule(MONTH, monthStartDay = 30)
        assertThat(PeriodCalendar.containing(date(2027, 3, 1), month))
            .isEqualTo(range(date(2027, 2, 28), date(2027, 3, 30)))
        assertThat(PeriodCalendar.containing(date(2028, 3, 1), month))
            .isEqualTo(range(date(2028, 2, 29), date(2028, 3, 30)))
        assertThat(PeriodCalendar.containing(date(2027, 2, 27), month))
            .isEqualTo(range(date(2027, 1, 30), date(2027, 2, 28)))
    }

    @Test
    fun `a month starting on the 31st runs 31 Jan, 28 Feb, 31 Mar, 30 Apr`() {
        val month = rule(MONTH, monthStartDay = 31)
        var period = PeriodCalendar.containing(date(2027, 2, 1), month)
        assertThat(period).isEqualTo(range(date(2027, 1, 31), date(2027, 2, 28)))
        period = PeriodCalendar.next(period, month)
        assertThat(period).isEqualTo(range(date(2027, 2, 28), date(2027, 3, 31)))
        period = PeriodCalendar.next(period, month)
        assertThat(period).isEqualTo(range(date(2027, 3, 31), date(2027, 4, 30)))
        period = PeriodCalendar.next(period, month)
        assertThat(period).isEqualTo(range(date(2027, 4, 30), date(2027, 5, 31)))
        // A leap year's February.
        assertThat(PeriodCalendar.containing(date(2028, 2, 29), month))
            .isEqualTo(range(date(2028, 2, 29), date(2028, 3, 31)))
        // The day before a short month's start is still the month before.
        assertThat(PeriodCalendar.containing(date(2027, 4, 29), month))
            .isEqualTo(range(date(2027, 3, 31), date(2027, 4, 30)))
    }

    @Test
    fun `a year starting on 1 January`() {
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), rule(YEAR)))
            .isEqualTo(range(date(2026, 1, 1), date(2027, 1, 1)))
        assertThat(PeriodCalendar.containing(date(2026, 1, 1), rule(YEAR)))
            .isEqualTo(range(date(2026, 1, 1), date(2027, 1, 1)))
    }

    @Test
    fun `a year starting on 31 January`() {
        val year = rule(YEAR, monthStartDay = 31)
        assertThat(PeriodCalendar.containing(date(2026, 11, 5), year))
            .isEqualTo(range(date(2026, 1, 31), date(2027, 1, 31)))
        assertThat(PeriodCalendar.containing(date(2027, 1, 30), year))
            .isEqualTo(range(date(2026, 1, 31), date(2027, 1, 31)))
        assertThat(PeriodCalendar.containing(date(2027, 1, 31), year))
            .isEqualTo(range(date(2027, 1, 31), date(2028, 1, 31)))
    }

    @Test
    fun `the next period follows without a gap`() {
        for (kind in listOf(DAY, WEEK, MONTH, YEAR)) {
            val rule = rule(kind, monthStartDay = 31)
            var period = PeriodCalendar.containing(date(2026, 1, 15), rule)
            repeat(30) {
                val next = PeriodCalendar.next(period, rule)
                assertThat(next.start).isEqualTo(period.endExclusive)
                period = next
            }
        }
    }

    @Test
    fun `an open period that starts mid-way runs to the rule's end`() {
        // A daily cap turned weekly on Thursday, after three closed days (PLANNING.md §4.5).
        assertThat(PeriodCalendar.openPeriod(date(2026, 11, 5), rule(WEEK)))
            .isEqualTo(range(date(2026, 11, 5), date(2026, 11, 9)))
        assertThat(PeriodCalendar.openPeriod(date(2026, 11, 2), rule(WEEK)))
            .isEqualTo(range(date(2026, 11, 2), date(2026, 11, 9)))
    }
}
