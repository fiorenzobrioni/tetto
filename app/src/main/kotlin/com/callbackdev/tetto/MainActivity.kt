package com.callbackdev.tetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.shell.TettoRoot
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The one activity, edge to edge. It wears the reader's appearance (theme, dress, typeface,
 * wallpaper colours: PLANNING.md §11 Phase 3) and hands the pages to [TettoRoot], which starts
 * with the first run until it is done.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                    TettoRoot(current)
                }
            }
        }
    }
}
