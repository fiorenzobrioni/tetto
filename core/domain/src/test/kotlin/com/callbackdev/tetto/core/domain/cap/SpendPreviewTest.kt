package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.cap.SpendPreview.Kind
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The numpad's preview (PLANNING.md §4.6). */
class SpendPreviewTest {
    @Test
    fun `a spend that fits says what is left`() {
        assertThat(SpendPreview.of(left = euros(20), typed = euros(5))).isEqualTo(SpendPreview(Kind.FITS, euros(15)))
    }

    @Test
    fun `a spend to exactly zero fits`() {
        assertThat(SpendPreview.of(left = euros(20), typed = euros(20))).isEqualTo(SpendPreview(Kind.FITS, Money.ZERO))
    }

    @Test
    fun `a spend that goes over says how far`() {
        assertThat(SpendPreview.of(left = euros(20), typed = euros(50)))
            .isEqualTo(SpendPreview(Kind.GOES_OVER, euros(30)))
        assertThat(SpendPreview.of(left = Money.ZERO, typed = euros(0, 1)))
            .isEqualTo(SpendPreview(Kind.GOES_OVER, euros(0, 1)))
    }

    @Test
    fun `a cap already over stays over, by more`() {
        assertThat(SpendPreview.of(left = -euros(30), typed = euros(15)))
            .isEqualTo(SpendPreview(Kind.STILL_OVER, euros(45)))
    }

    @Test
    fun `nothing typed yet says the cap as it stands`() {
        assertThat(SpendPreview.of(left = euros(12), typed = Money.ZERO)).isEqualTo(SpendPreview(Kind.FITS, euros(12)))
        assertThat(SpendPreview.of(left = -euros(30), typed = Money.ZERO))
            .isEqualTo(SpendPreview(Kind.STILL_OVER, euros(30)))
    }

    @Test
    fun `a refund grows what is left, and may bring a cap back under`() {
        assertThat(
            SpendPreview.ofRefund(euros(16), euros(4)),
        ).isEqualTo(SpendPreview(SpendPreview.Kind.FITS, euros(20)))
        assertThat(SpendPreview.ofRefund(-euros(10), euros(4)))
            .isEqualTo(SpendPreview(SpendPreview.Kind.STILL_OVER, euros(6)))
        assertThat(
            SpendPreview.ofRefund(-euros(10), euros(12)),
        ).isEqualTo(SpendPreview(SpendPreview.Kind.FITS, euros(2)))
    }
}
