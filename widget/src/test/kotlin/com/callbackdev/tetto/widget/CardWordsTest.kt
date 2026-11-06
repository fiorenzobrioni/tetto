package com.callbackdev.tetto.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.model.AppCurrency
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The cards' words, as the caps screen says them (PLANNING.md §4.6), in both languages. The samples'
 * Wednesday 11 November 2026: Coffee €8 of €20 this week, Groceries €238 of €300 this month (ahead of
 * the days), Fun €46 of €40 this week (over).
 */
@RunWith(AndroidJUnit4::class)
class CardWordsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val euro = AppCurrency("EUR", 2)
    private val caps get() = WidgetSamples.caps(context, euro)
    private val words get() = CardWords(context, euro, WidgetSamples.Today)

    /** The locale's own spaces (a no-break space before "€" in Italian) read as plain ones. */
    private fun String.plain(): String = replace('\u00A0', ' ').replace('\u202F', ' ')

    @Test
    @Config(qualifiers = "en-rGB")
    fun `what is left, its cap and its last day`() {
        val coffee = caps[0]
        assertThat(words.figure(coffee)).isEqualTo("€12 left")
        assertThat(words.amount(coffee)).isEqualTo("€12")
        assertThat(words.detail(coffee)).isEqualTo("of €20 · until Sunday")
        assertThat(words.line(coffee)).isEqualTo("Coffee · until Sunday")
        assertThat(words.paceForms(coffee)).containsExactly("about €2 a day", "€2 a day").inOrder()
        assertThat(words.noteForms(coffee)).containsExactly("about €2 a day")
        assertThat(words.lastWords(coffee, withName = false))
            .containsExactly("of €20 · until Sunday", "of €20\nuntil Sunday", "until Sunday").inOrder()
        assertThat(words.lastWords(coffee, withName = true))
            .containsExactly("Coffee · until Sunday", "Coffee\nuntil Sunday", "Coffee").inOrder()
        assertThat(words.description(coffee)).isEqualTo("Coffee, €12 left of €20, until Sunday, about €2 a day")
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `ahead of the days, said with the pace, the month's last day a date`() {
        val groceries = caps[1]
        assertThat(words.noteForms(groceries).first()).isEqualTo("ahead of the days: about €3 a day")
        assertThat(words.detail(groceries)).isEqualTo("of €300 · until 30 Nov")
        assertThat(words.noteForms(groceries).last()).isEqualTo("ahead of the days")
        assertThat(words.description(groceries)).contains("spending ahead of the days")
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `over is said in words, and when it starts again`() {
        val over = caps[4]
        assertThat(words.figure(over)).isEqualTo("€6 over")
        assertThat(words.word(over)).isEqualTo("over")
        assertThat(words.detail(over)).isEqualTo("of €40 · starts again on Monday")
        assertThat(words.line(over)).isEqualTo("Fun · starts again on Monday")
        assertThat(words.noteForms(over)).containsExactly("forgiven when it starts again")
        assertThat(words.lastWords(over, withName = false).last()).isEqualTo("starts again on Monday")
        assertThat(words.description(over)).isEqualTo("Fun, €6 over its cap of €40, starts again on Monday")
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun `what does not fit, counted`() {
        assertThat(words.more(3)).isEqualTo("3 more")
        assertThat(words.more(3, short = true)).isEqualTo("+3")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian`() {
        val coffee = caps[0]
        val over = caps[4]
        assertThat(words.figure(coffee).plain()).isEqualTo("ancora 12 €")
        assertThat(words.detail(coffee).plain()).isEqualTo("su 20 € · fino a domenica")
        assertThat(words.line(coffee)).isEqualTo("Caffè · fino a domenica")
        assertThat(words.figure(over).plain()).isEqualTo("sotto di 6 €")
        assertThat(words.detail(over).plain()).isEqualTo("su 40 € · riparte lunedì")
        assertThat(words.wordAbove).isTrue()
        assertThat(words.more(1)).isEqualTo("altro 1")
        assertThat(words.more(3)).isEqualTo("altri 3")
    }
}
