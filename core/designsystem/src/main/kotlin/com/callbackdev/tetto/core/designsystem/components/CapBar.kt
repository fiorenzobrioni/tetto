package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.model.Share

/**
 * What a cap spent, as a bar (VISION, «Caps»): its fill in the accent, in the *attention* ink when
 * spending runs ahead of the days, with a thin mark where today falls in the period. Past the cap
 * it is full and hatched: a shape as well as a colour, and the words beside it say "over". No
 * traffic lights. Decorative: the tile's sentence says all of it.
 */
@Composable
fun CapBar(bar: Share, mark: Share?, over: Boolean, ahead: Boolean, modifier: Modifier = Modifier) {
    val reduced = reducedMotion()
    val target = if (over) 1f else bar.fraction()
    val fill by animateFloatAsState(target, TettoMotion.spatial(reduced), label = "cap bar")
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val ink = when {
        over -> MaterialTheme.colorScheme.error
        ahead -> TettoTheme.colors.attention
        else -> MaterialTheme.colorScheme.primary
    }
    val hatch = MaterialTheme.colorScheme.onError
    val markInk = MaterialTheme.colorScheme.onSurface
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.fillMaxWidth().height(BarHeight + MarkOverhang * 2)) {
        val barTop = MarkOverhang.toPx()
        val height = BarHeight.toPx()
        val radius = CornerRadius(height / 2)
        drawRoundRect(track, Offset(0f, barTop), Size(size.width, height), radius)
        val filled = size.width * fill
        if (filled > 0f) {
            val left = if (rtl) size.width - filled else 0f
            drawRoundRect(ink, Offset(left, barTop), Size(filled, height), radius)
            if (over) {
                val clip = Path().apply {
                    addRoundRect(
                        RoundRect(left, barTop, left + filled, barTop + height, radius),
                    )
                }
                clipPath(clip, ClipOp.Intersect) {
                    val step = height * 1.5f
                    var x = -height
                    while (x < size.width) {
                        drawLine(
                            hatch,
                            Offset(x, barTop + height),
                            Offset(x + height, barTop),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                        x += step
                    }
                }
            }
        }
        if (mark != null && !over) {
            val at = size.width * mark.fraction()
            val x = (if (rtl) size.width - at else at).coerceIn(1f, size.width - 1f)
            drawLine(markInk, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
        }
    }
}

/** The share as a length: the screen's arithmetic, never the engine's. */
fun Share.fraction(): Float = (part.toDouble() / whole).coerceIn(0.0, 1.0).toFloat()

private val BarHeight = 8.dp
private val MarkOverhang = 3.dp
