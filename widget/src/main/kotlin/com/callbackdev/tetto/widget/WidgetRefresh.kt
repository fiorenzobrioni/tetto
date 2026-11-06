package com.callbackdev.tetto.widget

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The tick the cards' compositions listen to when something they draw has changed, and what each
 * card last drew.
 *
 * Glance runs `provideGlance` once per session and keeps the composition alive for a while; inside
 * that window `update()` only wakes the composition that is already there, so a model read before
 * `provideContent` would stay as it was. So every redraw bumps this first, and
 * [rememberWidgetModel] reloads on it.
 *
 * Each composition reports the revision it has drawn ([drawn]), so whatever asked for a redraw (the
 * midnight alarm, a receiver) can stay until the cards are drawn ([awaitDrawn]): Glance composes in
 * a worker of Tetto's process that no job holds awake once the broadcast that asked has ended, and
 * a process left cached may be frozen with the card half drawn.
 *
 * In memory on purpose: a session cannot outlive the process that runs it.
 */
internal object WidgetRefresh {
    private val ticks = MutableStateFlow(0L)

    val revision: StateFlow<Long> = ticks.asStateFlow()

    /** The newest revision each card has drawn, by `appWidgetId`. */
    private val drawnRevisions = MutableStateFlow<Map<Int, Long>>(emptyMap())

    /** A new revision, which every live composition reloads on; the revision, for [awaitDrawn]. */
    fun invalidate(): Long = ticks.updateAndGet { it + 1 }

    /** [appWidgetId]'s composition has drawn what it read at [revision]. */
    fun drawn(appWidgetId: Int, revision: Long) {
        drawnRevisions.update { drawn ->
            if ((drawn[appWidgetId] ?: Long.MIN_VALUE) >= revision) drawn else drawn + (appWidgetId to revision)
        }
    }

    /** Removed cards: nothing of theirs is kept. */
    fun forget(appWidgetIds: IntArray) {
        drawnRevisions.update { drawn -> drawn - appWidgetIds.toSet() }
    }

    /**
     * Until every card in [appWidgetIds] has drawn [revision] or later, then [DRAW_GRACE_MILLIS] for
     * Glance to hand the views to the launcher; never longer than [timeoutMillis]. Whether they all
     * made it, for the log.
     */
    suspend fun awaitDrawn(appWidgetIds: Collection<Int>, revision: Long, timeoutMillis: Long): Boolean {
        val all = withTimeoutOrNull(timeoutMillis) {
            drawnRevisions.first { drawn -> appWidgetIds.all { (drawn[it] ?: Long.MIN_VALUE) >= revision } }
        } != null
        if (all) delay(DRAW_GRACE_MILLIS)
        return all
    }

    private const val DRAW_GRACE_MILLIS = 500L
}
