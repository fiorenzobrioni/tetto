package com.callbackdev.tetto.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.collection.intSetOf
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.widget.HomeCard
import com.callbackdev.tetto.core.data.widget.HomeScreenCards
import com.callbackdev.tetto.widget.glance.GlanceWidgetReceiver
import com.callbackdev.tetto.widget.words.WordsWidgetReceiver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's side of the refresh (PLANNING.md §7, "a write"): every change of the caps, their spends
 * or their closed periods, once its transaction is committed, and every change of a setting a card
 * draws with (the currency, the appearance, a week or month start), redraws the cards at once.
 * Whatever wrote it: the caps screen, the numpad over the home screen, the first run, a restore.
 * Only with a card on a home screen; nothing runs on a timer, and the listener costs nothing until
 * something is written.
 */
@Singleton
class WidgetUpdater
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val caps: CapsRepository,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** From the Application: listens for as long as the process lives. */
    fun start() {
        scope.launch {
            merge(caps.changes, settings.settings.distinctUntilChanged().drop(1).map { })
                .conflate()
                .collect { redraw() }
        }
    }

    private suspend fun redraw() {
        if (TettoWidgets.hasWidgets(context)) runCatching { TettoWidgets.updateAll(context) }
    }
}

/** The pin request and its answers (PLANNING.md §8), for the first run and Settings. */
@Singleton
class PinnedCards @Inject constructor(@ApplicationContext private val context: Context) : HomeScreenCards {
    private val manager: AppWidgetManager? get() = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()

    override fun canPin(): Boolean = manager?.isRequestPinAppWidgetSupported == true

    override fun pin(card: HomeCard): Boolean {
        val kind = when (card) {
            HomeCard.AT_A_GLANCE -> WidgetKind.GLANCE
            HomeCard.IN_WORDS -> WidgetKind.WORDS
        }
        return runCatching { manager?.requestPinAppWidget(TettoWidgets.provider(context, kind), null, null) == true }
            .getOrDefault(false)
    }

    override fun placed(): Boolean = TettoWidgets.hasWidgets(context)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class WidgetModule {
    @Binds
    abstract fun homeScreenCards(pins: PinnedCards): HomeScreenCards
}

/**
 * The picker's generated previews (Android 15+): the real cards, drawn from [WidgetSamples] by
 * `providePreview`, published once per app version. The platform rate-limits the call, and a
 * preview only changes when the app does, so the version it was published for is remembered; a
 * refused call is tried again at the next start. Below Android 15 the static `previewLayout` is
 * what the picker shows.
 */
object WidgetPreviews {
    suspend fun publishIfNeeded(context: Context, appVersion: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val store = context.widgetLookDataStore
        if (store.data.first()[PublishedFor] == appVersion) return
        val manager = GlanceAppWidgetManager(context)
        val categories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
        // Bounded: this runs at every app start until it succeeds, and nothing is worth a
        // coroutine left hanging on the launcher's side of a binder call.
        val results = listOf(GlanceWidgetReceiver::class, WordsWidgetReceiver::class).map { receiver ->
            runCatching {
                withTimeoutOrNull(PUBLISH_TIMEOUT_MILLIS) { manager.setWidgetPreviews(receiver, categories) }
            }
                .onFailure { logWidgetFailure("Publishing the picker preview failed", it) }
                .getOrNull()
        }
        if (results.all { it == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS }) {
            store.edit { it[PublishedFor] = appVersion }
        }
    }

    private val PublishedFor = longPreferencesKey("previews_published_for")
    private const val PUBLISH_TIMEOUT_MILLIS = 30_000L
}
