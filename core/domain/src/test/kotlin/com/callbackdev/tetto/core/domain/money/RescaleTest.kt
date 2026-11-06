package com.callbackdev.tetto.core.domain.money

import com.callbackdev.tetto.core.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A change of currency relabels; where the decimals differ, the figure is kept (PLANNING.md §4.1). */
class RescaleTest {
    @Test
    fun `the rescale between currencies of different decimals, euro to yen, rounds half up`() {
        assertThat(Rescale.amount(Money(1250), fromDigits = 2, toDigits = 0)).isEqualTo(Money(13))
        assertThat(Rescale.amount(Money(1249), fromDigits = 2, toDigits = 0)).isEqualTo(Money(12))
        assertThat(Rescale.amount(Money(1251), fromDigits = 2, toDigits = 0)).isEqualTo(Money(13))
    }

    @Test
    fun `yen to euro keeps the figure`() {
        assertThat(Rescale.amount(Money(13), fromDigits = 0, toDigits = 2)).isEqualTo(Money(1300))
    }

    @Test
    fun `three decimals to two round half up`() {
        assertThat(Rescale.amount(Money(1_255), fromDigits = 3, toDigits = 2)).isEqualTo(Money(126))
        assertThat(Rescale.amount(Money(1_254), fromDigits = 3, toDigits = 2)).isEqualTo(Money(125))
    }

    @Test
    fun `an amount above zero that rounds to zero becomes the smallest amount of the new currency`() {
        assertThat(Rescale.amount(Money(40), fromDigits = 2, toDigits = 0)).isEqualTo(Money(1))
        assertThat(Rescale.amount(Money(1), fromDigits = 2, toDigits = 0)).isEqualTo(Money(1))
        assertThat(Rescale.amount(Money(4), fromDigits = 3, toDigits = 2)).isEqualTo(Money(1))
        // Nothing stays nothing: an empty reserve is not given a yen.
        assertThat(Rescale.amount(Money.ZERO, fromDigits = 2, toDigits = 0)).isEqualTo(Money.ZERO)
    }

    @Test
    fun `the same decimals change nothing`() {
        assertThat(Rescale.amount(Money(1250), fromDigits = 2, toDigits = 2)).isEqualTo(Money(1250))
    }

    @Test
    fun `what the numpad typed is held to what it can type back, a closed line's sum is not`() {
        // 999,999,999.500 dinars would be a ten-digit yen amount: held to nine nines.
        val largestDinars = Money(999_999_999_500)
        assertThat(Rescale.amount(largestDinars, fromDigits = 3, toDigits = 0)).isEqualTo(Money(1_000_000_000))
        assertThat(Rescale.typed(largestDinars, fromDigits = 3, toDigits = 0)).isEqualTo(Money(999_999_999))
        // Below the edge, the two agree.
        assertThat(Rescale.typed(Money(1250), fromDigits = 2, toDigits = 0)).isEqualTo(Money(13))
        assertThat(Rescale.typed(Money(40), fromDigits = 2, toDigits = 0)).isEqualTo(Money(1))
        // The same decimals hold nothing back: the amount is as it was typed.
        assertThat(Rescale.typed(Money(1_000_000_000_00), fromDigits = 2, toDigits = 2))
            .isEqualTo(Money(1_000_000_000_00))
    }
}
