package com.callbackdev.tetto.shell

import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.feature.caps.ArrangeCapsRoute
import com.callbackdev.tetto.feature.caps.CapEditorRoute
import com.callbackdev.tetto.feature.caps.CapsRoute
import com.callbackdev.tetto.feature.guide.GuideRoute
import com.callbackdev.tetto.feature.onboarding.OnboardingRoute
import com.callbackdev.tetto.feature.settings.SettingsRoute
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

/** Settings, from the caps screen's gear. */
@Serializable
data object SettingsKey : NavKey

/** The guide, from the card first in Settings and from the caps screen's first-day tip (Phase 6). */
@Serializable
data object GuideKey : NavKey

/**
 * The shell (PLANNING.md §11 Phases 2, 3, 5 and 6): the first run until it is done, then the pages. The
 * first run stands before them, not on the back stack: it is not a page to go back to. A run that
 * made caps ends on the home screen's card, where the launcher can place one and none is there
 * yet (PLANNING.md §8), then on the evening reminder, where it can still be turned on (owner,
 * 7 Nov 2026); the caps are already made then, so a process ended on either page loses nothing
 * and makes nothing twice.
 */
@Composable
fun TettoRoot(settings: UserSettings) {
    val offer: HomeOfferViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = LocalActivity.current
    var offeringHome by rememberSaveable { mutableStateOf(false) }
    var offeringReminder by rememberSaveable { mutableStateOf(false) }
    if (!settings.onboardingCompleted) {
        OnboardingRoute(
            onFinishing = { withCaps ->
                offeringHome = withCaps && offer.worthOffering()
                // No cap, no reminder: it has nothing to ask about.
                offeringReminder = withCaps && reminderWorthOffering(context, activity, settings)
            },
        )
        return
    }
    if (offeringHome) {
        HomeOfferRoute(onLeave = { offeringHome = false }, viewModel = offer)
        return
    }
    if (offeringReminder) {
        ReminderOfferRoute(onLeave = { offeringReminder = false })
        return
    }
    MainPages()
}

/**
 * The pages, from the caps screen. The transitions: a short slide with a fade, the 300 ms
 * predictive back seeks, a plain fade under reduced motion.
 */
@Composable
private fun MainPages() {
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
                        onOpenSettings = { backStack.add(SettingsKey) },
                        onOpenGuide = { backStack.add(GuideKey) },
                    )
                }
                entry<EditorKey> { key -> CapEditorRoute(capId = key.capId, onBack = goBack) }
                entry<ArrangeKey> { ArrangeCapsRoute(onBack = goBack) }
                entry<SettingsKey> { SettingsRoute(onBack = goBack, onOpenGuide = { backStack.add(GuideKey) }) }
                entry<GuideKey> { GuideRoute(onBack = goBack) }
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
