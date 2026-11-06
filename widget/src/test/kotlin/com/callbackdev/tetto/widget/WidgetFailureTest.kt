package com.callbackdev.tetto.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.widget.words.WordsWidgetReceiver
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * A card whose drawing threw (PLANNING.md §7, Rendering): Tetto's words in place of Glance's blank
 * box, the whole card a way into the app.
 */
@RunWith(AndroidJUnit4::class)
class WidgetFailureTest {
    private val context: Application = ApplicationProvider.getApplicationContext()
    private val manager = AppWidgetManager.getInstance(context)

    private fun texts(view: View): List<String> = when (view) {
        is TextView -> listOf(view.text.toString())
        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
        else -> emptyList()
    }

    private fun placeACard(id: Int) {
        val info = AppWidgetProviderInfo().apply { provider = ComponentName(context, WordsWidgetReceiver::class.java) }
        shadowOf(manager).addBoundWidget(id, info)
    }

    @Test
    fun `says it could not be drawn, and what a touch does`() {
        placeACard(42)
        WidgetFailure.show(context, 42, IllegalStateException("a form's arithmetic"))
        val view = shadowOf(manager).getViewFor(42)
        assertThat(texts(view)).containsExactly("This card could not be drawn", "Touch to open Tetto").inOrder()
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian`() {
        placeACard(43)
        WidgetFailure.show(context, 43, IllegalStateException("a form's arithmetic"))
        assertThat(texts(shadowOf(manager).getViewFor(43)))
            .containsExactly("Impossibile disegnare la scheda", "Tocca per aprire Tetto").inOrder()
    }
}
