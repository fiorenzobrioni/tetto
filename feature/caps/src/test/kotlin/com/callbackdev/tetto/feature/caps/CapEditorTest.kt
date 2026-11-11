package com.callbackdev.tetto.feature.caps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
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

/** The cap editor (VISION, «New and edited caps»): four fields, the amount typed exactly. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class CapEditorTest {
    @get:Rule val compose = createComposeRule()

    private var draft by mutableStateOf(CapDraft("", Money.ZERO, PeriodKind.MONTH, CapIcon.OTHER))
    private var saved = 0

    private fun draw(editing: PeriodKind? = null, start: CapDraft = draft) {
        draft = start
        compose.setContent {
            TettoTheme {
                CapEditorScreen(
                    editing = editing,
                    draft = draft,
                    currency = CapsSample.EUR,
                    onChange = { draft = it },
                    onSave = { saved++ },
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun `a new cap is saved once it has a name and an amount`() {
        draw()
        compose.onNodeWithTag(CapsTags.EDITOR_SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(CapsTags.EDITOR_NAME).performTextInput("Coffee")
        compose.onNodeWithTag(CapsTags.EDITOR_SAVE).assertIsNotEnabled()
        compose.onNodeWithTag(CapsTags.EDITOR_AMOUNT).performTextInput("20.5")
        compose.onNodeWithText("Week").performClick()
        compose.onNodeWithContentDescription("Coffee and bars").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Coffee and bars").assertIsSelected()
        compose.onNodeWithTag(CapsTags.EDITOR_SAVE).assertIsEnabled().performClick()
        assertThat(draft).isEqualTo(CapDraft("Coffee", euros(20, 50), PeriodKind.WEEK, CapIcon.COFFEE))
        assertThat(saved).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("editor-new")
    }

    @Test
    fun `the amount takes what the numpad would, and refuses the rest`() {
        draw()
        val amount = compose.onNodeWithTag(CapsTags.EDITOR_AMOUNT)
        amount.performTextReplacement("12.345")
        assertThat(draft.amount).isEqualTo(Money.ZERO)
        amount.performTextReplacement("1a")
        assertThat(draft.amount).isEqualTo(Money.ZERO)
        amount.performTextReplacement("12.34")
        assertThat(draft.amount).isEqualTo(euros(12, 34))
        amount.performTextReplacement("1234567890")
        assertThat(draft.amount).isEqualTo(euros(12, 34))
    }

    @Test
    fun `a cap's amount opens with its decimals in full`() {
        draw(editing = PeriodKind.MONTH, start = CapDraft("Books", euros(12, 50), PeriodKind.MONTH, CapIcon.BOOKS))
        compose.onNodeWithTag(CapsTags.EDITOR_AMOUNT).assertTextContains("12.50")
    }

    @Test
    fun `a change of period says what it does to this period`() {
        draw(editing = PeriodKind.WEEK, start = CapDraft("Coffee", euros(20), PeriodKind.WEEK, CapIcon.COFFEE))
        compose.onNodeWithText("This period’s spends move into the new period; the reserve stays.").assertDoesNotExist()
        compose.onNodeWithText("Month").performClick()
        compose.onNodeWithText("This period’s spends move into the new period; the reserve stays.").assertExists()
        compose.writeScreenshot("editor-edit")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian, the comma and the euro after`() {
        draw()
        compose.onNodeWithTag(CapsTags.EDITOR_AMOUNT).performTextInput("7,5")
        assertThat(draft.amount).isEqualTo(euros(7, 50))
        compose.onNodeWithText("Nuovo tetto").assertExists()
        compose.writeScreenshot("editor-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw(start = CapDraft("Groceries", euros(300), PeriodKind.MONTH, CapIcon.GROCERIES))
        compose.walkPage(hasTestTag(CapsTags.EDITOR), "editor-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw(start = CapDraft("Groceries", euros(300), PeriodKind.MONTH, CapIcon.GROCERIES))
        compose.walkPage(hasTestTag(CapsTags.EDITOR), "editor-foldable")
    }
}
