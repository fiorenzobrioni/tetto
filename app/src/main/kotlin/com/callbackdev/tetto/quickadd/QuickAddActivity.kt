package com.callbackdev.tetto.quickadd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.reminder.EveningReminder
import com.callbackdev.tetto.feature.caps.QuickAddRoute
import dagger.hilt.android.AndroidEntryPoint
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
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var reminder: EveningReminder

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
                QuickAddRoute(
                    onClose = ::finish,
                    onOpenApp = ::openApp,
                    onAdded = { reminder.appOpened() },
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
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
