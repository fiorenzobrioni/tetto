package com.callbackdev.tetto.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.glance.GlanceCardContent
import com.callbackdev.tetto.widget.words.WordsCardContent
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's picture of the cards (docs/screenshots), drawn by Glance as a launcher draws them,
 * on a stand-in wallpaper, from [WidgetSamples]' caps in euros. Run with
 * `./gradlew :widget:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val output = System.getProperty("tetto.readmeScreenshots")
    private val euro = AppCurrency("EUR", 2)

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    /**
     * The pair on a home screen, «At a glance» first as in the launcher's list: a one-row «At a
     * glance» in the app's violet; «At a glance» two by two in ochre on the cap at ease and in
     * terracotta on the one over, side by side; «List» at four by two in the violet.
     */
    @Test
    fun widgets() {
        val panel = DpSize(361.dp, 189.dp)
        val half = DpSize(172.dp, 189.dp)
        val row = DpSize(361.dp, 85.dp)
        fun words(cap: Long, color: WidgetCardColor) = WidgetSamples.model(
            context,
            words = true,
            currency = euro,
            look = WidgetLook(cardColor = color, wordsCap = cap),
        )
        val board = HomeBoard(context, widthDp = 393)
            .row(row to renderCard(context, row) { WordsCardContent(words(2, WidgetCardColor.PLUM)) })
            .row(
                half to renderCard(context, half) { WordsCardContent(words(1, WidgetCardColor.OCHRE)) },
                half to renderCard(context, half) { WordsCardContent(words(5, WidgetCardColor.CLAY)) },
            )
            .row(
                panel to renderCard(context, panel) {
                    GlanceCardContent(WidgetSamples.model(context, currency = euro))
                },
            )
        board.draw().saveTo(File(checkNotNull(output)), "widgets")
    }
}
