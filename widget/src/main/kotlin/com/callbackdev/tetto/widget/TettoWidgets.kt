package com.callbackdev.tetto.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.callbackdev.tetto.widget.glance.GlanceWidget
import com.callbackdev.tetto.widget.glance.GlanceWidgetReceiver
import com.callbackdev.tetto.widget.refresh.MidnightAlarm
import com.callbackdev.tetto.widget.words.WordsWidget
import com.callbackdev.tetto.widget.words.WordsWidgetReceiver
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** The two cards, as the settings screen and the previews need to tell them apart. */
enum class WidgetKind {
    /** «At a glance»: the caps, each its icon, what is left and a slim bar. */
    GLANCE,

    /** «In words»: one cap in the family's ring, and the words. */
    WORDS,
}

/**
 * The cards as one household: who is placed, and how to redraw them. Redraws go by the system's
 * own mapping of ids to providers, never by Glance's class bookkeeping, and always through
 * [WidgetRefresh] first, because `update()` alone wakes a live session without reloading it.
 */
object TettoWidgets {
    private val household: List<Pair<Class<out GlanceAppWidgetReceiver>, () -> GlanceAppWidget>> = listOf(
        GlanceWidgetReceiver::class.java to { GlanceWidget() },
        WordsWidgetReceiver::class.java to { WordsWidget() },
    )

    /** The provider of each kind, for a pin request. */
    fun provider(context: Context, kind: WidgetKind): ComponentName = ComponentName(
        context,
        when (kind) {
            WidgetKind.GLANCE -> GlanceWidgetReceiver::class.java
            WidgetKind.WORDS -> WordsWidgetReceiver::class.java
        },
    )

    /** Whether a card of Tetto's is on a home screen. */
    fun hasWidgets(context: Context): Boolean {
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull() ?: return false
        return household.any { (receiver, _) ->
            runCatching { manager.getAppWidgetIds(ComponentName(context, receiver)) }.getOrNull()?.isNotEmpty() == true
        }
    }

    /** Which card [appWidgetId] is; null for an id the host has not bound yet. */
    fun kindOf(context: Context, appWidgetId: Int): WidgetKind? =
        when (AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className) {
            GlanceWidgetReceiver::class.java.name -> WidgetKind.GLANCE
            WordsWidgetReceiver::class.java.name -> WidgetKind.WORDS
            else -> null
        }

    /** Every placed card, with a fresh model. */
    suspend fun updateAll(context: Context) {
        update(context) { true }
    }

    /** One card, with a fresh model: its settings screen's «apply now». */
    suspend fun updateOne(context: Context, appWidgetId: Int) {
        update(context) { it == appWidgetId }
    }

    /**
     * Every placed card, with a fresh model, and the caller kept until they are drawn (never longer
     * than [timeoutMillis]): a receiver holds the process awake for exactly the redraw it asked for
     * ([WidgetRefresh]). [why] names it in the log.
     */
    suspend fun redrawAll(context: Context, why: String, timeoutMillis: Long) {
        val started = SystemClock.elapsedRealtime()
        val (revision, ids) = update(context) { true }
        val drawn = WidgetRefresh.awaitDrawn(ids, revision, timeoutMillis)
        val took = SystemClock.elapsedRealtime() - started
        val line = if (drawn) {
            "$why: ${ids.size} card(s) drawn in $took ms"
        } else {
            "$why: not every card drawn in $timeoutMillis ms"
        }
        logWidget(line)
    }

    /** The revision the cards will reload at, and the cards asked to. */
    private suspend fun update(context: Context, which: (Int) -> Boolean): Pair<Long, List<Int>> {
        val revision = WidgetRefresh.invalidate()
        val manager = AppWidgetManager.getInstance(context)
        val glance = GlanceAppWidgetManager(context)
        val updated = mutableListOf<Int>()
        household.forEach { (receiver, widget) ->
            manager.getAppWidgetIds(ComponentName(context, receiver)).filter(which).forEach { id ->
                runCatching { widget().update(context, glance.getGlanceIdBy(id)) }
                    .onSuccess { updated += id }
                    .onFailure { logWidgetFailure("Updating card $id failed", it) }
            }
        }
        return revision to updated
    }
}

/** What Glance's classes, which Hilt does not build, need from the graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun loader(): WidgetModelLoader

    fun looks(): WidgetLookStore

    fun midnight(): MidnightAlarm
}

internal fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/**
 * Where one card's model comes from, for its `provideGlance`: the loader when the graph can hand
 * one over, and otherwise a card that says the caps could not be read. Either way the card reaches
 * `provideContent`: Glance keeps its loading spinner on screen until it does. After each read, the
 * midnight alarm is armed (PLANNING.md §7): the next redraw a card needs by itself.
 */
internal class CardModels(context: Context, private val appWidgetId: Int) {
    private val entry: WidgetEntryPoint? = try {
        context.widgetEntryPoint()
    } catch (e: Exception) {
        Log.e(WidgetModelLoader.TAG, "No model loader for card $appWidgetId", e)
        null
    }

    suspend fun load(): WidgetModel {
        val model = entry?.loader()?.loadForCard(appWidgetId) ?: WidgetModel.unavailable()
        runCatching { entry?.midnight()?.arm() }
        return model
    }
}

/**
 * The receivers' shared duties: a removed card takes its look with it, and the last card gone
 * takes the midnight alarm with it, so nothing of Tetto waits on a midnight no card shows.
 */
abstract class TettoWidgetReceiver : GlanceAppWidgetReceiver() {
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext
        WidgetRefresh.forget(appWidgetIds)
        // Glance's own onDeleted has already held the broadcast open with goAsync, which hands its
        // pending result out once: here it may be null, and finishing a null crashes the process.
        // The look is forgotten in the time Glance's own work keeps the broadcast open; if the
        // process went first, a few bytes stay under an id never reused.
        val pending: PendingResult? = goAsync()
        Cleanup.launch {
            try {
                runCatching { app.widgetEntryPoint().looks().forget(appWidgetIds) }
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Called per provider: the other card may still be on a home screen.
        if (!TettoWidgets.hasWidgets(context)) {
            runCatching { context.widgetEntryPoint().midnight().disarm() }
        }
    }

    private companion object {
        val Cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
