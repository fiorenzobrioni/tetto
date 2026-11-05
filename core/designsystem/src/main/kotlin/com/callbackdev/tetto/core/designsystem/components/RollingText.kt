package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion

/**
 * A figure that rolls to its new value (VISION, «The numpad»: after Add, the cap's figure rolls):
 * down when it falls, up when it grows, by [value]; a fade under reduced motion. The text is there
 * at once either way: the motion never gates the number.
 */
@Composable
fun RollingText(
    text: String,
    value: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    val reduced = reducedMotion()
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
        Text(rolled.text, style = style, color = color)
    }
}

private data class Rolled(val text: String, val value: Long)
