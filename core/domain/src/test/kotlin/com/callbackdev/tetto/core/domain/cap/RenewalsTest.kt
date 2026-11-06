package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.Renewal
import com.callbackdev.tetto.core.model.Renewal.Kind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** What the day a period renews says of the one that ended (owner, 6 Nov 2026). */
class RenewalsTest {
    private val week = DateRange(date(2026, 10, 26), date(2026, 11, 2))

    private fun line(spent: Money, before: Money, periods: Int = 1): ClosedPeriod {
        val outcome = Rollover.close(euros(20), before, spent)
        return ClosedPeriod(1, week, euros(20), spent, before, outcome.reserveAfter, outcome.forgiven, periods)
    }

    @Test
    fun `forgiveness first`() {
        assertThat(Renewals.of(line(euros(32), euros(4)))).isEqualTo(Renewal(Kind.FORGIVEN, euros(8)))
    }

    @Test
    fun `an overspend the reserve covered`() {
        assertThat(Renewals.of(line(euros(26), euros(10)))).isEqualTo(Renewal(Kind.COVERED, euros(6)))
    }

    @Test
    fun `under the cap, what went to the reserve`() {
        assertThat(Renewals.of(line(euros(16), euros(2)))).isEqualTo(Renewal(Kind.SAVED, euros(4)))
    }

    @Test
    fun `under the cap with the reserve already full`() {
        assertThat(Renewals.of(line(euros(5), euros(20)))).isEqualTo(Renewal(Kind.RESERVE_FULL, euros(20)))
    }

    @Test
    fun `right on the cap`() {
        assertThat(Renewals.of(line(euros(20), Money.ZERO))).isEqualTo(Renewal(Kind.EVEN, euros(20)))
    }

    @Test
    fun `said only on the day the period renews`() {
        val renewed = cap(WEEK, openStart = date(2026, 11, 2), amount = euros(20), reserve = euros(4))
        val closed = line(euros(16), Money.ZERO).copy(reserveAfter = euros(4))
        val monday = CapStates.of(renewed, Money.ZERO, rule(WEEK), date(2026, 11, 2), EUR, date(2026, 11, 2), closed)
        assertThat(monday.renewal).isEqualTo(Renewal(Kind.SAVED, euros(4)))
        val tuesday = CapStates.of(renewed, Money.ZERO, rule(WEEK), date(2026, 11, 3), EUR, date(2026, 11, 2), closed)
        assertThat(tuesday.renewal).isNull()
    }
}
