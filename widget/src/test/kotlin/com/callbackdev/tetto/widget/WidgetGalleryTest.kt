package com.callbackdev.tetto.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.core.model.WordsBelow
import com.callbackdev.tetto.widget.glance.GlanceCardContent
import com.callbackdev.tetto.widget.words.WordsCardContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every form of both cards, drawn to `build/screenshots` to be looked at after a change to a card
 * (CLAUDE.md). Not an assertion: the arithmetic is pinned by `GlanceFitTest`, and what is cut by
 * `WidgetFitTest`.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xhdpi")
class WidgetGalleryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/screenshots")
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
        Grants.FourByThree,
    )

    private fun sample(words: Boolean = false, look: WidgetLook = WidgetLook()) =
        WidgetSamples.model(context, words = words, currency = euro, look = look)

    @Test
    fun `at a glance, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size -> board.row(size to renderCard(context, size) { GlanceCardContent(sample()) }) }
        board.draw().saveTo(out, "widget-glance-sizes")
    }

    @Test
    fun `in words, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(size to renderCard(context, size) { WordsCardContent(sample(words = true)) })
        }
        board.draw().saveTo(out, "widget-words-sizes")
    }

    @Test
    fun `in words, without the sentence`() {
        val look = WidgetLook(showSentence = false)
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.TwoByTwo, Grants.OwnersTwoByTwo, Grants.FourByTwo, Grants.FourByThree).forEach { size ->
            board.row(
                size to renderCard(context, size) { WordsCardContent(sample(words = true)) },
            )
            board.row(size to renderCard(context, size) { WordsCardContent(sample(words = true, look = look)) })
        }
        board.draw().saveTo(out, "widget-words-no-sentence")
    }

    /**
     * A wide card's strip (owner, 14 Nov 2026): the cap's period day by day, a month's days, and
     * the other caps the reader picked, as many as fit and then counted; at the largest text the
     * caps leave the four-by-two tall again.
     */
    @Test
    fun `in words, a wide card's days and other caps`() {
        val board = HomeBoard(context, widthDp = 380)
        val week = WidgetLook(wordsCap = 1L)
        val month = WidgetLook(wordsCap = 2L)
        val caps = WidgetLook(wordsCap = 1L, wordsBelow = WordsBelow.CAPS, wordsBelowCaps = setOf(2L, 3L, 5L))
        val many = WidgetLook(wordsCap = 1L, wordsBelow = WordsBelow.CAPS, wordsBelowCaps = setOf(2L, 3L, 4L, 5L))
        listOf(week, month, caps, many).forEach { look ->
            board.row(
                Grants.FourByTwo to renderCard(context, Grants.FourByTwo) {
                    WordsCardContent(sample(look = look))
                },
            )
        }
        board.row(Grants.ThreeByTwo to renderCard(context, Grants.ThreeByTwo) { WordsCardContent(sample(look = caps)) })
        board.row(
            Grants.FourByThree to renderCard(context, Grants.FourByThree) {
                WordsCardContent(sample(look = week))
            },
        )
        board.draw().saveTo(out, "widget-words-panel")
    }

    /** The three states a cap's ring and bar say: at ease, ahead of the days, over (in words too). */
    @Test
    fun `in words, at ease, ahead and over`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(1L, 2L, 5L).forEach { cap ->
            val look = WidgetLook(wordsCap = cap)
            board.row(
                Grants.OneByOne to renderCard(context, Grants.OneByOne) { WordsCardContent(sample(look = look)) },
                Grants.TwoByOne to renderCard(context, Grants.TwoByOne) { WordsCardContent(sample(look = look)) },
            )
            board.row(
                Grants.FourByOne to renderCard(context, Grants.FourByOne) {
                    WordsCardContent(sample(look = look))
                },
            )
            board.row(Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsCardContent(sample(look = look)) })
        }
        board.draw().saveTo(out, "widget-words-states")
    }

    /** The amounts hidden (owner, 6 Nov 2026): every form of both cards, at ease, ahead and over. */
    @Test
    fun `with the amounts hidden`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(
                size to renderCard(context, size) { GlanceCardContent(sample(look = WidgetLook(hideAmounts = true))) },
            )
        }
        listOf(1L, 2L, 5L).forEach { cap ->
            val look = WidgetLook(wordsCap = cap, hideAmounts = true)
            board.row(
                Grants.OneByOne to renderCard(context, Grants.OneByOne) { WordsCardContent(sample(look = look)) },
                Grants.TwoByOne to renderCard(context, Grants.TwoByOne) { WordsCardContent(sample(look = look)) },
            )
            board.row(
                Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsCardContent(sample(look = look)) },
            )
            board.row(
                Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsCardContent(sample(look = look)) },
                Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsCardContent(sample()) },
            )
        }
        board.draw().saveTo(out, "widget-hidden-amounts")
    }

    /** A one-off chosen for «In words» (owner, 6 Nov 2026): running, and the day after it ended. */
    @Test
    fun `in words, a one-off running and ended`() {
        val board = HomeBoard(context, widthDp = 380)
        val running = sample().copy(caps = listOf(WidgetSamplesForTests.lisbon(running = true)))
        val ended = sample(
            look = WidgetLook(wordsCap = 9L),
        ).copy(caps = listOf(WidgetSamplesForTests.lisbon(running = false)))
        listOf(running, ended).forEach { model ->
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsCardContent(model) })
            board.row(
                Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsCardContent(model) },
                Grants.OneByOne to renderCard(context, Grants.OneByOne) { WordsCardContent(model) },
            )
        }
        board.draw().saveTo(out, "widget-words-one-off")
    }

    @Test
    fun `the grounds and the colours`() {
        val looks = listOf(
            WidgetLook(ground = WidgetGround.LIGHT),
            WidgetLook(ground = WidgetGround.DARK),
            WidgetLook(ground = WidgetGround.SYSTEM),
            WidgetLook(ground = WidgetGround.COLOR, opacityPct = 40),
        ) + WidgetCardColor.entries.map { WidgetLook(cardColor = it) }
        listOf(true, false).forEach { dark ->
            val board = HomeBoard(context, widthDp = 380)
            looks.forEach { look ->
                board.row(
                    Grants.TwoByTwo to
                        renderCard(context, Grants.TwoByTwo) { WordsCardContent(sample(words = true, look = look)) },
                    Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { GlanceCardContent(sample(look = look)) },
                )
            }
            board.draw(
                dark,
            ).saveTo(out, if (dark) "widget-grounds-dark-wallpaper" else "widget-grounds-light-wallpaper")
        }
    }

    @Test
    fun `nothing to show`() {
        val empty = sample().copy(caps = emptyList())
        val gone = sample(look = WidgetLook(wordsCap = 99L))
        val board = HomeBoard(context, widthDp = 380)
        board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { GlanceCardContent(empty) })
        board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsCardContent(gone) })
        board.row(
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) {
                WordsCardContent(WidgetModel.unavailable())
            },
        )
        board.draw().saveTo(out, "widget-messages")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in italian`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.OneByOne, Grants.FourByOne, Grants.TwoByTwo, Grants.FourByTwo).forEach { size ->
            board.row(size to renderCard(context, size) { WordsCardContent(sample(words = true)) })
            board.row(size to renderCard(context, size) { GlanceCardContent(sample()) })
        }
        board.draw().saveTo(out, "widget-italian")
    }

    @Test
    fun `at the largest text`() {
        RuntimeEnvironment.setFontScale(1.3f)
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.TwoByOne, Grants.FourByOne, Grants.TwoByTwo, Grants.FourByTwo).forEach { size ->
            board.row(size to renderCard(context, size) { WordsCardContent(sample(words = true)) })
            board.row(size to renderCard(context, size) { GlanceCardContent(sample()) })
        }
        board.draw().saveTo(out, "widget-large-text")
    }
}
