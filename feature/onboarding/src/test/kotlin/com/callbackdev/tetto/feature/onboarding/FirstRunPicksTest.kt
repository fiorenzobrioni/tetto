package com.callbackdev.tetto.feature.onboarding

import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The first run's picks (PLANNING.md §11 Phase 3): the grid's order, the amounts, the currency. */
class FirstRunPicksTest {
    private val euro = AppCurrency("EUR", 2)
    private val yen = AppCurrency("JPY", 0)

    @Test
    fun `the twelve areas are the palette's first, and bills are not among them`() {
        assertThat(FirstRunAreas).hasSize(12)
        assertThat(FirstRunAreas).isEqualTo(CapIcon.entries.take(12))
        assertThat(FirstRunAreas).doesNotContain(CapIcon.BILLS)
    }

    @Test
    fun `areas keep the grid's order whatever the order they are picked in`() {
        val picks = FirstRunPicks().toggle(CapIcon.HOME).toggle(CapIcon.GROCERIES).toggle(CapIcon.COFFEE)
        assertThat(picks.areas.map { it.area })
            .containsExactly(CapIcon.GROCERIES, CapIcon.COFFEE, CapIcon.HOME)
            .inOrder()
        assertThat(picks.areas.all { it.kind == PeriodKind.MONTH }).isTrue()
    }

    @Test
    fun `an area picked again is dropped with what was typed for it`() {
        val picks = FirstRunPicks().toggle(CapIcon.FUEL).withAmount(CapIcon.FUEL, Money(8_000))
        val again = picks.toggle(CapIcon.FUEL).toggle(CapIcon.FUEL)
        assertThat(again.areas.single()).isEqualTo(AreaPick(CapIcon.FUEL))
    }

    @Test
    fun `complete once every area has an amount above zero`() {
        var picks = FirstRunPicks()
        assertThat(picks.complete).isFalse()
        picks = picks.toggle(CapIcon.GROCERIES).toggle(CapIcon.FUN)
        assertThat(picks.complete).isFalse()
        picks = picks.withAmount(CapIcon.GROCERIES, Money(30_000))
        assertThat(picks.complete).isFalse()
        picks = picks.withAmount(CapIcon.FUN, Money(5_000)).withKind(CapIcon.FUN, PeriodKind.WEEK)
        assertThat(picks.complete).isTrue()
        assertThat(picks.areas[1]).isEqualTo(AreaPick(CapIcon.FUN, Money(5_000), PeriodKind.WEEK))
    }

    @Test
    fun `a change of currency keeps the figures typed, rescaled where the decimals differ`() {
        val picks = FirstRunPicks()
            .toggle(CapIcon.GROCERIES)
            .toggle(CapIcon.COFFEE)
            .withAmount(CapIcon.GROCERIES, Money(1_250))
            .withAmount(CapIcon.COFFEE, Money(40))
        val inYen = picks.withCurrency(euro, yen)
        assertThat(inYen.currency).isEqualTo("JPY")
        assertThat(inYen.areas.map { it.amount }).containsExactly(Money(13), Money(1)).inOrder()
        assertThat(inYen.withCurrency(yen, euro).areas.map { it.amount })
            .containsExactly(Money(1_300), Money(100))
            .inOrder()
        // An area with no amount yet still has none.
        assertThat(FirstRunPicks().toggle(CapIcon.PETS).withCurrency(euro, yen).areas.single().amount)
            .isEqualTo(Money.ZERO)
    }
}
