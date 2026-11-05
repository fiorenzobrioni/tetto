package com.callbackdev.tetto.feature.caps

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The order (PLANNING.md §11 Phase 2): dragged by the handles, a place at a time with TalkBack. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class ArrangeCapsTest {
    @get:Rule val compose = createComposeRule()

    private val orders = mutableListOf<List<Long>>()
    private var done = 0

    private fun draw() {
        val items = CapsSample.underCaps.map { ArrangeItem(it.cap.id, it.cap.name, it.cap.icon.symbol) }
        compose.setContent {
            TettoTheme { ArrangeCapsScreen(items, onOrder = { orders += it }, onDone = { done++ }) }
        }
    }

    private fun action(name: String, label: String) {
        val node = compose.onNode(hasText(name)).fetchSemanticsNode()
        node.config[SemanticsActions.CustomActions].single { it.label == label }.action()
        compose.waitForIdle()
    }

    @Test
    fun `TalkBack moves a cap a place at a time, and each move is kept`() {
        draw()
        action("Groceries", "Move up")
        assertThat(orders.last()).containsExactly(2L, 1L, 3L, 5L).inOrder()
        action("Groceries", "Move down")
        action("Restaurants", "Move down")
        assertThat(orders.last()).containsExactly(1L, 2L, 5L, 3L).inOrder()
        compose.onNodeWithText("Done").performClick()
        assertThat(done).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("arrange")
    }

    @Test
    fun `the first cannot move up, nor the last down`() {
        draw()
        val first = compose.onNode(hasText("Coffee")).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertThat(first.map { it.label }).containsExactly("Move down")
        val last = compose.onNode(hasText("Lunch")).fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertThat(last.map { it.label }).containsExactly("Move up")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(CapsTags.ARRANGE_LIST), "arrange-large-text")
    }
}
