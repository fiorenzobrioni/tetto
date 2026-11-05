package com.callbackdev.tetto.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette

/**
 * Tetto's theme: the design language of `docs/adr/0002-design-language.md`. The defaults are the
 * vivid dress, Google Sans, the app's own colors. [dynamicColor] takes Material's roles from the wallpaper instead; the
 * semantic colors ([TettoColors]) keep following the dress, because a warning must
 * not change color with a photo.
 */
@Composable
fun TettoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: AppPalette = AppPalette.VIVID,
    font: AppFont = AppFont.GOOGLE_SANS,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dress = paletteFor(palette)
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> dress.scheme(darkTheme)
    }
    CompositionLocalProvider(
        LocalTettoColors provides dress.colors(darkTheme),
        LocalTettoType provides tettoType(font),
        LocalReducedMotion provides rememberReducedMotion(context),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = tettoTypography(font),
            shapes = TettoShapes,
            content = content,
        )
    }
}

/** The design system's own values, next to [MaterialTheme]'s. */
object TettoTheme {
    val colors: TettoColors
        @Composable @ReadOnlyComposable
        get() = LocalTettoColors.current

    val type: TettoType
        @Composable @ReadOnlyComposable
        get() = LocalTettoType.current
}
