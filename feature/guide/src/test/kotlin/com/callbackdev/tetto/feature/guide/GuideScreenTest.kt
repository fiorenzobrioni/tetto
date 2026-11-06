package com.callbackdev.tetto.feature.guide

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek

/**
 * The guide (PLANNING.md §11 Phase 6): every chapter, its examples, in the reader's currency and
 * week, both languages, both themes, every reader.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class GuideScreenTest {
    @get:Rule val compose = createComposeRule()

    private val euros = GuideUiState(AppCurrency("EUR", 2), DayOfWeek.MONDAY)

    private fun show(state: GuideUiState? = euros, dark: Boolean = false, onBack: () -> Unit = {}) {
        compose.setContent { TettoTheme(darkTheme = dark) { GuideScreen(state = state, onBack = onBack) } }
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasText(text, substring = true))
    }

    private fun scrollToTag(tag: String) {
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(tag))
    }

    private fun description(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].joinToString()

    @Test
    fun `the guide opens on what Tetto answers, and goes back`() {
        var back = false
        show(onBack = { back = true })

        compose.onNodeWithText("The guide").assertIsDisplayed()
        compose.onNodeWithText("Tetto answers one question", substring = true).assertIsDisplayed()
        compose.onNodeWithText("What the caps screen answers").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("guide")
        compose.onNodeWithContentDescription("Back").performClick()
        assertThat(back).isTrue()
    }

    @Test
    fun `until the settings are read, no example is drawn in the wrong currency`() {
        show(state = null)
        compose.onNodeWithText("The guide").assertIsDisplayed()
        compose.onNodeWithTag(GuideTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun `every chapter is there, each with what it answers`() {
        show()
        // One line each chapter alone has.
        listOf(
            "The bar and today’s line",
            "A period, the reserve and forgiveness",
            "Closed when you next look",
            "The numpad, and fixing a spend",
            "Undo, and later too",
            "On the home screen",
            "The evening reminder",
            "it sends nothing, to anyone",
            "What Tetto does not do",
            "an expense tracker",
            "One currency",
        ).forEachIndexed { index, line ->
            scrollTo(line)
            compose.onNodeWithText(line, substring = true).assertIsDisplayed()
            compose.assertAccessible()
            compose.writeScreenshot("guide-${index + 1}")
        }
    }

    @Test
    fun `each chapter is a heading TalkBack can move by`() {
        show()
        val titles = headings(compose.onNodeWithTag(GuideTags.CONTENT).fetchSemanticsNode())
        assertThat(titles).containsExactly(
            "What the caps screen answers",
            "A period, the reserve and forgiveness",
            "The numpad, and fixing a spend",
            "On the home screen",
            "Your data",
            "What Tetto does not do",
        ).inOrder()
    }

    /** The headings under [node], in reading order. */
    private fun headings(node: SemanticsNode): List<String> {
        val own = if (isHeading().matches(node)) {
            node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        } else {
            emptyList()
        }
        return own + node.children.flatMap(::headings)
    }

    @Test
    fun `the tiles are the caps screen's own, pictures said to be examples`() {
        show()
        scrollToTag(GuideTags.TILES)
        // Each tile is one sentence for TalkBack, as on the caps screen: its words are under it.
        compose.onNode(
            hasContentDescription(
                "Example. Coffee, €16 left of €20, until Sunday, about €4 a day, with €4 in reserve",
            ),
        ).assertIsDisplayed()
        compose.onNode(
            hasContentDescription(
                "Example. Eating out, €9 left of €40, until Sunday, spending ahead of the days",
                substring = true,
            ),
        ).assertIsDisplayed()
        // Pictures: nothing on them to touch.
        compose.onNode(
            hasContentDescription("Example. Coffee", substring = true) and hasClickAction(),
        ).assertDoesNotExist()
        scrollTo("An example, on a Thursday")
        compose.onNodeWithText("An example, on a Thursday", substring = true).assertIsDisplayed()
        compose.writeScreenshot("guide-tiles")
    }

    @Test
    fun `the reserve's story is the engine's, carried, used, forgiven and whole again`() {
        show()
        scrollToTag(GuideTags.STORY)
        val story = compose.onNodeWithTag(GuideTags.STORY)
        story.assertIsDisplayed()
        listOf(
            "Example. Week 1: €14 of €20. +€6 to the reserve",
            "Example. Week 2: €23 of €20. €3 from the reserve",
            "Example. Week 3: €30 of €20. The reserve’s €3 used, €7 forgiven",
            "Example. Week 4: €0 of €20. Starts whole: €20 left",
        ).forEach { compose.onNode(hasContentDescription(it)).assertExists() }
        scrollTo("An example: four weeks of a €20 coffee cap")
        compose.assertAccessible()
        compose.writeScreenshot("guide-story")
    }

    @Test
    fun `the numpad's example says what it typed and what it would leave`() {
        show()
        scrollToTag(GuideTags.NUMPAD)
        assertThat(description(GuideTags.NUMPAD)).isEqualTo(
            "Example of the numpad: €5.00 typed on Coffee, which has €16 left. Under the amount: €11 left",
        )
        scrollTo("An example: €5.00 typed on a cap with €16 left")
        compose.writeScreenshot("guide-numpad")
    }

    @Test
    fun `the examples are in the reader's currency, their days by the reader's week`() {
        show(GuideUiState(AppCurrency("JPY", 0), DayOfWeek.SUNDAY))
        scrollToTag(GuideTags.TILES)
        compose.onNode(hasContentDescription("Example. Coffee, JP¥16 left of JP¥20, until Saturday", substring = true))
            .assertIsDisplayed()
        scrollTo("An example, on a Wednesday")
        scrollToTag(GuideTags.NUMPAD)
        assertThat(description(GuideTags.NUMPAD)).startsWith("Example of the numpad: JP¥5 typed")
        compose.writeScreenshot("guide-yen")
    }

    @Test
    fun `the guide reads in the dark`() {
        show(dark = true)
        scrollToTag(GuideTags.TILES)
        compose.writeScreenshot("guide-dark")
        scrollToTag(GuideTags.STORY)
        compose.writeScreenshot("guide-dark-story")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the guide speaks Italian`() {
        show()
        compose.onNodeWithText("La guida").assertIsDisplayed()
        compose.onNodeWithText("A cosa risponde la schermata dei tetti").assertIsDisplayed()
        scrollToTag(GuideTags.TILES)
        compose.onNode(hasContentDescription("Esempio. Caffè, ancora 16 € su 20 €, fino a domenica", substring = true))
            .assertIsDisplayed()
        scrollTo("Un esempio, di giovedì")
        scrollTo("Un periodo, la riserva e il condono")
        scrollToTag(GuideTags.STORY)
        compose.onNode(
            hasContentDescription(
                "Esempio. Settimana 3: 30 € su 20 €. Usati i 3 € della riserva, condono di 7 €",
            ),
        ).assertExists()
        compose.writeScreenshot("guide-it")
        scrollTo("un’app per tracciare le spese")
        compose.onNodeWithText("Quello che Tetto non fa").assertIsDisplayed()
        compose.onNodeWithText("un’app per tracciare le spese", substring = true).assertIsDisplayed()
        compose.writeScreenshot("guide-it-end")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, the guide still reads`() {
        show()
        compose.walkPage(hasTestTag(GuideTags.CONTENT), "guide-large-text", maxScreens = 60)
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xhdpi")
    fun `on an open foldable the guide is a column in the middle`() {
        show()
        compose.walkPage(hasTestTag(GuideTags.CONTENT), "guide-foldable", maxScreens = 16)
        compose.onNode(hasContentDescription("Back")).assertIsDisplayed()
    }
}
