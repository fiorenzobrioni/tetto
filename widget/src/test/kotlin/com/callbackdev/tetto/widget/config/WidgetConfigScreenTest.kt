package com.callbackdev.tetto.widget.config

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.callbackdev.tetto.widget.WidgetKind
import com.callbackdev.tetto.widget.WidgetModel
import com.callbackdev.tetto.widget.WidgetSamples
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** One card's settings: the live preview on top, the ground, the caps, the sentence (PLANNING.md §7). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetConfigScreenTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var look by mutableStateOf(WidgetLook())
    private var done = false

    private fun show(
        kind: WidgetKind,
        model: WidgetModel = WidgetSamples.model(context, currency = AppCurrency("EUR", 2)),
    ) {
        compose.setContent {
            TettoTheme {
                WidgetConfigScreen(
                    kind = kind,
                    look = look,
                    model = model,
                    preview = model,
                    placed = null,
                    onLook = { look = it },
                    onOpacityDrag = { look = look.copy(opacityPct = it) },
                    onOpacityDone = {},
                    onDone = { done = true },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(WidgetConfigTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `the preview first, then the ground, each with its swatch`() {
        show(WidgetKind.GLANCE)
        compose.onNodeWithContentDescription("Card preview").assertExists()
        compose.onNodeWithText("Background").assertExists()
        compose.onNodeWithText("Blue").assertExists()
        compose.assertAccessible()
        compose.writeScreenshot("widget-config-glance")
    }

    @Test
    fun `a ground and a colour are chosen`() {
        show(WidgetKind.GLANCE)
        compose.onNodeWithText("Dark").performClick()
        assertThat(look.ground).isEqualTo(WidgetGround.DARK)
        compose.onNodeWithText("A colour").performClick()
        compose.onNodeWithContentDescription("Terracotta").performClick()
        assertThat(look.ground).isEqualTo(WidgetGround.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.CLAY)
    }

    @Test
    fun `at a glance shows every cap until the reader ticks some, starting from all`() {
        show(WidgetKind.GLANCE)
        scrollTo(WidgetConfigTags.ALL_CAPS)
        compose.onNodeWithTag(WidgetConfigTags.ALL_CAPS).performClick()
        assertThat(look.glanceCaps).containsExactly(1L, 2L, 3L, 4L, 5L)
        scrollTo(WidgetConfigTags.cap(3))
        compose.onNodeWithTag(WidgetConfigTags.cap(3)).performClick()
        assertThat(look.glanceCaps).containsExactly(1L, 2L, 4L, 5L)
        compose.writeScreenshot("widget-config-glance-chosen")
    }

    @Test
    fun `the last cap ticked cannot be unticked`() {
        look = WidgetLook(glanceCaps = setOf(2L))
        show(WidgetKind.GLANCE)
        scrollTo(WidgetConfigTags.cap(2))
        compose.onNodeWithTag(WidgetConfigTags.cap(2)).performClick()
        assertThat(look.glanceCaps).containsExactly(2L)
    }

    @Test
    fun `in words, the closest or one cap, and the sentence`() {
        show(WidgetKind.WORDS)
        scrollTo(WidgetConfigTags.cap(5))
        compose.onNodeWithTag(WidgetConfigTags.cap(5)).performClick()
        assertThat(look.wordsCap).isEqualTo(5L)
        scrollTo(WidgetConfigTags.CLOSEST)
        compose.onNodeWithTag(WidgetConfigTags.CLOSEST).performClick()
        assertThat(look.wordsCap).isNull()
        scrollTo(WidgetConfigTags.SENTENCE)
        compose.onNodeWithTag(WidgetConfigTags.SENTENCE).performClick()
        assertThat(look.showSentence).isFalse()
        scrollTo(WidgetConfigTags.DONE)
        compose.onNodeWithTag(WidgetConfigTags.DONE).performClick()
        assertThat(done).isTrue()
        compose.writeScreenshot("widget-config-words")
    }

    @Test
    fun `either card can hide its amounts, the real card above showing what stays`() {
        show(WidgetKind.GLANCE)
        scrollTo(WidgetConfigTags.HIDE_AMOUNTS)
        compose.onNodeWithText("Hide the amounts").assertExists()
        compose.onNodeWithTag(WidgetConfigTags.HIDE_AMOUNTS).performClick()
        assertThat(look.hideAmounts).isTrue()
        compose.assertAccessible()
        compose.writeScreenshot("widget-config-hidden")
    }

    @Test
    fun `with no caps yet, it says the card will show them`() {
        show(WidgetKind.WORDS, WidgetSamples.model(context).copy(caps = emptyList()))
        scrollTo(WidgetConfigTags.CLOSEST)
        compose.onNodeWithText("No caps yet", substring = true).assertExists()
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, nothing is cut`() {
        show(WidgetKind.WORDS)
        compose.walkPage(hasTestTag(WidgetConfigTags.LIST), "widget-config-large-text", maxScreens = 30)
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable, a centred column`() {
        show(WidgetKind.GLANCE)
        compose.walkPage(hasTestTag(WidgetConfigTags.LIST), "widget-config-foldable")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in Italian`() {
        show(WidgetKind.WORDS)
        compose.onNodeWithText("Questa scheda").assertExists()
        compose.onNodeWithText("Sfondo").assertExists()
        compose.writeScreenshot("widget-config-it")
    }
}
