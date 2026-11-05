package com.callbackdev.tetto.core.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.Duration

/**
 * The reminder's channel and its one notification (PLANNING.md §8): `reminder`, at the default
 * importance, since it is something the reader asked for in Settings. One sentence, a question
 * about today; a touch opens the caps screen, and it goes by itself when the day it asks about
 * ends.
 */
internal class ReminderNotifications(private val context: Context) {
    /** Made, or renamed into the app's language, at every start of the process. */
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** "Anything to add today?", for [lifetime]: the rest of the day it asks about. */
    fun post(lifetime: Duration) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tetto)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setTimeoutAfter(lifetime.toMillis())
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** Takes the reminder back from the shade, if it is there. */
    fun cancel() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    /**
     * The app's own front door: this module does not know its activity, and should not. The caps
     * screen is where it opens.
     */
    private fun openApp(): PendingIntent? = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
        PendingIntent.getActivity(context, NOTIFICATION_ID, it, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val CHANNEL_ID = "reminder"
        const val NOTIFICATION_ID = 1
    }
}

/** What stands between the reminder and the reader, if anything. */
enum class ReminderBlock {
    /** It can show. */
    NONE,

    /** Tetto may not notify at all: the permission not given yet, refused, or its notifications off. */
    APP,

    /** Tetto may notify, but the reader turned the reminder's channel off in the system's settings. */
    CHANNEL,
}

/**
 * Whether the reminder the reader turned on can reach them: a switch that is on while Android
 * drops what it sends would be the screen lying, so Settings says so and opens the page that
 * fixes it, and no alarm wakes the phone for a notification that cannot show (PLANNING.md §8).
 */
object ReminderAccess {
    fun block(context: Context): ReminderBlock {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return ReminderBlock.APP
        val channel = manager.getNotificationChannel(ReminderNotifications.CHANNEL_ID)
        return if (channel?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) {
            ReminderBlock.CHANNEL
        } else {
            ReminderBlock.NONE
        }
    }

    /** The system's page that lifts [block]: the reminder's channel, or the app's notifications. */
    fun settingsIntent(context: Context, block: ReminderBlock): Intent = if (block == ReminderBlock.CHANNEL) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, ReminderNotifications.CHANNEL_ID)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
}
