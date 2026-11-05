package com.callbackdev.tetto.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors Material has no role for, per dress and per theme, never flipped: a dark value is
 * chosen for dark. A meaning keeps its color; a family of them is added here with the phase that
 * needs it, not before.
 *
 * - **attention**: an ink for "this needs a look": a cap spending ahead of its period's days,
 *   in an amber, 7:1 or better on either surface.
 *
 * Over a cap is said in words, never by a colour alone (PLANNING.md §6): an ink for it, if a
 * phase needs one, comes here with its contrast test.
 */
@Immutable
data class TettoColors(val attention: Color)

internal val PaperLightColors = TettoColors(attention = Color(0xFF7A5200))

internal val PaperDarkColors = TettoColors(attention = Color(0xFFFFBC27))

internal val VividLightColors = TettoColors(attention = Color(0xFF7A5200))

internal val VividDarkColors = TettoColors(attention = Color(0xFFFFBC27))

val LocalTettoColors = staticCompositionLocalOf { VividLightColors }
