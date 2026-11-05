package com.callbackdev.tetto.core.designsystem.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.Collator
import java.util.Locale

/** The currencies a cap can count in, as the picker lists and finds them (PLANNING.md §4.1). */
class CurrenciesTest {
    private val italian = Locale.ITALY

    @Test
    fun `the currencies some region uses today, and nothing a cap cannot count in`() {
        val codes = Currencies.all(Locale.UK).map { it.code }
        assertThat(codes).containsAtLeast("EUR", "USD", "GBP", "CHF", "JPY", "PLN", "KWD")
        // A currency of the past, a fund's accounting unit, gold, the test code.
        assertThat(codes).containsNoneOf("ITL", "DEM", "CLF", "BOV", "XAU", "XXX")
        assertThat(codes).containsNoDuplicates()
    }

    @Test
    fun `the reader's own is listed even when no region uses it`() {
        assertThat(Currencies.all(Locale.UK, keep = "CLF").map { it.code }).contains("CLF")
        assertThat(Currencies.all(Locale.UK, keep = "XXX").map { it.code }).doesNotContain("XXX")
    }

    @Test
    fun `named in the reader's language, in its alphabetical order`() {
        val all = Currencies.all(italian)
        val collator = Collator.getInstance(italian)
        assertThat(all.map { it.name }).isInOrder(collator)
        assertThat(Currencies.name("EUR", italian)).isEqualTo("Euro")
        assertThat(Currencies.inSentence("JPY", italian)).isEqualTo("yen giapponese")
    }

    @Test
    fun `found by a part of the name, accents and case aside, the start of the code, or the symbol`() {
        fun found(query: String, locale: Locale) = Currencies.all(locale).filter { it.matches(query) }.map { it.code }
        assertThat(found("zloty", italian)).containsExactly("PLN")
        assertThat(found("YEN", Locale.UK)).contains("JPY")
        assertThat(found("eu", Locale.UK)).contains("EUR")
        assertThat(found("$", Locale.US)).contains("USD")
        assertThat(found("  ", Locale.UK)).hasSize(Currencies.all(Locale.UK).size)
        assertThat(found("nothing like it", Locale.UK)).isEmpty()
    }
}
