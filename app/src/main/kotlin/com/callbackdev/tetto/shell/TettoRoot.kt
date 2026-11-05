package com.callbackdev.tetto.shell

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.feature.caps.ArrangeCapsRoute
import com.callbackdev.tetto.feature.caps.CapEditorRoute
import com.callbackdev.tetto.feature.caps.CapsRoute
import kotlinx.serialization.Serializable

/*
 * The shell's pages as Navigation 3 keys, on one back stack: back is previewed under the finger
 * (predictive back), and the stack survives a rotation and the process's death. The numpad and a
 * cap's sheet are the caps screen's sheets, not pages: they close by the system's gestures.
 */

@Serializable
data object CapsKey : NavKey

/** The cap editor: a new cap with [capId] null, else that cap. */
@Serializable
data class EditorKey(val capId: Long? = null) : NavKey

/** The order, from "Change the order" under the tiles. */
@Serializable
data object ArrangeKey : NavKey

/**
 * The shell (PLANNING.md §11 Phase 2). Settings' gear joins with Settings (Phase 3): no entry is
 * drawn before its page exists (owner, 5 Nov 2026). The transitions: a short slide with a fade,
 * the 300 ms predictive back seeks, a plain fade under reduced motion.
 */
@Composable
fun TettoRoot() {
    val backStack = rememberNavBackStack(CapsKey)
    val reduced = reducedMotion()
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeAt(backStack.lastIndex) }
    Surface(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = goBack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            transitionSpec = { forward(reduced) },
            popTransitionSpec = { backward(reduced) },
            predictivePopTransitionSpec = { backward(reduced) },
            entryProvider = entryProvider<NavKey> {
                entry<CapsKey> {
                    CapsRoute(
                        onNewCap = { backStack.add(EditorKey()) },
                        onEditCap = { backStack.add(EditorKey(it)) },
                        onChangeOrder = { backStack.add(ArrangeKey) },
                    )
                }
                entry<EditorKey> { key -> CapEditorRoute(capId = key.capId, onBack = goBack) }
                entry<ArrangeKey> { ArrangeCapsRoute(onBack = goBack) }
            },
        )
    }
}

private const val NAV_MILLIS = 300
private const val SLIDE_DIVISOR = 6

private fun forward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(TettoMotion.fade()) togetherWith fadeOut(TettoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR })
}

private fun backward(reduced: Boolean): ContentTransform {
    if (reduced) return fadeIn(TettoMotion.fade()) togetherWith fadeOut(TettoMotion.fade())
    val curve = tween<Float>(NAV_MILLIS, easing = FastOutSlowInEasing)
    return (
        fadeIn(curve) + slideInHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { -it / SLIDE_DIVISOR }
        ) togetherWith
        (fadeOut(curve) + slideOutHorizontally(tween(NAV_MILLIS, easing = FastOutSlowInEasing)) { it / SLIDE_DIVISOR })
}
