package com.callbackdev.tetto.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import com.callbackdev.tetto.core.model.AppPalette

/** One dress: a scheme and its semantic colors, measured together and never mixed. */
@Immutable
internal data class TettoPalette(
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightColors: TettoColors,
    val darkColors: TettoColors,
) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme

    fun colors(dark: Boolean): TettoColors = if (dark) darkColors else lightColors
}

/** Warm paper and amber. */
private val Paper = TettoPalette(PaperLightScheme, PaperDarkScheme, PaperLightColors, PaperDarkColors)

/** Daylight white and azure: the default. */
private val Vivid = TettoPalette(VividLightScheme, VividDarkScheme, VividLightColors, VividDarkColors)

internal fun paletteFor(choice: AppPalette): TettoPalette = when (choice) {
    AppPalette.PAPER -> Paper
    AppPalette.VIVID -> Vivid
}
