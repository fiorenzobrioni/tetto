package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.oneOff
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.callbackdev.tetto.core.model.Share
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek.SUNDAY

/** A period cut short by a change has its share of the cap (PLANNING.md §4.5; owner, 7 Nov 2026). */
class PeriodAmountTest {
    @Test
    fun `a whole period has the whole cap`() {
        for (kind in listOf(DAY, WEEK, MONTH, YEAR)) {
            val whole =
                cap(kind, openStart = CapChanges.openStartForNew(rule(kind), date(2026, 11, 5)), amount = euros(70))
            assertThat(PeriodAmount.of(whole, rule(kind))).isEqualTo(euros(70))
            assertThat(PeriodAmount.partOf(whole, rule(kind))).isNull()
        }
    }

    @Test
    fun `a day turned into a week on a Thursday has four sevenths of it`() {
        val week = cap(WEEK, openStart = date(2026, 11, 5), amount = euros(70))
        assertThat(PeriodAmount.of(week, rule(WEEK))).isEqualTo(euros(40))
        assertThat(PeriodAmount.partOf(week, rule(WEEK))).isEqualTo(Share(4, 7))
    }

    @Test
    fun `the share is rounded down to the minor unit`() {
        // Four sevenths of €20 is €11.428…: €11.42, never a cent the cap did not give.
        val week = cap(WEEK, openStart = date(2026, 11, 5), amount = euros(20))
        assertThat(PeriodAmount.of(week, rule(WEEK))).isEqualTo(euros(11, 42))
    }

    @Test
    fun `a share never comes to nothing, so a period always has a cap to draw`() {
        // ¥300 a year, from 31 December: a day of 365 is 0.8 of a yen, held at ¥1.
        val year = cap(YEAR, openStart = date(2026, 12, 31), amount = Money(300))
        assertThat(PeriodAmount.of(year, rule(YEAR))).isEqualTo(Money(1))
        val state = CapStates.of(year, Money.ZERO, rule(YEAR), date(2026, 12, 31), JPY)
        assertThat(state.amount).isEqualTo(Money(1))
        assertThat(state.left).isEqualTo(Money(1))
        // Spent past it, it is over, as any cap is: never a bar of nothing.
        val over = CapStates.of(year, Money(5), rule(YEAR), date(2026, 12, 31), JPY)
        assertThat(over.overBy).isEqualTo(Money(4))
        assertThat(over.bar.isWhole).isTrue()
    }

    @Test
    fun `a month cut short by a month start moved, and a week by a week start moved`() {
        // The month moved to the 27th after 1 November closed: 1 to 26 November, 26 days of 31.
        val month = cap(MONTH, openStart = date(2026, 11, 1), amount = euros(310))
        assertThat(PeriodAmount.of(month, rule(MONTH, monthStartDay = 27))).isEqualTo(euros(260))
        // The week moved to Sunday after Monday's close: Monday to Saturday, six days of seven.
        val week = cap(WEEK, openStart = date(2026, 11, 2), amount = euros(70))
        assertThat(PeriodAmount.of(week, rule(WEEK, weekStart = SUNDAY))).isEqualTo(euros(60))
    }

    @Test
    fun `a one-off cap's dates are its own, never a share`() {
        val trip = oneOff(date(2026, 11, 5), date(2026, 11, 14), amount = euros(800))
        assertThat(PeriodAmount.of(trip, rule(trip.kind))).isEqualTo(euros(800))
        assertThat(PeriodAmount.partOf(trip, rule(trip.kind))).isNull()
    }

    @Test
    fun `what a cap cut short says is of its share`() {
        val week = cap(WEEK, openStart = date(2026, 11, 5), amount = euros(70), reserve = euros(5))
        val state = CapStates.of(week, euros(15), rule(WEEK), date(2026, 11, 6), EUR)
        assertThat(state.amount).isEqualTo(euros(40))
        assertThat(state.partial).isEqualTo(Share(4, 7))
        // €40 and €5 of reserve, less €15.
        assertThat(state.left).isEqualTo(euros(30))
        assertThat(state.bar).isEqualTo(Share(1_500, 4_500))
    }
}
