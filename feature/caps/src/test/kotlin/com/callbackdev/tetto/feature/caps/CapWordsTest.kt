package com.callbackdev.tetto.feature.caps

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.domain.cap.SpendPreview
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * The caps' words (PLANNING.md §4.6): what a tile shows (its sentence for TalkBack is
 * [CapsScreenTest]'s), the renewal, the preview, the last periods' lines, in both languages.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rGB")
class CapWordsTest {
    @get:Rule val compose = createComposeRule()

    private fun words(
        currency: AppCurrency = CapsSample.EUR,
        content: @Composable (MoneyFormat) -> List<String?>,
    ): List<String?> {
        val out = mutableListOf<String?>()
        compose.setContent {
            val format = rememberMoneyFormat(currency)
            Column {
                content(format).forEach { word ->
                    out += word
                    Text(word.orEmpty(), Modifier.testTag("word"))
                }
            }
        }
        compose.waitForIdle()
        return out
    }

    private val today = CapsSample.today

    @Test
    fun `a tile shows what is left, the cap, its last day, the pace and the reserve`() {
        val coffee = CapsSample.coffee
        assertThat(
            words {
                listOf(
                    leftWords(coffee, it),
                    detailWords(coffee, it, today),
                    paceWords(coffee, it),
                    reserveWords(coffee, it),
                )
            },
        ).containsExactly("€16 left", "of €20 · until Sunday", "about €4 a day", "+€4 reserve").inOrder()
    }

    @Test
    fun `cents are said only where there are some, and a spend always in full`() {
        assertThat(words { listOf(leftWords(CapsSample.groceries, it), it.full(euros(5)), it.short(euros(5))) })
            .containsExactly("€203.60 left", "€5.00", "€5").inOrder()
    }

    @Test
    fun `over, the figure and when it starts again`() {
        val shopping = CapsSample.shopping
        assertThat(words { listOf(leftWords(shopping, it), detailWords(shopping, it, today)) })
            .containsExactly("€30 over", "of €100 · starts again on 1 Dec").inOrder()
    }

    @Test
    fun `until names the last day, by how near it is`() {
        val near = words {
            listOf(
                untilWords(today, today),
                untilWords(today.plusDays(1), today),
                untilWords(today.plusDays(3), today),
                untilWords(today.plusDays(7), today),
                untilWords(LocalDate.of(2027, 1, 31), today),
                againWords(today.plusDays(1), today),
                againWords(today.plusDays(4), today),
            )
        }
        assertThat(near).containsExactly(
            "today",
            "until tomorrow",
            "until Sunday",
            "until 12 Nov",
            "until 31 Jan 2027",
            "tomorrow",
            "on Monday",
        ).inOrder()
    }

    @Test
    fun `the preview says what the spend would leave`() {
        assertThat(
            words {
                listOf(
                    previewWords(SpendPreview.of(euros(20), euros(5)), it),
                    previewWords(SpendPreview.of(euros(20), euros(50)), it),
                    previewWords(SpendPreview.of(-euros(30), euros(15)), it),
                )
            },
        ).containsExactly("€15 left", "€30 over", "Still €45 over").inOrder()
    }

    @Test
    fun `the last periods' lines tell the reserve's story`() {
        val lines = words {
            listOf(
                lineWords(CapsSample.coffeeLines[0], PeriodKind.WEEK, it, today),
                lineWords(CapsSample.coffeeLines[1], PeriodKind.WEEK, it, today),
                lineWords(
                    CapsSample.line(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 11, 1),
                        spent = euros(130),
                        before = euros(0),
                        after = euros(0),
                        cap = euros(100),
                        forgiven = euros(30),
                    ),
                    PeriodKind.MONTH,
                    it,
                    today,
                ),
                lineWords(
                    CapsSample.line(
                        LocalDate.of(2026, 9, 28),
                        LocalDate.of(2026, 10, 19),
                        spent = euros(0),
                        before = euros(0),
                        after = euros(20),
                        periods = 3,
                    ),
                    PeriodKind.WEEK,
                    it,
                    today,
                ),
                lineWords(
                    CapsSample.line(today.minusDays(1), today, euros(20), euros(3), euros(3)),
                    PeriodKind.DAY,
                    it,
                    today,
                ),
            )
        }
        // A range's dash stands between thin spaces, the locale's typography: compared as plain ones.
        assertThat(lines.map { it?.replace(Regex("[\\u2009\\u202F\\u00A0]"), " ") }).containsExactly(
            "26 Oct – 1 Nov: €16 of €20, +€4 to the reserve",
            "19–25 Oct: €26 of €20, €6 from the reserve",
            "1–31 Oct: €130 of €100, €30 forgiven",
            "28 Sept – 18 Oct: nothing spent in 3 weeks, the reserve at €20",
            "4 Nov: €20 of €20",
        ).inOrder()
    }

    @Test
    fun `a currency without decimals`() {
        val yen = CapsSample.state(
            9,
            "Ramen",
            CapIcon.EATING_OUT,
            euros(50),
            PeriodKind.WEEK,
            euros(10),
            currency = CapsSample.JPY,
        )
        assertThat(words(CapsSample.JPY) { listOf(leftWords(yen, it), it.full(yen.cap.amount)) })
            .containsExactly("JP¥4,000 left", "JP¥5,000").inOrder()
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian, with the weekday before a date and no article to agree`() {
        val shopping = CapsSample.shopping
        val near = words {
            listOf(
                untilWords(today.plusDays(3), today),
                untilWords(LocalDate.of(2026, 11, 30), today),
                detailWords(shopping, it, today),
                leftWords(CapsSample.coffee, it),
            )
        }
        assertThat(near).containsExactly(
            "fino a domenica",
            "fino a lunedì 30 nov",
            "su 100 € · riparte martedì 1 dic",
            "ancora 16 €",
        ).inOrder()
    }
}
