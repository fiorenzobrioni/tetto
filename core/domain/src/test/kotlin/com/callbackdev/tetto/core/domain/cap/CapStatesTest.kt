package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.callbackdev.tetto.core.model.Share
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** What a cap says (PLANNING.md §4.6; §4.7, What a cap says). Today is Thursday 5 Nov 2026. */
class CapStatesTest {
    private val today = date(2026, 11, 5)
    private val week = cap(WEEK, openStart = date(2026, 11, 2), amount = euros(20))

    @Test
    fun `left is the cap and the reserve less what was spent`() {
        val state = CapStates.of(week.copy(reserve = euros(5)), euros(12), rule(WEEK), today, EUR)
        assertThat(state.left).isEqualTo(euros(13))
        assertThat(state.over).isFalse()
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 2), date(2026, 11, 9)))
        assertThat(state.daysLeft).isEqualTo(4)
        assertThat(state.lastDay).isEqualTo(date(2026, 11, 8))
        assertThat(state.startsAgain).isEqualTo(date(2026, 11, 9))
    }

    @Test
    fun `over, said as how far`() {
        val state = CapStates.of(week, euros(50), rule(WEEK), today, EUR)
        assertThat(state.over).isTrue()
        assertThat(state.left).isEqualTo(-euros(30))
        assertThat(state.overBy).isEqualTo(euros(30))
        assertThat(state.bar.isWhole).isTrue()
        assertThat(state.pace).isNull()
        assertThat(state.aheadOfDays).isFalse()
    }

    @Test
    fun `exactly zero left is not over`() {
        val state = CapStates.of(week, euros(20), rule(WEEK), today, EUR)
        assertThat(state.left).isEqualTo(Money.ZERO)
        assertThat(state.over).isFalse()
        assertThat(state.overBy).isEqualTo(Money.ZERO)
        assertThat(state.bar).isEqualTo(Share(2000, 2000))
        assertThat(state.pace).isNull()
        assertThat(state.aheadOfDays).isTrue()
    }

    @Test
    fun `the mark for today counts today`() {
        // Thursday is the fourth day of seven.
        val state = CapStates.of(week, euros(8), rule(WEEK), today, EUR)
        assertThat(state.todayMark).isEqualTo(Share(4, 7))
        assertThat(state.bar).isEqualTo(Share(800, 2000))
    }

    @Test
    fun `ahead of the days, the bar past the mark and its fading slack`() {
        // Thursday, 4/7 of the week gone, 4 days left: the line is 4/7 + 1/7 × 4/7 = 32/49, €13.06
        // of a €20 cap; the mark alone (€11.43) is no longer enough.
        assertThat(CapStates.of(week, euros(12), rule(WEEK), today, EUR).aheadOfDays).isFalse()
        assertThat(CapStates.of(week, euros(13, 6), rule(WEEK), today, EUR).aheadOfDays).isFalse()
        assertThat(CapStates.of(week, euros(13, 7), rule(WEEK), today, EUR).aheadOfDays).isTrue()
        // Exactly on the line is not ahead: €32 of a €49 cap is 32/49.
        val exact = week.copy(amount = euros(49))
        assertThat(CapStates.of(exact, euros(32), rule(WEEK), today, EUR).aheadOfDays).isFalse()
        assertThat(CapStates.of(exact, euros(32, 1), rule(WEEK), today, EUR).aheadOfDays).isTrue()
    }

    @Test
    fun `a week's first day forgives a day's worth, its last almost nothing`() {
        val monday = date(2026, 11, 2)
        // Monday: the line is 2/7, €5.71 of €20.
        assertThat(CapStates.of(week, euros(5, 71), rule(WEEK), monday, EUR).aheadOfDays).isFalse()
        assertThat(CapStates.of(week, euros(5, 72), rule(WEEK), monday, EUR).aheadOfDays).isTrue()
        // Sunday: 7/7 and a forty-ninth: a cap not over is never ahead on its last day.
        val sunday = date(2026, 11, 8)
        assertThat(CapStates.of(week, euros(20), rule(WEEK), sunday, EUR).aheadOfDays).isFalse()
    }

    @Test
    fun `a month's big shop on its first days is not a warning, a month nearly spent still is`() {
        val month = cap(MONTH, openStart = date(2026, 11, 1), amount = euros(300))
        // The 5th, 26 days left: the line is 5/30 + 7/30 × 26/30, about 37%; €96.40 is 32%.
        val fifth = date(2026, 11, 5)
        assertThat(CapStates.of(month, euros(96, 40), rule(MONTH), fifth, EUR).aheadOfDays).isFalse()
        // A week's shop on the first day is not one either, where the mark alone said so for a week;
        // twice that still is, for six days instead of twelve.
        val first = date(2026, 11, 1)
        assertThat(CapStates.of(month, euros(75), rule(MONTH), first, EUR).aheadOfDays).isFalse()
        assertThat(CapStates.of(month, euros(150), rule(MONTH), first, EUR).aheadOfDays).isTrue()
        assertThat(CapStates.of(month, euros(120), rule(MONTH), date(2026, 11, 7), EUR).aheadOfDays).isFalse()
        // The 24th, 7 days left: the line is about 85%; 90% spent is ahead.
        val late = date(2026, 11, 24)
        assertThat(CapStates.of(month, euros(270), rule(MONTH), late, EUR).aheadOfDays).isTrue()
        assertThat(CapStates.of(month, euros(240), rule(MONTH), late, EUR).aheadOfDays).isFalse()
    }

    @Test
    fun `the pace rounds down to a whole unit`() {
        // €13 left over four days: €3.25 a day, said "about €3".
        val state = CapStates.of(week.copy(reserve = euros(5)), euros(12), rule(WEEK), today, EUR)
        assertThat(state.pace).isEqualTo(euros(3))
    }

    @Test
    fun `the pace under one unit is not shown`() {
        val state = CapStates.of(week, euros(17), rule(WEEK), today, EUR)
        assertThat(state.left).isEqualTo(euros(3))
        assertThat(state.pace).isNull()
        val justOne = CapStates.of(week, euros(16), rule(WEEK), today, EUR)
        assertThat(justOne.pace).isEqualTo(euros(1))
    }

    @Test
    fun `the pace in a currency without decimals`() {
        val yen = week.copy(amount = Money(5_000))
        val state = CapStates.of(yen, Money(1_001), rule(WEEK), today, JPY)
        // ¥3,999 over four days: ¥999.75, said "about ¥999".
        assertThat(state.pace).isEqualTo(Money(999))
    }

    @Test
    fun `the last day of a period`() {
        val sunday = date(2026, 11, 8)
        val state = CapStates.of(week, euros(5), rule(WEEK), sunday, EUR)
        assertThat(state.daysLeft).isEqualTo(1)
        assertThat(state.pace).isNull()
        assertThat(state.todayMark).isEqualTo(Share(7, 7))
        assertThat(state.lastDay).isEqualTo(sunday)
    }

    @Test
    fun `a day cap has no mark and no pace, and is never ahead`() {
        val coffee = cap(DAY, openStart = today, amount = euros(5))
        val state = CapStates.of(coffee, euros(4), rule(DAY), today, EUR)
        assertThat(state.todayMark).isNull()
        assertThat(state.pace).isNull()
        assertThat(state.aheadOfDays).isFalse()
        assertThat(state.daysLeft).isEqualTo(1)
        assertThat(state.lastDay).isEqualTo(today)
    }

    @Test
    fun `a cap with a reserve and nothing spent`() {
        val state = CapStates.of(week.copy(reserve = euros(7)), Money.ZERO, rule(WEEK), today, EUR)
        assertThat(state.left).isEqualTo(euros(27))
        assertThat(state.bar).isEqualTo(Share(0, 2700))
        assertThat(state.aheadOfDays).isFalse()
        assertThat(state.pace).isEqualTo(euros(6))
    }

    @Test
    fun `a month's and a year's mark`() {
        val month = cap(MONTH, openStart = date(2026, 11, 1), amount = euros(300))
        assertThat(CapStates.of(month, euros(10), rule(MONTH), today, EUR).todayMark).isEqualTo(Share(5, 30))
        val year = cap(YEAR, openStart = date(2026, 1, 1), amount = euros(1_000))
        assertThat(CapStates.of(year, euros(10), rule(YEAR), today, EUR).todayMark).isEqualTo(Share(309, 365))
    }

    @Test
    fun `renewed today, when a period closed today and this one began with it`() {
        val renewed = week.copy(openStart = date(2026, 11, 9))
        val monday = date(2026, 11, 9)
        assertThat(CapStates.of(renewed, Money.ZERO, rule(WEEK), monday, EUR, lastClosedEnd = monday).renewedToday)
            .isTrue()
        // A cap made today renewed nothing.
        assertThat(CapStates.of(renewed, Money.ZERO, rule(WEEK), monday, EUR, lastClosedEnd = null).renewedToday)
            .isFalse()
        assertThat(
            CapStates.of(week, Money.ZERO, rule(WEEK), today, EUR, lastClosedEnd = date(2026, 11, 2)).renewedToday,
        )
            .isFalse()
    }

    @Test
    fun `today before the open period's start (a clock set back) keeps the numbers in range`() {
        val ahead = week.copy(openStart = date(2026, 11, 9))
        val state = CapStates.of(ahead, euros(5), rule(WEEK), today, EUR)
        assertThat(state.period).isEqualTo(DateRange(date(2026, 11, 9), date(2026, 11, 16)))
        assertThat(state.daysLeft).isEqualTo(7)
        assertThat(state.todayMark).isEqualTo(Share(1, 7))
    }
}
