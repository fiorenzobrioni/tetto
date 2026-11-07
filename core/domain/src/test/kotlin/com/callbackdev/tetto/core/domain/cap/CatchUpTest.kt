package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Closing every ended period, in order (PLANNING.md §4.4; §4.7, The catch-up). */
class CatchUpTest {
    private fun Cap.after(plan: CatchUpPlan) = copy(openStart = plan.openStart, reserve = plan.reserve)

    @Test
    fun `nothing ended, nothing to do`() {
        val coffee = cap(WEEK, openStart = date(2026, 11, 2))
        assertThat(CatchUp.plan(coffee, euros(5), rule(WEEK), today = date(2026, 11, 8))).isNull()
    }

    @Test
    fun `a cap created today has nothing to close`() {
        val today = date(2026, 11, 5)
        for (kind in listOf(DAY, WEEK, MONTH)) {
            val created = cap(kind, openStart = CapChanges.openStartForNew(rule(kind), today))
            assertThat(CatchUp.plan(created, Money.ZERO, rule(kind), today)).isNull()
        }
    }

    @Test
    fun `one period ended`() {
        val coffee = cap(WEEK, openStart = date(2026, 10, 26), reserve = euros(2))
        val plan = checkNotNull(CatchUp.plan(coffee, euros(12), rule(WEEK), today = date(2026, 11, 2)))
        assertThat(plan.closed).containsExactly(
            ClosedPeriod(
                capId = 1,
                range = DateRange(date(2026, 10, 26), date(2026, 11, 2)),
                cap = euros(20),
                spent = euros(12),
                reserveBefore = euros(2),
                reserveAfter = euros(10),
                forgiven = Money.ZERO,
                periods = 1,
            ),
        )
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 2))
        assertThat(plan.reserve).isEqualTo(euros(10))
    }

    @Test
    fun `several ended, the empty ones after the first folded into one line`() {
        val groceries = cap(WEEK, openStart = date(2026, 10, 5), amount = euros(100))
        val plan = checkNotNull(CatchUp.plan(groceries, euros(130), rule(WEEK), today = date(2026, 11, 5)))
        assertThat(plan.closed).hasSize(2)
        val (first, run) = plan.closed
        assertThat(first.range).isEqualTo(DateRange(date(2026, 10, 5), date(2026, 10, 12)))
        assertThat(first.forgiven).isEqualTo(euros(30))
        assertThat(first.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(run.range).isEqualTo(DateRange(date(2026, 10, 12), date(2026, 11, 2)))
        assertThat(run.periods).isEqualTo(3)
        assertThat(run.spent).isEqualTo(Money.ZERO)
        assertThat(run.reserveBefore).isEqualTo(Money.ZERO)
        // At the limit after one empty week: it never grows past one period's cap.
        assertThat(run.reserveAfter).isEqualTo(euros(100))
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 2))
    }

    @Test
    fun `a run of 400 empty daily periods writes two lines and leaves the reserve at its limit`() {
        val coffee = cap(DAY, openStart = date(2025, 10, 1), amount = euros(3))
        val today = date(2025, 10, 1).plusDays(401)
        val plan = checkNotNull(CatchUp.plan(coffee, euros(2), rule(DAY), today))
        assertThat(plan.closed).hasSize(2)
        assertThat(plan.closed[0].periods).isEqualTo(1)
        assertThat(plan.closed[0].reserveAfter).isEqualTo(euros(1))
        assertThat(plan.closed[1].periods).isEqualTo(400)
        assertThat(plan.closed[1].range).isEqualTo(DateRange(date(2025, 10, 2), today))
        assertThat(plan.reserve).isEqualTo(euros(3))
        assertThat(plan.openStart).isEqualTo(today)
    }

    @Test
    fun `idempotence, a second catch-up does nothing`() {
        val coffee = cap(DAY, openStart = date(2026, 11, 1))
        val today = date(2026, 11, 5)
        val plan = checkNotNull(CatchUp.plan(coffee, euros(4), rule(DAY), today))
        assertThat(CatchUp.plan(coffee.after(plan), Money.ZERO, rule(DAY), today)).isNull()
    }

    @Test
    fun `the lines follow each other without a gap`() {
        val month = rule(MONTH, monthStartDay = 31)
        val rent = cap(MONTH, openStart = date(2026, 1, 31), amount = euros(50))
        val plan = checkNotNull(CatchUp.plan(rent, euros(10), month, today = date(2026, 6, 1)))
        assertThat(plan.closed.first().range.start).isEqualTo(date(2026, 1, 31))
        assertThat(plan.closed.first().range.endExclusive).isEqualTo(plan.closed.last().range.start)
        assertThat(plan.closed.last().range.endExclusive).isEqualTo(date(2026, 5, 31))
        assertThat(plan.closed.last().periods).isEqualTo(3)
        assertThat(plan.openStart).isEqualTo(date(2026, 5, 31))
    }

    @Test
    fun `today before the open period's start (a clock set back, a flight west) closes nothing`() {
        val coffee = cap(DAY, openStart = date(2026, 11, 7))
        assertThat(CatchUp.plan(coffee, euros(4), rule(DAY), today = date(2026, 11, 6))).isNull()
        val weekly = cap(WEEK, openStart = date(2026, 11, 9))
        assertThat(CatchUp.plan(weekly, euros(4), rule(WEEK), today = date(2026, 11, 5))).isNull()
    }

    @Test
    fun `a flight east across two midnights closes the day left and the day skipped`() {
        // Rome 22:00 on the 5th, Auckland 10:00 on the 7th: the 6th never had a morning.
        val coffee = cap(DAY, openStart = date(2026, 11, 5), amount = euros(3))
        val plan = checkNotNull(CatchUp.plan(coffee, euros(4), rule(DAY), today = date(2026, 11, 7)))
        assertThat(plan.closed.map { it.range }).containsExactly(
            DateRange(date(2026, 11, 5), date(2026, 11, 6)),
            DateRange(date(2026, 11, 6), date(2026, 11, 7)),
        ).inOrder()
        assertThat(plan.closed[0].forgiven).isEqualTo(euros(1))
        assertThat(plan.closed[1].reserveAfter).isEqualTo(euros(3))
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 7))
    }

    @Test
    fun `an open period that began mid-way closes at the rule's end`() {
        // A weekly cap whose open period began on a Thursday (§4.5).
        val coffee = cap(WEEK, openStart = date(2026, 11, 5))
        val plan = checkNotNull(CatchUp.plan(coffee, euros(4), rule(WEEK), today = date(2026, 11, 9)))
        assertThat(plan.closed.single().range).isEqualTo(DateRange(date(2026, 11, 5), date(2026, 11, 9)))
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 9))
    }

    @Test
    fun `a period a change cut short closes on its share, the reserve held to a whole cap`() {
        // A daily cap turned weekly on Thursday (owner, 7 Nov 2026): Thursday to Sunday is four
        // sevenths of €70, €40; €30 spent leaves €10 for the reserve, a whole week's cap its limit.
        val coffee = cap(WEEK, openStart = date(2026, 11, 5), amount = euros(70), reserve = euros(65))
        val plan = checkNotNull(CatchUp.plan(coffee, euros(30), rule(WEEK), today = date(2026, 11, 9)))
        val line = plan.closed.single()
        assertThat(line.cap).isEqualTo(euros(40))
        assertThat(line.reserveAfter).isEqualTo(euros(70))
        assertThat(line.forgiven).isEqualTo(Money.ZERO)
        // Over its share and the reserve: what neither covers is forgiven.
        val over =
            checkNotNull(CatchUp.plan(coffee.copy(reserve = Money.ZERO), euros(55), rule(WEEK), date(2026, 11, 9)))
        assertThat(over.closed.single().forgiven).isEqualTo(euros(15))
        // The week after is whole.
        assertThat(PeriodAmount.of(coffee.after(plan), rule(WEEK))).isEqualTo(euros(70))
    }

    @Test
    fun `the cap in force is the limit at the close`() {
        val coffee = cap(WEEK, openStart = date(2026, 10, 26), amount = euros(20), reserve = euros(80))
        val plan = checkNotNull(CatchUp.plan(coffee, euros(10), rule(WEEK), today = date(2026, 11, 2)))
        assertThat(plan.reserve).isEqualTo(euros(20))
        assertThat(plan.closed.single().cap).isEqualTo(euros(20))
    }
}
