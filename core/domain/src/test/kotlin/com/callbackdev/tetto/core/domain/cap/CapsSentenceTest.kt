package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * The sentence (PLANNING.md §4.6; §4.7, The sentence), with the owner's order of 5 Nov 2026: over,
 * then renewed today, then all under. Today is Monday 9 Nov 2026.
 */
class CapsSentenceTest {
    private val today = date(2026, 11, 9)

    private fun state(
        id: Long,
        amount: Money,
        spent: Money,
        kind: PeriodKind = WEEK,
        lastClosedEnd: LocalDate? = null,
        openStart: LocalDate = CapChanges.openStartForNew(rule(kind), today),
    ): CapState = CapStates.of(
        cap(kind, openStart = openStart, amount = amount, id = id, name = "Cap $id", position = id.toInt()),
        spent,
        rule(kind),
        today,
        EUR,
        lastClosedEnd,
    )

    @Test
    fun `no caps`() {
        assertThat(CapsSentence.of(emptyList())).isEqualTo(CapsSentence.NoCaps)
    }

    @Test
    fun `one cap, under`() {
        val coffee = state(1, euros(20), euros(8))
        assertThat(CapsSentence.of(listOf(coffee))).isEqualTo(CapsSentence.AllUnder(coffee, caps = 1))
    }

    @Test
    fun `all under, the closest is the lowest share left`() {
        val coffee = state(1, euros(20), euros(8)) // 60% left
        val restaurants = state(2, euros(200), euros(188), kind = MONTH) // 6% left, €12
        val groceries = state(3, euros(100), euros(50)) // 50% left
        val sentence = CapsSentence.of(listOf(coffee, restaurants, groceries))
        assertThat(sentence).isEqualTo(CapsSentence.AllUnder(restaurants, caps = 3))
    }

    @Test
    fun `with nothing spent on any cap, none is named the closest`() {
        val fuel = state(1, euros(200), Money.ZERO, kind = MONTH)
        val coffee = state(2, euros(100), Money.ZERO, kind = MONTH)
        assertThat(CapsSentence.of(listOf(fuel, coffee))).isEqualTo(CapsSentence.NothingSpent(caps = 2))
    }

    @Test
    fun `one spend anywhere and the closest is named again`() {
        val fuel = state(1, euros(200), Money.ZERO, kind = MONTH)
        val coffee = state(2, euros(100), euros(1), kind = MONTH)
        assertThat(CapsSentence.of(listOf(fuel, coffee))).isEqualTo(CapsSentence.AllUnder(coffee, caps = 2))
    }

    @Test
    fun `a single cap with nothing spent is still said with its left and its day`() {
        val fuel = state(1, euros(200), Money.ZERO, kind = MONTH)
        assertThat(CapsSentence.of(listOf(fuel))).isEqualTo(CapsSentence.AllUnder(fuel, caps = 1))
    }

    @Test
    fun `ties for the closest go to the first in the reader's order`() {
        val first = state(1, euros(20), euros(10))
        val second = state(2, euros(40), euros(20))
        assertThat(CapsSentence.of(listOf(first, second))).isEqualTo(CapsSentence.AllUnder(first, caps = 2))
        assertThat(CapsSentence.of(listOf(second, first))).isEqualTo(CapsSentence.AllUnder(second, caps = 2))
    }

    @Test
    fun `several over, the first in the reader's order and the others counted`() {
        val coffee = state(1, euros(20), euros(8))
        val shopping = state(2, euros(100), euros(130), kind = MONTH)
        val outings = state(3, euros(50), euros(51))
        val sentence = CapsSentence.of(listOf(coffee, shopping, outings))
        assertThat(sentence).isEqualTo(CapsSentence.Over(shopping, others = 1, caps = 3))
        assertThat((sentence as CapsSentence.Over).first.startsAgain).isEqualTo(date(2026, 12, 1))
    }

    @Test
    fun `every cap over`() {
        val coffee = state(1, euros(20), euros(21))
        val shopping = state(2, euros(100), euros(130))
        assertThat(CapsSentence.of(listOf(coffee, shopping))).isEqualTo(CapsSentence.Over(coffee, others = 1, caps = 2))
    }

    @Test
    fun `a cap renewed today, when none is over`() {
        val coffee = state(1, euros(20), Money.ZERO, lastClosedEnd = today)
        val restaurants = state(2, euros(200), euros(188), kind = MONTH)
        assertThat(CapsSentence.of(listOf(restaurants, coffee)))
            .isEqualTo(CapsSentence.Renewed(coffee, others = 0, caps = 2))
    }

    @Test
    fun `a cap over wins over a cap renewed today`() {
        val coffee = state(1, euros(20), Money.ZERO, lastClosedEnd = today)
        val shopping = state(2, euros(100), euros(130), kind = MONTH)
        assertThat(
            CapsSentence.of(listOf(coffee, shopping)),
        ).isEqualTo(CapsSentence.Over(shopping, others = 0, caps = 2))
    }

    @Test
    fun `a day's cap renews every day, and that is not news`() {
        val coffee = state(1, euros(5), euros(1), kind = DAY, lastClosedEnd = today)
        assertThat(coffee.renewedToday).isTrue()
        assertThat(CapsSentence.of(listOf(coffee))).isEqualTo(CapsSentence.AllUnder(coffee, caps = 1))
    }
}
