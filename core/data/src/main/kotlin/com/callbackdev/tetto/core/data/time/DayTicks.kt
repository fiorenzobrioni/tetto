package com.callbackdev.tetto.core.data.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject

/**
 * The days as a visible page lives through them (PLANNING.md §4.4): today at once, then again at
 * each next local midnight, and at once whenever the clock, the date or the zone is changed, which
 * re-arms the wait. Bound to its collector: nothing is armed or registered while no page collects,
 * and a page stops collecting when it is no longer visible (§9). The wait is the coroutine's own,
 * which a sleeping phone does not count; a page shown again collects afresh, and reads today then.
 */
class DayTicks
@Inject
constructor(@ApplicationContext private val context: Context, private val clock: TodaySource) {
    fun days(): Flow<LocalDate> = channelFlow {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                changes.trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            while (true) {
                send(clock.today())
                val wait = Duration.between(clock.now(), clock.nextMidnight()).toMillis().coerceAtLeast(1)
                withTimeoutOrNull(wait) { changes.receive() }
            }
        } finally {
            context.unregisterReceiver(receiver)
        }
    }
}
