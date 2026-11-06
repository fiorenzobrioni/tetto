package com.callbackdev.tetto.feature.caps

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A cap's sheet (VISION, «A cap's sheet»), from its state, its spends and its lines. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class CapSheetTest {
    @get:Rule val compose = createComposeRule()

    private val corrected = mutableListOf<Spend>()
    private val deleted = mutableListOf<Spend>()
    private var cleared = 0
    private var edited = 0
    private var deletedCap = 0

    private fun draw(
        state: CapState = CapsSample.coffee,
        spends: List<Spend> = CapsSample.coffeeSpends,
        dark: Boolean = false,
    ) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                CapSheetContent(
                    state = state,
                    data = CapSheetData(
                        state.cap.id,
                        spends,
                        if (state ==
                            CapsSample.coffee
                        ) {
                            CapsSample.coffeeLines
                        } else {
                            emptyList()
                        },
                    ),
                    format = rememberMoneyFormat(CapsSample.EUR),
                    today = CapsSample.today,
                    actions = CapSheetActions(
                        onCorrect = { corrected += it },
                        onDelete = { deleted += it },
                        onClearReserve = { cleared++ },
                        onEdit = { edited++ },
                        onDeleteCap = { deletedCap++ },
                    ),
                )
            }
        }
    }

    @Test
    fun `this period, its spends, the last periods, and what can be done`() {
        draw()
        compose.onNodeWithText("Coffee").assertExists()
        // The receipt, read as one sentence; its figures share their cents with the spends'.
        compose.onNodeWithTag(CapsTags.RECEIPT)
            .assertContentDescriptionEquals("€20.00 cap, plus €4.00 in reserve, less €8.00 spent: €16.00 left")
        compose.onNodeWithText("€3.20").assertExists()
        // The tile's pace, which the list's rows leave out.
        compose.onNodeWithText("To stay under, about €4 a day").assertExists()
        compose.onNodeWithText("The last periods").performScrollTo()
        compose.onNodeWithText("Clear the reserve").performScrollTo().performClick()
        compose.onNodeWithText("Edit").performScrollTo().performClick()
        compose.onNodeWithText("Delete the cap").performScrollTo().performClick()
        assertThat(listOf(cleared, edited, deletedCap)).containsExactly(1, 1, 1).inOrder()
        compose.assertAccessible()
        compose.writeScreenshot("cap-sheet")
    }

    @Test
    fun `a touch corrects a spend, a swipe or TalkBack's action deletes it`() {
        draw()
        compose.onNodeWithTag(CapsTags.spend(3)).performClick()
        assertThat(corrected.map { it.id }).containsExactly(3L)
        compose.onNodeWithTag(CapsTags.spend(2)).performScrollTo().performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertThat(deleted.map { it.id }).containsExactly(2L)
        val actions = compose.onNodeWithTag(
            CapsTags.spend(1),
        ).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        actions.single { it.label == "Delete" }.action()
        assertThat(deleted.map { it.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun `with nothing spent and no reserve, it says so and offers no clearing`() {
        draw(state = CapsSample.groceries, spends = emptyList())
        compose.onNodeWithText("Nothing spent in this period yet.").assertExists()
        compose.onNodeWithText("Clear the reserve").assertDoesNotExist()
        compose.onNodeWithText("The last periods").assertDoesNotExist()
        compose.onNodeWithText("To stay under, about", substring = true).assertExists()
        // One figure with cents gives every figure its cents; no reserve, none said.
        compose.onNodeWithTag(
            CapsTags.RECEIPT,
        ).assertContentDescriptionEquals("€300.00 cap, less €96.40 spent: €203.60 left")
    }

    @Test
    fun `over, it says how far`() {
        draw(state = CapsSample.shopping, spends = emptyList())
        compose.onNodeWithTag(CapsTags.RECEIPT).assertContentDescriptionContains("€30 over", substring = true)
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(CapsTags.SHEET), "cap-sheet-large-text")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw(dark = true)
        compose.onNodeWithText("Gli ultimi periodi").performScrollTo()
        compose.writeScreenshot("cap-sheet-it-dark")
    }
}
