package com.callbackdev.tetto.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.glance.GlanceCardContent
import com.callbackdev.tetto.widget.words.WordsCardContent
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The amounts hidden (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): no figure anywhere on either
 * card, at any size, TalkBack's descriptions included; the names, the days and the words stay, and
 * over is still said.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class HiddenAmountsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val euro = AppCurrency("EUR", 2)

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.FourByOne,
        Grants.OneByTwo,
        Grants.TwoByTwo,
        Grants.FourByTwo,
        Grants.FourByThree,
    )

    /** Every text a card draws and every description it gives TalkBack. */
    private fun everything(size: DpSize, content: @androidx.compose.runtime.Composable () -> Unit): List<String> =
        runBlocking {
            val views = GlanceRemoteViews().compose(context, size) { content() }.remoteViews
            val host = FrameLayout(context)
            host.addView(views.apply(context, host))
            val found = mutableListOf<String>()
            fun collect(view: View) {
                view.contentDescription?.let { found += it.toString() }
                if (view is android.widget.TextView) found += view.text.toString()
                if (view is android.view.ViewGroup) for (i in 0 until view.childCount) collect(view.getChildAt(i))
            }
            collect(host)
            found
        }

    /** No amount: no currency sign, and no digit but a date's ("until 30 Nov", which stays). */
    private fun assertNoFigure(texts: List<String>, what: String) {
        texts.forEach { text ->
            val withoutDates = text.replace(DATE, "")
            assertWithMessage("$what: «$text»").that(withoutDates.none { it.isDigit() || it == '€' }).isTrue()
        }
    }

    private companion object {
        val DATE = Regex("\\d{1,2}[\\s\\u00A0][A-Z][a-z]{2}")
    }

    @Test
    fun `at a glance shows no figure, and still says over`() {
        val model = WidgetSamples.model(context, currency = euro, look = WidgetLook(hideAmounts = true))
        sizes.forEach { size ->
            val texts = everything(size) { GlanceCardContent(model) }
            // The count of caps that do not fit ("3 more") is not an amount.
            assertNoFigure(texts.filterNot { it.contains("more") || it.startsWith("+") }, "At a glance ${size.width}")
        }
        val grid = everything(Grants.FourByThree) { GlanceCardContent(model) }
        assertThat(grid.joinToString()).contains("Over")
        assertThat(grid.joinToString()).contains("Coffee")
    }

    @Test
    fun `in words shows no figure, at ease, ahead and over`() {
        listOf(1L, 2L, 5L).forEach { cap ->
            listOf(true, false).forEach { sentence ->
                val look = WidgetLook(wordsCap = cap, showSentence = sentence, hideAmounts = true)
                val model = WidgetSamples.model(context, currency = euro, look = look)
                sizes.forEach { size ->
                    assertNoFigure(everything(size) { WordsCardContent(model) }, "In words, cap $cap, ${size.width}")
                }
            }
        }
        val over = WidgetSamples.model(context, currency = euro, look = WidgetLook(wordsCap = 5L, hideAmounts = true))
        assertThat(everything(Grants.TwoByTwo) { WordsCardContent(over) }.joinToString()).contains("Over its cap")
        val ahead = WidgetSamples.model(context, currency = euro, look = WidgetLook(wordsCap = 2L, hideAmounts = true))
        assertThat(
            everything(Grants.FourByOne) {
                WordsCardContent(ahead)
            }.joinToString(),
        ).contains("Ahead of the days")
    }

    @Test
    fun `shown, the same cards say their figures`() {
        val model = WidgetSamples.model(context, currency = euro, look = WidgetLook(wordsCap = 1L))
        assertThat(everything(Grants.TwoByTwo) { WordsCardContent(model) }.joinToString()).contains("€12 left")
    }
}
