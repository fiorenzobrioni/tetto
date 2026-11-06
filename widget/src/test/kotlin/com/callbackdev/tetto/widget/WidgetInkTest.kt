package com.callbackdev.tetto.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.callbackdev.tetto.core.designsystem.theme.widgetCardContainer
import com.callbackdev.tetto.core.designsystem.theme.widgetDress
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The ink rule (`docs/adr/0003-widgets.md`), and what it gives on every solid ground: the words at
 * 4.5:1 or better, the over ink too (it carries "€30 over" in words), and the marks a ring and a
 * bar draw at 3:1 or better.
 */
class WidgetInkTest {
    @Test
    fun `a solid card is its own ground`() {
        assertThat(
            widgetInk(WidgetGround.COLOR, 100, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.OVER_COLOR)
        assertThat(
            widgetInk(WidgetGround.LIGHT, 100, night = true, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.DARK, 100, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 100, night = true, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 100, night = false, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
    }

    @Test
    fun `a see-through card asks the wallpaper only where the reader named no ink`() {
        assertThat(
            widgetInk(WidgetGround.LIGHT, 20, night = true, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.DARK, 20, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_DARK)
        assertThat(
            widgetInk(WidgetGround.COLOR, 20, night = false, wallpaperCarriesDarkInk = true),
        ).isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(
            widgetInk(WidgetGround.SYSTEM, 20, night = false, wallpaperCarriesDarkInk = false),
        ).isEqualTo(WidgetInk.ON_DARK)
    }

    @Test
    fun `on every solid ground, the words and the over ink read at 4·5 to 1, the marks at 3 to 1`() {
        AppPalette.entries.forEach { palette ->
            val dress = widgetDress(palette)
            val grounds = listOf(
                WidgetLook(ground = WidgetGround.LIGHT) to WidgetInk.ON_LIGHT,
                WidgetLook(ground = WidgetGround.DARK) to WidgetInk.ON_DARK,
            ) + WidgetCardColor.entries.map { WidgetLook(cardColor = it) to WidgetInk.OVER_COLOR }
            grounds.forEach { (look, ink) ->
                val palette2 = widgetPaletteFor(ink, look, dress, night = false)
                val ground = cardGround(look, dress, night = false)
                val name = "$palette ${look.ground} ${look.cardColor}"
                assertWithMessage("$name primary").that(contrast(palette2.primary, ground)).isAtLeast(4.5)
                assertWithMessage("$name secondary").that(contrast(palette2.secondary, ground)).isAtLeast(4.5)
                assertWithMessage("$name over").that(contrast(palette2.over, ground)).isAtLeast(4.5)
                assertWithMessage("$name accent").that(contrast(palette2.accent, ground)).isAtLeast(3.0)
                assertWithMessage("$name attention").that(contrast(palette2.attention, ground)).isAtLeast(3.0)
            }
        }
    }

    @Test
    fun `the six colours are the family's`() {
        assertThat(widgetCardContainer(WidgetCardColor.BLUE)).isEqualTo(Color(0xFF0F3B6B))
        assertThat(widgetCardContainer(WidgetCardColor.CLAY)).isEqualTo(Color(0xFF7A3320))
    }

    private fun contrast(ink: Color, ground: Color): Double {
        val a = ink.compositeOver(ground).luminance().toDouble()
        val b = ground.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }
}
