package com.callbackdev.tetto.core.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager
import java.io.File
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The evening reminder (PLANNING.md §8, §11 Phase 4): its one alarm, armed only while it is on and
 * can show, at the reader's time in the zone in effect; and its firing, which asks only on a day
 * with caps and no spend added. Unless a test moves it, the clock stands at Thursday 5 Nov 2026,
 * 10:00 in Rome.
 */
@RunWith(AndroidJUnit4::class)
class EveningReminderTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val clock = Clock(ZoneId.of("Europe/Rome"))
    private lateinit var database: TettoDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var caps: CapsRepository
    private lateinit var reminder: EveningReminder

    private val alarms: ShadowAlarmManager get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val shade get() = shadowOf(context.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, TettoDatabase::class.java).allowMainThreadQueries().build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        preferences = UserPreferencesDataSource(
            PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") },
        )
        caps = CapsRepository(database, preferences, clock, Euro)
        reminder = EveningReminder(context, preferences, caps, clock)
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        clock.at("2026-11-05T10:00")
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    private suspend fun turnOn(time: LocalTime = LocalTime.of(20, 30)) {
        preferences.update { it.copy(reminderOn = true, reminderTime = time) }
    }

    private fun rome(local: String): Long =
        LocalDateTime.parse(local).atZone(ZoneId.of("Europe/Rome")).toInstant().toEpochMilli()

    private fun dueAt(local: String): Instant = Instant.ofEpochMilli(rome(local))

    private fun theReminder() = shade.getNotification(ReminderNotifications.NOTIFICATION_ID)

    @Test
    fun `off, no alarm is armed`() = runTest {
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun `on, one inexact alarm wakes the phone at the reader's time, let through Doze`() = runTest {
        turnOn()
        reminder.reschedule()
        val alarm = alarms.scheduledAlarms.single()
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.triggerAtTime).isEqualTo(rome("2026-11-05T20:30"))
        assertThat(alarm.allowWhileIdle).isTrue()
    }

    @Test
    fun `after the time, tomorrow's, and armed again it replaces the one before`() = runTest {
        turnOn(LocalTime.of(9, 0))
        reminder.reschedule()
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(rome("2026-11-06T09:00"))
    }

    @Test
    fun `a new zone puts the alarm at its own evening`() = runTest {
        turnOn()
        reminder.reschedule()
        clock.zone = ZoneId.of("Europe/Lisbon")
        reminder.reschedule()
        val lisbon = LocalDateTime.parse("2026-11-05T20:30").atZone(ZoneId.of("Europe/Lisbon")).toInstant()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(lisbon.toEpochMilli())
    }

    @Test
    fun `while Tetto may not notify no alarm wakes the phone, and its return arms one`() = runTest {
        turnOn()
        shade.setNotificationsEnabled(false)
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()

        shade.setNotificationsEnabled(true)
        reminder.accessMayHaveChanged()
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `the reminder's channel turned off arms nothing`() = runTest {
        turnOn()
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                ReminderNotifications.CHANNEL_ID,
                "Evening reminder",
                NotificationManager.IMPORTANCE_NONE,
            ),
        )
        reminder.reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(ReminderAccess.block(context)).isEqualTo(ReminderBlock.CHANNEL)
    }

    @Test
    fun `a day with caps and no spend asks, until the day ends, and arms tomorrow's`() = runTest {
        turnOn()
        caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T20:31")

        reminder.fire(dueAt("2026-11-05T20:30"))

        val notification = theReminder()
        assertThat(notification).isNotNull()
        assertThat(notification.extras.getString(NotificationCompat.EXTRA_TITLE)).isEqualTo("Anything to add today?")
        assertThat(notification.channelId).isEqualTo(ReminderNotifications.CHANNEL_ID)
        assertThat(notification.category).isEqualTo(NotificationCompat.CATEGORY_REMINDER)
        assertThat(notification.timeoutAfter).isEqualTo(Duration.ofMinutes(3 * 60 + 29).toMillis())
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(rome("2026-11-06T20:30"))
    }

    @Test
    fun `a day a spend was added stays quiet`() = runTest {
        turnOn()
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T08:15")
        caps.addSpend(coffee, Money(130))
        clock.at("2026-11-05T20:30")

        reminder.fire(dueAt("2026-11-05T20:30"))

        assertThat(theReminder()).isNull()
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `yesterday's spend does not count for today`() = runTest {
        turnOn()
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-04T23:50")
        caps.addSpend(coffee, Money(130))
        clock.at("2026-11-05T20:30")

        reminder.fire(dueAt("2026-11-05T20:30"))

        assertThat(theReminder()).isNotNull()
    }

    @Test
    fun `without a cap there is nothing to ask`() = runTest {
        turnOn()
        clock.at("2026-11-05T20:30")
        reminder.fire(dueAt("2026-11-05T20:30"))
        assertThat(theReminder()).isNull()
    }

    @Test
    fun `an alarm held back to the next day stays quiet`() = runTest {
        turnOn()
        caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-06T07:00")
        reminder.fire(dueAt("2026-11-05T20:30"))
        assertThat(theReminder()).isNull()
        assertThat(alarms.scheduledAlarms.single().triggerAtTime).isEqualTo(rome("2026-11-06T20:30"))
    }

    @Test
    fun `the firing closes the periods that ended before it asks`() = runTest {
        turnOn()
        clock.at("2026-10-26T09:00")
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T20:30")
        reminder.fire(dueAt("2026-11-05T20:30"))
        assertThat(database.capsDao().closedPeriods(coffee)).hasSize(1)
    }

    @Test
    fun `turned off meanwhile, the alarm says nothing and arms nothing`() = runTest {
        caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T20:30")
        reminder.fire(dueAt("2026-11-05T20:30"))
        assertThat(theReminder()).isNull()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun `turning it off takes back the alarm and a reminder still in the shade`() = runTest {
        turnOn()
        caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T20:30")
        reminder.fire(dueAt("2026-11-05T20:30"))
        assertThat(theReminder()).isNotNull()

        preferences.update { it.copy(reminderOn = false) }
        reminder.reschedule()

        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(theReminder()).isNull()
    }

    @Test
    fun `the app opened takes the reminder back, since it has brought the reader there`() = runTest {
        turnOn()
        caps.addCap("Coffee", CapIcon.COFFEE, Money(2000), PeriodKind.WEEK)
        clock.at("2026-11-05T20:30")
        reminder.fire(dueAt("2026-11-05T20:30"))
        reminder.appOpened()
        assertThat(theReminder()).isNull()
    }

    @Test
    fun `started, the alarm follows the setting and its time by itself`() = runBlocking {
        reminder.start()
        assertThat(shade.notificationChannels.map { it.id }).contains(ReminderNotifications.CHANNEL_ID)

        turnOn(LocalTime.of(21, 15))
        awaitAlarm { it == rome("2026-11-05T21:15") }
        preferences.update { it.copy(reminderTime = LocalTime.of(19, 0)) }
        awaitAlarm { it == rome("2026-11-05T19:00") }
        preferences.update { it.copy(reminderOn = false) }
        awaitAlarm { it == null }
    }

    /** Waits, in real time, for the one alarm (or none) [expected] describes. */
    private suspend fun awaitAlarm(expected: (Long?) -> Boolean) {
        withTimeout(5_000) {
            while (!expected(alarms.scheduledAlarms.singleOrNull()?.triggerAtTime)) delay(10)
        }
    }

    /** A clock the test sets: an instant and the zone in effect, both movable. */
    private class Clock(@Volatile var zone: ZoneId) : TodaySource {
        @Volatile var instant: Instant = Instant.EPOCH

        override fun now(): Instant = instant

        override fun zone(): ZoneId = zone

        fun at(local: String) {
            instant = LocalDateTime.parse(local).atZone(zone).toInstant()
        }
    }

    private object Euro : PhoneDefaults {
        override fun currency(): AppCurrency = AppCurrency("EUR", 2)

        override fun weekStart(): DayOfWeek = DayOfWeek.MONDAY
    }
}
