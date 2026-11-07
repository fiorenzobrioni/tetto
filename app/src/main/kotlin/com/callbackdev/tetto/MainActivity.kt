package com.callbackdev.tetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import com.callbackdev.tetto.core.data.lock.AppLock
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.reminder.EveningReminder
import com.callbackdev.tetto.lock.LockedPage
import com.callbackdev.tetto.shell.TettoRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The one activity, edge to edge. It wears the reader's appearance (theme, dress, typeface,
 * wallpaper colours: PLANNING.md §11 Phase 3) and hands the pages to [TettoRoot], which starts
 * with the first run until it is done. Each time it comes to the front, the evening reminder
 * hears of it (PLANNING.md §8): its question has brought the reader here, and the notifications
 * may have been turned on or off meanwhile.
 *
 * With the lock on (owner, 6 Nov 2026), nothing of the reader's is composed until the phone's own
 * lock says yes ([AppLock]): at start, and after a minute away; and the recent apps keep no picture
 * of it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var reminder: EveningReminder

    @Inject lateinit var appLock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // The recent apps' picture of Tetto is the reader's caps: none while the lock is on.
        lifecycleScope.launch {
            settingsRepository.settings.map { it.appLock }.distinctUntilChanged().collect { on ->
                setRecentsScreenshotEnabled(!on)
            }
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (settings?.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            // The bars' ink follows the applied theme, not the system's: a reader who forces
            // light on a dark phone would otherwise get white icons on a white page.
            val view = LocalView.current
            DisposableEffect(dark) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
                onDispose { }
            }
            val current = settings
            if (current == null) {
                // One bare frame until the settings are read: the wrong dress, or the first run
                // flashing before the caps, would be worse.
                TettoTheme(darkTheme = dark) { Surface(Modifier.fillMaxSize()) { } }
            } else {
                TettoTheme(
                    darkTheme = dark,
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
                            appLock.askOnce(this@MainActivity, title)
                        }
                        LockedPage(onUnlock = { scope.launch { appLock.ask(this@MainActivity, title) } })
                    } else {
                        TettoRoot(current)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appLock.pageShown()
        reminder.appOpened()
    }

    override fun onStop() {
        super.onStop()
        appLock.pageHidden(turning = isChangingConfigurations)
    }
}
