package com.callbackdev.tetto.core.reminder

import android.content.Context
import android.util.Log
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.domain.reminder.ReminderSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The evening reminder (PLANNING.md §8): "Anything to add today?", at the reader's time, only on a
 * day with caps and no spend added, never otherwise.
 *
 * Nothing here runs on a timer. One inexact alarm ([ReminderAlarm]), armed again whenever what it
 * depends on changes: at every start of the process (a boot and an update start it, and a boot
 * clears every alarm), at a change of the setting or its time ([start]), at a change of the clock or
 * the zone and of the notifications' state ([ReminderReceiver]), and at each firing. While the
 * reminder is off, or its notification could not show, no alarm is armed at all: no wake for
 * nothing (PLANNING.md §8, §9).
 */
@Singleton
class EveningReminder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val preferences: UserPreferencesDataSource,
    private val caps: CapsRepository,
    private val clock: TodaySource,
) {
    private val alarm = ReminderAlarm(context)
    private val notifications = ReminderNotifications(context)
    private val scheduleLock = Mutex()
    private val started = AtomicBoolean(false)

    // Whether the reminder could reach the reader when the alarm was last armed or cancelled.
    @Volatile private var armedForAccess: Boolean? = null

    /**
     * Keeps the alarm in step with the setting for as long as the process lives. Called once, by
     * the application: the channel is made (so Settings can open its page before the first
     * notification), and the alarm armed now and at every change of the setting or its time.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        notifications.ensureChannel()
        Background.launch {
            preferences.settings
                .map { Inputs(it.reminderOn, it.reminderTime) }
                .distinctUntilChanged()
                .collect { guarded("Arming the reminder") { reschedule() } }
        }
    }

    /**
     * Arms, moves or cancels the alarm for what the setting says now: the reader's time, next
     * after now, in the zone in effect now. Turned off, the alarm goes, and so does a reminder
     * still in the shade: the reader said they do not want it.
     */
    suspend fun reschedule() = scheduleLock.withLock {
        val settings = preferences.current()
        val mayNotify = mayNotify()
        armedForAccess = mayNotify
        if (settings.reminderOn && mayNotify) {
            alarm.set(ReminderSchedule.next(clock.now(), clock.zone(), settings.reminderTime))
        } else {
            alarm.cancel()
        }
        if (!settings.reminderOn) notifications.cancel()
    }

    /**
     * The alarm went off, due at [dueAt]. The next one is armed first, whatever this one decides.
     * It asks only if the reminder is still on, its day is not over (Doze or a phone switched off
     * can hold an alarm back), there is a cap to add to, and no spend was added today; the caps'
     * catch-up runs first, as before every read (§4.4).
     */
    suspend fun fire(dueAt: Instant?) = guarded("The evening reminder") {
        reschedule()
        if (!preferences.current().reminderOn) return@guarded
        val now = clock.now()
        if (dueAt != null && !ReminderSchedule.stillTimely(dueAt, now, clock.zone())) return@guarded
        if (caps.currentStates().isEmpty() || caps.anySpendToday()) return@guarded
        notifications.ensureChannel()
        notifications.post(ReminderSchedule.untilEndOfDay(clock.now(), clock.zone()))
    }

    /**
     * The reminder's notifications may have been turned off or on in the system's settings, or the
     * permission given (the app coming back to the front, the system's broadcast, Settings' own
     * request): the alarm follows, if that changed anything since it was armed.
     */
    suspend fun accessMayHaveChanged() {
        if (armedForAccess != mayNotify()) guarded("Arming the reminder") { reschedule() }
    }

    /**
     * The app came to the front. The reminder in the shade has done its work: the reader is here,
     * where the question was meant to bring them. And the notifications may have changed meanwhile.
     */
    fun appOpened() {
        notifications.cancel()
        Background.launch { accessMayHaveChanged() }
    }

    private fun mayNotify(): Boolean = ReminderAccess.block(context) == ReminderBlock.NONE

    /** A failure costs one notification or one arming, logged, never the caller (a receiver, the app). */
    private suspend fun guarded(what: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed", e)
        }
    }

    /** What the alarm depends on among the settings: a change in anything else arms nothing. */
    private data class Inputs(val on: Boolean, val time: LocalTime)

    private companion object {
        const val TAG = "TettoReminder"

        /** Outlives any one caller: the setting is watched for the whole process. */
        val Background = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
