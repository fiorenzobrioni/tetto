package com.callbackdev.tetto.widget

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.domain.widget.GlanceFit
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.glance.GlanceCardContent
import com.callbackdev.tetto.widget.words.WordsCardContent
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Nothing a card says is cut (PLANNING.md §7): every form at the family's reference grants, at
 * 100, 115 and 130% text, every line set 5% wider than this process measures it, as a launcher on
 * another face draws it. A cap's name may end in an ellipsis (it is the reader's, and as long as
 * they made it); a figure, a sentence, a word never. And «In words»'s ring is the family's circle
 * on the card as drawn, not only in the arithmetic.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetFitTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val euro = AppCurrency("EUR", 2)

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.OneByTwo,
        Grants.TwoByTwo,
        Grants.OwnersTwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.TwoByThree,
        Grants.ThreeByThree,
        Grants.FourByThree,
    )

    private fun names(model: WidgetModel) = model.caps.map { it.cap.name }

    private fun check(scale: Float) {
        RuntimeEnvironment.setFontScale(scale)
        val cut = mutableListOf<String>()
        sizes.forEach { size ->
            // With the amounts and without them (owner, 6 Nov 2026): the words that stand in for a
            // figure are measured as the figures are, and nothing of them is cut either.
            listOf(false, true).forEach { hidden ->
                listOf(1L, 2L, 5L).forEach { cap ->
                    listOf(true, false).forEach { sentence ->
                        val model = WidgetSamples.model(
                            context,
                            currency = euro,
                            look = WidgetLook(wordsCap = cap, showSentence = sentence, hideAmounts = hidden),
                        )
                        val mayEllipsize = { text: String -> names(model).any { it in text } }
                        cutTexts(context, size, STRETCH, mayEllipsize) { WordsCardContent(model) }
                            .forEach {
                                cut += "In words, cap $cap, sentence $sentence, hidden $hidden, " +
                                    "${size.width}×${size.height} at $scale: $it"
                            }
                    }
                }
                val model = WidgetSamples.model(context, currency = euro, look = WidgetLook(hideAmounts = hidden))
                val mayEllipsize = { text: String -> names(model).any { it in text } }
                cutTexts(context, size, STRETCH, mayEllipsize) { GlanceCardContent(model) }
                    .forEach { cut += "At a glance, hidden $hidden, ${size.width}×${size.height} at $scale: $it" }
            }
        }
        assertThat(cut).isEmpty()
    }

    @Test
    fun `nothing is cut at the default text size`() = check(1f)

    @Test
    fun `nothing is cut at 115 percent`() = check(1.15f)

    @Test
    fun `nothing is cut at 130 percent`() = check(1.3f)

    @Test
    fun `in words draws the family's circle at every reference grant`() {
        val expected = mapOf(
            Grants.OneByOne to 69f,
            Grants.OneByTwo to 69f,
            Grants.TwoByOne to 44f,
            Grants.ThreeByOne to 56f,
            Grants.FourByOne to 56f,
            Grants.TwoByTwo to 52.75f,
            Grants.ThreeByTwo to 73.75f,
            Grants.FourByTwo to 73.75f,
            Grants.TwoByThree to 104f,
            Grants.ThreeByThree to 104f,
            Grants.FourByThree to 119.25f,
            Grants.OwnersTwoByTwo to 81.75f,
        )
        expected.forEach { (size, circle) ->
            val model = WidgetSamples.model(context, words = true, currency = euro)
            assertWithMessage("${size.width}×${size.height}").that(
                abs(
                    ringSide(size) {
                        WordsCardContent(model)
                    } - circle,
                ),
            )
                .isAtMost(0.5f)
        }
    }

    @Test
    fun `without the sentence, the tall circle takes the band`() {
        val model = WidgetSamples.model(context, words = true, currency = euro, look = WidgetLook(showSentence = false))
        assertThat(ringSide(Grants.TwoByTwo) { WordsCardContent(model) }).isWithin(0.5f).of(95f)
        assertThat(ringSide(Grants.OwnersTwoByTwo) { WordsCardContent(model) }).isWithin(0.5f).of(104f)
        assertThat(ringSide(Grants.FourByTwo) { WordsCardContent(model) }).isWithin(0.5f).of(95f)
    }

    @Test
    fun `the circle is the card's, whichever cap it shows`() {
        val sides = listOf(1L, 2L, 5L).map { cap ->
            val model = WidgetSamples.model(context, currency = euro, look = WidgetLook(wordsCap = cap))
            ringSide(Grants.TwoByTwo) { WordsCardContent(model) }
        }
        assertThat(sides.toSet()).hasSize(1)
        assertThat(sides.first()).isWithin(0.5f).of(GlanceFit.tallCircle(159f, 189f, 1f))
    }

    /**
     * The ring's side as the launcher lays it out, in dp: the card's largest square image, the
     * card's own ground (an image the size of the card, for its corner) left out.
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun ringSide(size: DpSize, content: @Composable () -> Unit): Float = runBlocking {
        val density = context.resources.displayMetrics.density
        val views = GlanceRemoteViews().compose(context, size) { content() }.remoteViews
        val host = FrameLayout(context)
        host.addView(views.apply(context, host))
        val w = (size.width.value * density).toInt()
        val h = (size.height.value * density).toInt()
        host.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, w, h)
        val images = mutableListOf<ImageView>()
        fun collect(view: View) {
            if (view is ImageView && view.visibility == View.VISIBLE) images += view
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) collect(view.getChildAt(i))
        }
        collect(host)
        images.filter { abs(it.width - it.height) <= 1 && it.width < w - 1 && it.height < h - 1 }.maxOf { it.width } /
            density
    }

    private companion object {
        const val STRETCH = 1.05f
    }
}
