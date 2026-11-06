package com.callbackdev.tetto.feature.caps

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** The numpad over the home screen, as its sheet holds it: typing, the line after Add, a cap gone. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class QuickAddSheetTest {
    @get:Rule val compose = createComposeRule()

    private val ui = QuickAddUiState(CapsSample.today, CapsSample.EUR, CapsSample.coffee)
    private val spend = Spend(7, 1, euros(5), Instant.parse("2026-11-05T09:00:00Z"), CapsSample.coffee.period.start)
    private var undone = 0
    private var closed = 0
    private var opened = 0

    private fun draw(state: QuickAddUiState, phase: QuickAddPhase) {
        compose.setContent {
            TettoTheme {
                QuickAddContent(
                    state = state,
                    phase = phase,
                    onAdd = {},
                    onUndo = { undone++ },
                    onClose = { closed++ },
                    onOpenApp = { opened++ },
                )
            }
        }
    }

    @Test
    fun `the numpad on the cap, its amount typed in again after Undo`() {
        draw(ui, QuickAddPhase.Typing(initial = euros(5), round = 1))
        compose.onNodeWithTag(CapsTags.NUMPAD).assertExists()
        compose.onNodeWithText("Add €5.00").assertExists()
        compose.assertAccessible()
        compose.writeScreenshot("quick-add-typing")
    }

    @Test
    fun `after Add, one line with what is left and Undo`() {
        draw(ui, QuickAddPhase.Added(spend))
        compose.onNodeWithText("€5.00 added to Coffee").assertExists()
        compose.onNodeWithText("€16 left until Sunday").assertExists()
        compose.onNodeWithTag(CapsTags.QUICK_UNDO).performClick()
        assertThat(undone).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("quick-add-added")
    }

    @Test
    fun `the line closes the sheet by itself`() {
        compose.mainClock.autoAdvance = false
        draw(ui, QuickAddPhase.Added(spend))
        compose.mainClock.advanceTimeBy(10_000)
        assertThat(closed).isEqualTo(1)
    }

    @Test
    fun `a cap gone says so, and opens Tetto`() {
        draw(ui.copy(cap = null), QuickAddPhase.Typing())
        compose.onNodeWithText("This cap is no longer here").assertExists()
        compose.onNodeWithText("Open Tetto").performClick()
        assertThat(opened).isEqualTo(1)
        compose.assertAccessible()
    }
}
