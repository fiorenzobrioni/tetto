package com.callbackdev.tetto.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * The evening reminder's one alarm (PLANNING.md §8, §9): `setAndAllowWhileIdle` on the wall clock,
 * with a wake-up, never exact and needing no permission. One wake a day, only for a reader who
 * asked for the reminder and only while it can show, let through Doze: a question about today
 * that Doze holds until the next morning is worth nothing. It may come some minutes late; it
 * never comes early.
 *
 * The alarm carries the instant it was due, so a late delivery can tell whether its day is over.
 */
internal class ReminderAlarm(private val context: Context) {
    private val manager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun set(at: Instant) {
        manager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent(at))
    }

    fun cancel() {
        manager?.cancel(intent(null))
    }

    // One fixed request code with FLAG_UPDATE_CURRENT: setting it again replaces it, so the app
    // never collects a queue of alarms it forgot about. Extras do not count in the match, so the
    // same intent cancels it.
    private fun intent(dueAt: Instant?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        dueAt?.let { intent.putExtra(EXTRA_DUE_AT, it.toEpochMilli()) }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMIND = "com.callbackdev.tetto.action.EVENING_REMINDER"
        const val EXTRA_DUE_AT = "com.callbackdev.tetto.extra.DUE_AT"
        private const val REQUEST_CODE = 0x701
    }
}
