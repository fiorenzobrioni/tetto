package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion

/**
 * A figure that rolls to its new value (VISION, «The numpad»: after Add, the cap's figure rolls):
 * down when it falls, up when it grows, by [value]; a fade under reduced motion. The text is there
 * at once either way: the motion never gates the number.
 *
 * Always one line: a figure broken across two reads as two numbers. It keeps [style]'s size
 * wherever it fits, and only a figure too long for its width (millions, or a currency's long
 * symbol) steps down, never below [minSize].
 */
@Composable
fun RollingText(
    text: String,
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minSize: TextUnit = 16.sp,
) {
    val reduced = reducedMotion()
    val ink = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    AnimatedContent(
        targetState = Rolled(text, value),
        modifier = modifier,
        transitionSpec = {
            if (reduced || initialState.value == targetState.value) {
                fadeIn(TettoMotion.fade()) togetherWith fadeOut(TettoMotion.fade())
            } else {
                val down = targetState.value < initialState.value
                (
                    slideInVertically(TettoMotion.spatial()) { if (down) -it else it } +
                        fadeIn(TettoMotion.effects())
                    ) togetherWith
                    (
                        slideOutVertically(TettoMotion.spatial()) { if (down) it else -it } +
                            fadeOut(TettoMotion.effects())
                        )
            }
        },
        contentKey = { it.text },
        label = "rolling figure",
    ) { rolled ->
        BasicText(
            rolled.text,
            style = style.copy(color = ink),
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = style.fontSize, stepSize = 1.sp),
        )
    }
}

private data class Rolled(val text: String, val value: Long)
