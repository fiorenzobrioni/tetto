package com.callbackdev.tetto.core.domain.widget

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Which caps a card shows, and what «In words» says of its cap. Today is Wednesday 11 Nov 2026. */
class WidgetCapsTest {
    private val today = date(2026, 11, 11)

    private fun state(
        id: Long,
        amount: Money,
        spent: Money,
        kind: PeriodKind = PeriodKind.WEEK,
        reserve: Money = Money.ZERO,
    ) = CapStates.of(
        cap(
            kind,
            openStart = CapChanges.openStartForNew(rule(kind), today),
            amount = amount,
            reserve = reserve,
            id = id,
            name = "Cap $id",
            position = id.toInt(),
        ),
        spent,
        rule(kind),
        today,
        EUR,
        null,
    )

    private val coffee = state(1, euros(20), euros(8))
    private val dinners = state(2, euros(100), euros(90))
    private val groceries = state(3, euros(200), euros(50))

    @Test
    fun `at a glance shows every cap, or the reader's choice in the reader's order`() {
        val all = listOf(coffee, dinners, groceries)
        assertThat(WidgetCaps.glance(all, null)).isEqualTo(all)
        assertThat(WidgetCaps.glance(all, setOf(3L, 1L))).containsExactly(coffee, groceries).inOrder()
    }

    @Test
    fun `at a glance whose chosen caps are all gone shows every cap`() {
        val all = listOf(coffee, groceries)
        assertThat(WidgetCaps.glance(all, setOf(9L))).isEqualTo(all)
    }

    @Test
    fun `in words shows the chosen cap, or none when it is gone`() {
        val all = listOf(coffee, dinners)
        assertThat(WidgetCaps.words(all, 1L)).isEqualTo(coffee)
        assertThat(WidgetCaps.words(all, 9L)).isNull()
        assertThat(WidgetCaps.words(emptyList(), null)).isNull()
    }

    @Test
    fun `in words with no choice shows the closest to its cap`() {
        assertThat(WidgetCaps.words(listOf(coffee, dinners, groceries), null)).isEqualTo(dinners)
    }

    @Test
    fun `in words with no choice shows the first cap over`() {
        val over = state(4, euros(10), euros(14))
        assertThat(WidgetCaps.words(listOf(coffee, dinners, over), null)).isEqualTo(over)
    }

    @Test
    fun `a tie goes to the first in the reader's order`() {
        val a = state(5, euros(20), euros(10))
        val b = state(6, euros(40), euros(20))
        assertThat(WidgetCaps.words(listOf(a, b), null)).isEqualTo(a)
    }

    @Test
    fun `the note says the pace, and whether spending runs ahead of the days`() {
        // Wednesday, three days of seven gone: €8 of €20 is under the days' share.
        assertThat(CapNote.of(coffee)).isEqualTo(CapNote.Pace(euros(2), date(2026, 11, 15), ahead = false))
        assertThat(CapNote.of(dinners)).isEqualTo(CapNote.Pace(euros(2), date(2026, 11, 15), ahead = true))
    }

    @Test
    fun `over, the note says what is forgiven when it starts again`() {
        val over = state(4, euros(10), euros(14))
        assertThat(CapNote.of(over)).isEqualTo(CapNote.Forgiven(date(2026, 11, 16)))
    }

    @Test
    fun `a day's cap says its reserve, or nothing`() {
        assertThat(CapNote.of(state(7, euros(10), euros(2), PeriodKind.DAY))).isNull()
        assertThat(CapNote.of(state(8, euros(10), euros(2), PeriodKind.DAY, reserve = euros(3))))
            .isEqualTo(CapNote.Reserve(euros(3)))
    }

    @Test
    fun `ahead with no pace to say`() {
        val ahead = state(9, euros(10), euros(8))
        assertThat(CapNote.of(ahead)).isEqualTo(CapNote.Ahead)
    }
}
