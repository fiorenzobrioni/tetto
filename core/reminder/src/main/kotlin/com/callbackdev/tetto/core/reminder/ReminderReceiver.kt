package com.callbackdev.tetto.core.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The reminder's alarm going off, and what moves it (PLANNING.md §8): a boot, which clears every
 * alarm, and an update, which stops the process (each starts the process, whose application arms
 * the alarm, and arms it here too); the clock or the zone changing, which would leave 20:30 at the
 * old 20:30; the app's notifications or the reminder's channel turned off or on, which the system
 * tells this package alone. Every one is exempt from the limits on implicit broadcasts, so a
 * manifest receiver hears it.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val dueAt = intent.getLongExtra(ReminderAlarm.EXTRA_DUE_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        val reminder = EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
            .eveningReminder()
        val pending = goAsync()
        Work.launch {
            try {
                when (action) {
                    ReminderAlarm.ACTION_REMIND -> reminder.fire(dueAt)

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    -> reminder.reschedule()

                    NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED,
                    NotificationManager.ACTION_NOTIFICATION_CHANNEL_BLOCK_STATE_CHANGED,
                    -> reminder.accessMayHaveChanged()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val Work = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

/** The reminder, for a receiver the system makes outside Hilt's reach. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun eveningReminder(): EveningReminder
}
