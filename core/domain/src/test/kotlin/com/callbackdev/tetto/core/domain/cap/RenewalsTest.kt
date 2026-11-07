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

    /**
     * The days a change of kind kept (§4.5): a month's €260 of reserve, held to a week's €80 cap,
     * on 8 days of a €300 month (€80).
     */
    private fun kept(spent: Money): ClosedPeriod {
        val outcome = Rollover.close(euros(80), euros(260), spent, limit = euros(80))
        val range = DateRange(date(2026, 11, 1), date(2026, 11, 9))
        return ClosedPeriod(1, range, euros(80), spent, euros(260), outcome.reserveAfter, outcome.forgiven, 1)
    }

    @Test
    fun `a reserve held to a lower cap by a change is full, never an overspend covered`() {
        assertThat(Renewals.of(kept(euros(30)))).isEqualTo(Renewal(Kind.RESERVE_FULL, euros(80)))
        // Right on the share, the reserve held down all the same.
        assertThat(Renewals.of(kept(euros(80)))).isEqualTo(Renewal(Kind.RESERVE_FULL, euros(80)))
    }

    @Test
    fun `over the cap with the reserve held down, covered is what the reserve paid, not what the change took`() {
        // €100 spent of €80: the reserve paid €20, and the change held what was left to €80.
        val line = kept(euros(100))
        assertThat(line.reserveAfter).isEqualTo(euros(80))
        assertThat(Renewals.of(line)).isEqualTo(Renewal(Kind.COVERED, euros(20)))
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
