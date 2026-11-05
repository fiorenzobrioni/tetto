package com.callbackdev.tetto.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * What no other test reaches, because the launcher reads it: the two providers (PLANNING.md §7:
 * `updatePeriodMillis` 0, resizable both ways from one cell, no maximum, the settings optional) and
 * their static previews, which may only use views `RemoteViews` can inflate.
 */
class WidgetProviderTest {
    private val xml = File("src/main/res/xml")
    private val layout = File("src/main/res/layout")

    private val remoteViewsTags = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
    )

    private fun provider(name: String) = attributes(File(xml, name).readText())

    private fun attributes(text: String): Map<String, String> =
        Regex("""android:(\w+)="([^"]*)"""").findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `both cards are pushed, resizable from one cell, with no maximum, the settings optional`() {
        listOf("widget_glance_info.xml", "widget_words_info.xml").map(::provider).forEach { info ->
            assertThat(info["updatePeriodMillis"]).isEqualTo("0")
            assertThat(info["minResizeWidth"]).isEqualTo("40dp")
            assertThat(info["minResizeHeight"]).isEqualTo("40dp")
            assertThat(info["resizeMode"]).isEqualTo("horizontal|vertical")
            assertThat(info["widgetCategory"]).isEqualTo("home_screen")
            assertThat(info["widgetFeatures"]).isEqualTo("reconfigurable|configuration_optional")
            assertThat(info["configure"]).isEqualTo("com.callbackdev.tetto.widget.config.WidgetConfigActivity")
            assertThat(info.keys).containsNoneOf("maxResizeWidth", "maxResizeHeight")
        }
    }

    @Test
    fun `at a glance is placed four by two, in words four by one`() {
        val glance = provider("widget_glance_info.xml")
        val words = provider("widget_words_info.xml")
        assertThat(glance["targetCellWidth"] to glance["targetCellHeight"]).isEqualTo("4" to "2")
        assertThat(words["targetCellWidth"] to words["targetCellHeight"]).isEqualTo("4" to "1")
    }

    @Test
    fun `every provider names a preview that exists and inflates in RemoteViews`() {
        listOf("widget_glance_info.xml", "widget_words_info.xml").map(::provider).forEach { info ->
            val name = checkNotNull(info["previewLayout"]).removePrefix("@layout/")
            val preview = File(layout, "$name.xml")
            assertThat(preview.isFile).isTrue()
            val tags = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""").findAll(preview.readText())
                .map { it.groupValues[1] }
                .filterNot { it == "xml" }
                .toSet()
            assertThat(remoteViewsTags).containsAtLeastElementsIn(tags)
        }
    }

    @Test
    fun `the static preview's card is the default card`() {
        val colors = File("src/main/res/values/colors.xml").readText()
        assertThat(colors).contains("<color name=\"widget_preview_card\">#FF0F3B6B</color>")
    }
}
