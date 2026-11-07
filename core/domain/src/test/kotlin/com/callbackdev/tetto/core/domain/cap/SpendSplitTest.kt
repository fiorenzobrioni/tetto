package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Spend
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

/**
 * How an open period's spends divide at a change that keeps the days before it (PLANNING.md §4.5;
 * owner, 7 Nov 2026). The week of Monday 2 November, cut on Thursday 5.
 */
class SpendSplitTest {
    private val cut = Instant.parse("2026-11-05T00:00:00Z")
    private val monday = Instant.parse("2026-11-02T10:00:00Z")
    private val tuesday = Instant.parse("2026-11-03T10:00:00Z")
    private val thursday = Instant.parse("2026-11-05T10:00:00Z")
    private val friday = Instant.parse("2026-11-06T10:00:00Z")

    private fun spend(id: Long, amount: Money, at: Instant) = Spend(id, capId = 1, amount, at, date(2026, 11, 2))

    @Test
    fun `each spend goes by its instant`() {
        val split = SpendSplits.at(
            listOf(spend(1, euros(15), monday), spend(2, euros(10), thursday), spend(3, euros(4), friday)),
            cut,
        )
        assertThat(split).isEqualTo(SpendSplit(moving = listOf(2, 3), divided = null, keptSpent = euros(15)))
    }

    @Test
    fun `a refund of the days kept's spend, given after the cut, stays with them`() {
        // Monday's €50 given back on Thursday: the days kept spent nothing, the new period starts at nothing.
        val split = SpendSplits.at(listOf(spend(1, euros(50), monday), spend(2, -euros(50), thursday)), cut)
        assertThat(split).isEqualTo(SpendSplit(moving = emptyList(), divided = null, keptSpent = Money.ZERO))
    }

    @Test
    fun `a refund that gave back spends on both sides is cut in two`() {
        // Monday's €40 and Thursday's €20, €50 given back on Friday: €20 of it gives back Thursday's
        // spend, on its own side of the cut; the €30 more gives back Monday's.
        val split = SpendSplits.at(
            listOf(spend(1, euros(40), monday), spend(2, euros(20), thursday), spend(3, -euros(50), friday)),
            cut,
        )
        assertThat(split.moving).containsExactly(2L, 3L)
        assertThat(split.divided).isEqualTo(DividedRefund(spendId = 3, kept = -euros(30), moving = -euros(20)))
        assertThat(split.keptSpent).isEqualTo(euros(10))
    }

    @Test
    fun `the refunds nearest the cut cross it first, whole, then in part`() {
        val split = SpendSplits.at(
            listOf(
                spend(1, euros(30), monday),
                spend(2, -euros(10), thursday),
                spend(3, -euros(15), friday),
                spend(4, euros(5), friday),
            ),
            cut,
        )
        // The new period is €20 below nothing: Thursday's €10 stays whole, €10 of Friday's €15 stays.
        assertThat(split.moving).containsExactly(3L, 4L)
        assertThat(split.divided).isEqualTo(DividedRefund(spendId = 3, kept = -euros(10), moving = -euros(5)))
        assertThat(split.keptSpent).isEqualTo(euros(10))
    }

    @Test
    fun `days kept below nothing give the new period's spends back, as far as it spent`() {
        // A spend deleted after its refund, while Thursday's covered the period: Tuesday's €30 back
        // gives back Monday's €10 and €20 of Thursday's €25.
        val split = SpendSplits.at(
            listOf(spend(1, euros(10), monday), spend(2, -euros(30), tuesday), spend(3, euros(25), thursday)),
            cut,
        )
        assertThat(split.moving).containsExactly(3L, 2L)
        assertThat(split.divided).isEqualTo(DividedRefund(spendId = 2, kept = -euros(10), moving = -euros(20)))
        assertThat(split.keptSpent).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a period an older Tetto left below nothing keeps the new one at nothing`() {
        // €10 spent, €30 given back, all after the cut: the new period at nothing, the rest on the days kept.
        val split = SpendSplits.at(listOf(spend(1, euros(10), thursday), spend(2, -euros(30), friday)), cut)
        assertThat(split.divided).isEqualTo(DividedRefund(spendId = 2, kept = -euros(20), moving = -euros(10)))
        assertThat(split.keptSpent).isEqualTo(Money.ZERO)
    }

    @Test
    fun `every amount is kept, and the two sides add up to the period`() {
        val spends = listOf(
            spend(1, euros(40), monday),
            spend(2, -euros(5), tuesday),
            spend(3, euros(20), thursday),
            spend(4, -euros(50), friday),
        )
        val split = SpendSplits.at(spends, cut)
        val moved = spends.filter { it.id in split.moving }.map { spend ->
            split.divided?.takeIf { it.spendId == spend.id }?.moving ?: spend.amount
        }
        val stayed = spends.filter { it.id !in split.moving }.map { it.amount } + listOfNotNull(split.divided?.kept)
        assertThat((moved + stayed).fold(Money.ZERO, Money::plus)).isEqualTo(euros(5))
        assertThat(moved.fold(Money.ZERO, Money::plus)).isEqualTo(Money.ZERO)
        assertThat(split.keptSpent).isEqualTo(euros(5))
    }
}
