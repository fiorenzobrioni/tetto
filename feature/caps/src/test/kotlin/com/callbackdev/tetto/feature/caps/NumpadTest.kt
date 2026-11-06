package com.callbackdev.tetto.feature.caps

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The numpad (VISION, «The numpad»; PLANNING.md §11 Phase 2), as its sheet holds it. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class NumpadTest {
    @get:Rule val compose = createComposeRule()

    private val confirmed = mutableListOf<Money>()
    private var openedCap = 0

    private fun draw(
        state: CapState = CapsSample.coffee,
        currency: AppCurrency = CapsSample.EUR,
        correcting: Money? = null,
        dark: Boolean = false,
        amounts: List<Money> = emptyList(),
        refund: Boolean = false,
    ) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                Numpad(state, currency, onConfirm = {
                    confirmed += it
                }, onOpenCap = { openedCap++ }, correcting = correcting, amounts = amounts, refund = refund)
            }
        }
    }

    private fun type(keys: String) = keys.forEach { compose.onNodeWithText(it.toString()).performClick() }

    // The preview's own line, inside the readout TalkBack hears as one.
    private fun preview() = compose.onNodeWithTag(CapsTags.PREVIEW, useUnmergedTree = true)

    private fun readout() = compose.onNodeWithTag(CapsTags.READOUT)

    @Test
    fun `Add waits for an amount, and is never relabelled as something else`() {
        draw()
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsNotEnabled().assertTextEquals("Add")
        preview().assertTextEquals("€16 left")
        type("5")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsEnabled().assertTextEquals("Add €5.00")
        compose.assertAccessible()
        compose.writeScreenshot("numpad")
    }

    @Test
    fun `the cap's habits are one touch each, while nothing is typed`() {
        draw(amounts = listOf(euros(1, 20), euros(2, 50)))
        compose.onNodeWithContentDescription("Add €1.20").assertExists()
        compose.assertAccessible()
        compose.writeScreenshot("numpad-amounts")
        compose.onNodeWithContentDescription("Add €2.50").performClick()
        assertThat(confirmed).containsExactly(euros(2, 50))
    }

    @Test
    fun `typing puts the habits away, keeping their room`() {
        draw(amounts = listOf(euros(1, 20)))
        val keysBefore = compose.onNodeWithText("1").fetchSemanticsNode().boundsInRoot
        type("3")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Add €1.20").assertDoesNotExist()
        // The keys stay where the finger found them.
        assertThat(compose.onNodeWithText("1").fetchSemanticsNode().boundsInRoot).isEqualTo(keysBefore)
    }

    @Test
    fun `a refund grows what is left, and gives back at most what was spent`() {
        // Coffee: €8 spent, €16 left.
        draw(refund = true, amounts = listOf(euros(1, 20)))
        compose.onNodeWithText("A refund · €8.00 spent this period").assertExists()
        compose.onNodeWithContentDescription("Add €1.20").assertDoesNotExist()
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsNotEnabled().assertTextEquals("Refund")
        type("3")
        preview().assertTextEquals("€19 left")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsEnabled().assertTextEquals("Refund €3.00")
        compose.assertAccessible()
        compose.writeScreenshot("numpad-refund")
        type("0")
        preview().assertTextEquals("At most €8.00, what this period spent")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsNotEnabled().assertTextEquals("Refund")
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithTag(CapsTags.CONFIRM).performClick()
        assertThat(confirmed).containsExactly(euros(3))
    }

    @Test
    fun `no habits while a spend is corrected`() {
        draw(correcting = euros(3, 20), amounts = listOf(euros(1, 20)))
        compose.onNodeWithContentDescription("Add €1.20").assertDoesNotExist()
    }

    @Test
    fun `the preview follows every key, politely for TalkBack`() {
        draw()
        readout().assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        type("4.5")
        preview().assertTextEquals("€11.50 left")
        // TalkBack hears the amount typed with what it would leave, as one announcement.
        readout().assertTextEquals("€4.5", "€11.50 left")
        type("0")
        type("0")
        // The third decimal is refused: still €4.50.
        preview().assertTextEquals("€11.50 left")
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        compose.onNodeWithContentDescription("Delete").performClick()
        type("20")
        preview().assertTextEquals("€404 over")
        compose.onNodeWithTag(CapsTags.CONFIRM).performClick()
        assertThat(confirmed).containsExactly(euros(420))
    }

    @Test
    fun `the decimal key is named for TalkBack, whatever its punctuation setting`() {
        draw()
        compose.onNodeWithContentDescription("Decimal separator").assertExists()
        compose.onNodeWithContentDescription("Delete").assertExists()
    }

    @Test
    fun `a long press on delete clears`() {
        draw()
        type("123")
        compose.onNodeWithContentDescription("Delete").performTouchInput { longClick() }
        compose.onNodeWithTag(CapsTags.CONFIRM).assertIsNotEnabled()
    }

    @Test
    fun `a cap already over stays over`() {
        draw(CapsSample.shopping)
        type("15")
        preview().assertTextEquals("Still €45 over")
        compose.writeScreenshot("numpad-over")
    }

    @Test
    fun `a currency without decimals has no separator key`() {
        draw(
            CapsSample.state(
                9,
                "Ramen",
                CapIcon.EATING_OUT,
                Money(5_000),
                PeriodKind.WEEK,
                Money(1_000),
                currency = CapsSample.JPY,
            ),
            currency = CapsSample.JPY,
        )
        compose.onNodeWithText(".").assertDoesNotExist()
        type("850")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertTextEquals("Add JP¥850")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian, the separator is a comma`() {
        draw()
        type("2,5")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertTextEquals("Aggiungi 2,50 €")
        preview().assertTextEquals("Ancora 13,50 €")
        compose.writeScreenshot("numpad-it")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `a hardware keyboard types, deletes and adds`() {
        draw()
        compose.onNodeWithTag(CapsTags.NUMPAD).performKeyInput {
            pressKey(Key.Three)
            pressKey(Key.Period)
            pressKey(Key.Two)
            pressKey(Key.Five)
            pressKey(Key.Backspace)
            pressKey(Key.Enter)
        }
        assertThat(confirmed).containsExactly(euros(3, 20))
    }

    @Test
    fun `correcting a spend starts from its amount, and counts it back in what is left`() {
        draw(correcting = euros(3, 20))
        compose.onNodeWithText("Correcting a spend").assertExists()
        // Its decimals in full, as the cap's sheet lists it (owner, 6 Nov 2026).
        readout().assertTextEquals("€3.20", "€16 left")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertTextEquals("Save €3.20")
        repeat(4) { compose.onNodeWithContentDescription("Delete").performClick() }
        preview().assertTextEquals("€19.20 left")
        type("1")
        preview().assertTextEquals("€18.20 left")
    }

    @Test
    fun `the header opens the cap's sheet`() {
        draw()
        compose.onNodeWithText("Coffee").performClick()
        assertThat(openedCap).isEqualTo(1)
    }

    @Test
    fun `nine digits fit, never cut`() {
        draw()
        type("999999999.99")
        compose.writeScreenshot("numpad-nine-digits")
        compose.onNodeWithTag(CapsTags.CONFIRM).assertTextEquals("Add €999,999,999.99")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        type("12")
        compose.walkPage(hasTestTag(CapsTags.NUMPAD), "numpad-large-text")
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        type("5")
        compose.writeScreenshot("numpad-dark")
    }
}
