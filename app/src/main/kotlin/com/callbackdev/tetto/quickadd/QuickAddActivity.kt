package com.callbackdev.tetto.quickadd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.data.lock.AppLock
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.reminder.EveningReminder
import com.callbackdev.tetto.feature.caps.QuickAddRoute
import com.callbackdev.tetto.lock.LockedSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The numpad over the home screen (PLANNING.md §7, Taps; §8): a translucent activity in its own
 * task, kept out of the recent apps, opened by a card's cap or a launcher shortcut on one cap
 * (`QuickAdd.intent`). It wears the reader's appearance and holds the caps screen's numpad in a
 * sheet; it closes when the sheet does, and when the reader leaves it (home, the screen off), so
 * nothing waits half-typed behind the home screen.
 *
 * A spend added here answers the evening reminder's question as the app's return does: its
 * notification is taken back (§15, Phase 4).
 *
 * With the lock on (owner, 6 Nov 2026), the sheet holds the lock instead of the numpad until the
 * phone's own lock says yes: the same lock as the caps screen's ([AppLock]), so going from one to
 * the other asks once.
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var reminder: EveningReminder

    @Inject lateinit var appLock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            TettoTheme(
                darkTheme = when (current.theme) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = current.dynamicColor,
                palette = current.palette,
                font = current.font,
            ) {
                val unlocked by appLock.unlocked.collectAsStateWithLifecycle()
                val locked = remember(current.appLock, unlocked) { appLock.guards(current.appLock) }
                if (locked) {
                    val scope = rememberCoroutineScope()
                    val title = stringResource(R.string.lock_prompt_title)
                    LaunchedEffect(Unit) {
                        lifecycle.withResumed { }
                        appLock.askOnce(this@QuickAddActivity, title)
                    }
                    LockedSheet(onUnlock = {
                        scope.launch { appLock.ask(this@QuickAddActivity, title) }
                    }, onClose = ::finish)
                } else {
                    QuickAddRoute(
                        onClose = ::finish,
                        onOpenApp = ::openApp,
                        onAdded = { reminder.appOpened() },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.pageShown()
    }

    override fun onStop() {
        super.onStop()
        appLock.pageHidden(turning = isChangingConfigurations)
        // The phone's own lock screen over the sheet is not the reader leaving: it answers here.
        if (!isChangingConfigurations && !appLock.asking) finish()
    }

    override fun finish() {
        super.finish()
        // The sheet has already slid away: the window goes without an animation of its own.
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    }

    private fun openApp() {
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }
}
