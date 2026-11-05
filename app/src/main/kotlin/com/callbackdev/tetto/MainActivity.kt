package com.callbackdev.tetto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.shell.TettoRoot
import dagger.hilt.android.AndroidEntryPoint

/**
 * The one activity, edge to edge, in the default dress and the phone's light or dark. The reader's
 * own appearance (theme, dress, typeface, wallpaper colours) arrives with the settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TettoTheme {
                TettoRoot()
            }
        }
    }
}
