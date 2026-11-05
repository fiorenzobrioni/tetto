package com.callbackdev.tetto.widget.config

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.TettoWidgets
import com.callbackdev.tetto.widget.WidgetKind
import com.callbackdev.tetto.widget.WidgetLookStore
import com.callbackdev.tetto.widget.WidgetModel
import com.callbackdev.tetto.widget.WidgetModelLoader
import com.callbackdev.tetto.widget.WidgetSamples
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The launcher's door into one card's settings (`android:configure`, reconfigurable and optional:
 * a card is placed at once with the defaults, and a long press opens this). It wears the app's own
 * appearance: it is part of the app.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var looks: WidgetLookStore

    @Inject lateinit var loader: WidgetModelLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val target = configTarget(applicationContext, intent)
        if (target == null) {
            finish()
            return
        }
        val (appWidgetId, kind) = target
        // The host expects this result whether or not anything changes.
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        val placed = placedWidgetSize(applicationContext, appWidgetId)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            var look by remember { mutableStateOf<WidgetLook?>(null) }
            var model by remember { mutableStateOf<WidgetModel?>(null) }
            LaunchedEffect(appWidgetId) {
                look = looks.lookFor(appWidgetId)
                model = runCatching { loader.load(appWidgetId) }.getOrNull()
            }

            fun save(next: WidgetLook) {
                look = next
                scope.launch {
                    looks.set(appWidgetId, next)
                    runCatching { TettoWidgets.updateOne(applicationContext, appWidgetId) }
                }
            }

            TettoTheme(
                darkTheme = when (settings?.theme) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
                },
                dynamicColor = settings?.dynamicColor ?: false,
                palette = settings?.palette ?: AppPalette.VIVID,
                font = settings?.font ?: AppFont.GOOGLE_SANS,
            ) {
                // Until the stored look is read, nothing: a screen of defaults about to change
                // would show the reader switches in the wrong place.
                val current = look ?: return@TettoTheme
                val read = model
                // With no cap yet, the preview draws sample caps, and says so.
                val sample = read != null && read.caps.isEmpty()
                val preview = when {
                    read == null -> null

                    sample -> WidgetSamples.model(
                        this@WidgetConfigActivity,
                        words = kind == WidgetKind.WORDS,
                        currency = read.currency,
                        settings = read.settings,
                    )

                    else -> read
                }
                WidgetConfigScreen(
                    kind = kind,
                    look = current,
                    model = read,
                    preview = preview,
                    placed = placed,
                    sample = sample,
                    onLook = ::save,
                    onOpacityDrag = { look = current.copy(opacityPct = it) },
                    onOpacityDone = { look?.let(::save) },
                    onDone = ::finish,
                )
            }
        }
    }
}

/**
 * The card a configure request names, when it is one of Tetto's placed cards. The screen is
 * exported, because the launcher opens it, so any app could open it with any id: one that is not a
 * Tetto card on a home screen (Android names its provider only to the provider and the host) gets
 * nothing, rather than a screen drawn from the reader's caps that would write a look for a card
 * that does not exist.
 */
internal fun configTarget(context: Context, intent: Intent?): Pair<Int, WidgetKind>? {
    val appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
    return TettoWidgets.kindOf(context, appWidgetId)?.let { appWidgetId to it }
}
