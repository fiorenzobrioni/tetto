package com.callbackdev.tetto.widget

import android.util.Log
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetLook
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a card knows when it draws: the caps as the caps screen shows them, read at render time
 * after the catch-up (PLANNING.md §4.4: a card never sees a period that should have closed), in
 * the reader's order, and never invented.
 *
 * @property caps every cap; which of them the card shows is its look's ([com.callbackdev.tetto
 *   .core.domain.widget.WidgetCaps]).
 * @property unavailable the caps could not be read (an error, or a read that did not finish in
 *   time): said, not waited on.
 */
data class WidgetModel(
    val look: WidgetLook,
    val settings: UserSettings,
    val currency: AppCurrency,
    val today: LocalDate,
    val caps: List<CapState>,
    val unavailable: Boolean = false,
) {
    companion object {
        /** A card that says it could not read the caps, in the default dress. */
        fun unavailable(): WidgetModel = WidgetModel(
            look = WidgetLook(),
            settings = UserSettings(),
            currency = AppCurrency("EUR", 2),
            today = LocalDate.now(),
            caps = emptyList(),
            unavailable = true,
        )
    }
}

/** Reads a [WidgetModel]: the catch-up, then the caps' states, the settings and the card's look. */
@Singleton
class WidgetModelLoader
@Inject
constructor(
    private val caps: CapsRepository,
    private val settings: SettingsRepository,
    private val looks: WidgetLookStore,
    private val clock: TodaySource,
) {
    suspend fun load(appWidgetId: Int): WidgetModel {
        val states = caps.currentStates()
        return WidgetModel(
            look = looks.lookFor(appWidgetId),
            settings = settings.settings.first(),
            currency = settings.currency.first(),
            today = clock.today(),
            caps = states,
        )
    }

    /**
     * [load] for a card that is waiting to be drawn: never longer than [LOAD_TIMEOUT_MILLIS] and
     * never an exception, because Glance shows its loading spinner until `provideContent` is
     * reached and a card stuck on it says nothing to anybody. A failure is logged under [TAG] and
     * drawn as a card that says the caps could not be read; the next redraw tries again.
     */
    suspend fun loadForCard(appWidgetId: Int): WidgetModel =
        guardedLoad(LOAD_TIMEOUT_MILLIS, "card $appWidgetId") { load(appWidgetId) }

    companion object {
        /** The log tag of everything the cards report: `adb logcat -s TettoWidget`. */
        const val TAG: String = "TettoWidget"

        /** A read is a few queries on the phone's own storage; ten seconds is something wrong. */
        const val LOAD_TIMEOUT_MILLIS: Long = 10_000L
    }
}

/**
 * [load], bounded by [timeoutMillis] and never throwing but for cancellation: a failure is logged
 * as [what] and becomes [WidgetModel.unavailable]. Split off the loader so a test can pin it.
 */
internal suspend fun guardedLoad(timeoutMillis: Long, what: String, load: suspend () -> WidgetModel): WidgetModel =
    try {
        withTimeout(timeoutMillis) { load() }
    } catch (e: TimeoutCancellationException) {
        logWidgetFailure("Reading the caps for $what took over $timeoutMillis ms", e)
        WidgetModel.unavailable()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logWidgetFailure("Reading the caps for $what failed", e)
        WidgetModel.unavailable()
    }

/** A failure, in the log; quiet on a JVM without Android's log, where the tests run. */
internal fun logWidgetFailure(message: String, error: Throwable) {
    runCatching { Log.w(WidgetModelLoader.TAG, message, error) }
}

/** A line of what the cards did, for `adb logcat -s TettoWidget`. */
internal fun logWidget(message: String) {
    runCatching { Log.i(WidgetModelLoader.TAG, message) }
}
