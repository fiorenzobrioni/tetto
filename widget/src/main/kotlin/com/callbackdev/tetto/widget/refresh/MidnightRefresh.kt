package com.callbackdev.tetto.widget.refresh

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.widget.TettoWidgets
import com.callbackdev.tetto.widget.logWidgetFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/*
 * How the cards stay fresh (PLANNING.md §7, §9): pushed, never polled. Four ways in, nothing else:
 *
 * - a write in the app (a spend, a cap, a setting): `WidgetUpdater`, once its transaction is
 *   committed;
 * - midnight, when a period may end and the days left change: one inexact, NON-wakeup alarm at the
 *   next local midnight, armed after every render while a card is placed, delivered when the phone
 *   is next awake, which is when someone can see the card; the render runs the catch-up first;
 * - the clock, the zone or the language changed, or the app was updated: the exempt broadcasts;
 * - a boot: the system sends the cards their update, and the alarm is armed again from it.
 *
 * Whatever asked stays until the cards are drawn (`TettoWidgets.redrawAll`): Glance draws in a
 * worker of Tetto's process that no job holds awake, and a process left cached may be frozen with
 * the card half drawn.
 */

/** The cards' one alarm: the next local midnight, inexact and non-wakeup, only while a card is placed. */
@Singleton
class MidnightAlarm
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val clock: TodaySource,
) {
    /**
     * `AlarmManager.set` with `RTC`: inexact and non-wakeup, so it needs no permission and never
     * wakes the phone. One pending intent, so a new midnight replaces the old.
     */
    fun arm() {
        if (!TettoWidgets.hasWidgets(context)) return disarm()
        val at = clock.nextMidnight().plusMillis(AFTER_MIDNIGHT_MILLIS)
        armAt(at)
    }

    /** The last card is gone: nothing of Tetto waits on a midnight no card shows. */
    fun disarm() {
        context.getSystemService(AlarmManager::class.java)?.cancel(intent(context))
    }

    private fun armAt(at: Instant) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarms.set(AlarmManager.RTC, at.toEpochMilli(), intent(context)) }
            .onFailure { logWidgetFailure("Arming the midnight alarm failed", it) }
    }

    companion object {
        /** A breath past midnight: an alarm is never early, and today is then the new day. */
        private const val AFTER_MIDNIGHT_MILLIS = 1_000L

        /** How long a receiver stays for the cards it asked to redraw. */
        internal const val RECEIVER_DRAW_TIMEOUT_MILLIS = 8_000L

        internal fun intent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, MidnightReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** The midnight alarm: a period may have ended, and the days left have changed. */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = redraw(context, "Midnight")
}

/**
 * The exempt broadcasts (PLANNING.md §7): the clock set, the zone changed (a trip moves the days),
 * the language changed, the app updated. Received from the manifest, delivered by the system only.
 */
class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action in Actions) redraw(context, action.orEmpty().substringAfterLast('.'))
    }

    private companion object {
        val Actions = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/**
 * Every card drawn again within the receiver's own time (`goAsync`), never a service, and the
 * broadcast held open until the cards are drawn. Each render arms the next midnight.
 */
private fun BroadcastReceiver.redraw(context: Context, why: String) {
    val app = context.applicationContext
    if (!TettoWidgets.hasWidgets(app)) return
    val pending = goAsync()
    Redraws.launch {
        try {
            TettoWidgets.redrawAll(app, why, MidnightAlarm.RECEIVER_DRAW_TIMEOUT_MILLIS)
        } finally {
            pending.finish()
        }
    }
}

private val Redraws = CoroutineScope(SupervisorJob() + Dispatchers.Default)
