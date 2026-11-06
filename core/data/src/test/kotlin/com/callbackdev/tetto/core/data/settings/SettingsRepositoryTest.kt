package com.callbackdev.tetto.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.FakePhone
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.euros
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId

/**
 * The settings as the screens change them (PLANNING.md §11 Phase 3): the first run's end, and
 * every setting surviving a restart, read back by a new store over the same file.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: TettoDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var caps: CapsRepository
    private lateinit var repository: SettingsRepository

    private val clock = FakeToday(ZoneId.of("Europe/Rome"))
    private val phone = FakePhone(AppCurrency("CHF", 2))

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TettoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock.at("2026-11-05T10:00")
        open()
    }

    @After
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    /** The app's process starting: a new store over the settings file, and the repositories on it. */
    private fun open() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope) { File(folder.root, "settings.preferences_pb") }
        val preferences = UserPreferencesDataSource(store)
        caps = CapsRepository(database, preferences, clock, phone)
        repository = SettingsRepository(preferences, caps, phone)
    }

    private suspend fun restart() {
        // The old store lets go of its file only once its scope has ended.
        scope.coroutineContext.job.cancelAndJoin()
        open()
    }

    @Test
    fun `the first run ends with the currency stored, the caps made, then the flag`() = runTest {
        assertThat(repository.currency.first()).isEqualTo(AppCurrency("CHF", 2))
        repository.finishFirstRun(
            "EUR",
            listOf(
                CapDraft("Groceries", euros(300), PeriodKind.MONTH, CapIcon.GROCERIES),
                CapDraft("Coffee and bars", euros(20), PeriodKind.WEEK, CapIcon.COFFEE),
            ),
        )
        val settings = repository.settings.first()
        assertThat(settings.currency).isEqualTo("EUR")
        assertThat(settings.onboardingCompleted).isTrue()
        assertThat(caps.currentStates().map { it.cap.name }).containsExactly("Groceries", "Coffee and bars").inOrder()
    }

    @Test
    fun `started from nothing, the first run still stores the currency`() = runTest {
        repository.finishFirstRun("CHF", emptyList())
        assertThat(repository.settings.first()).isEqualTo(UserSettings(currency = "CHF", onboardingCompleted = true))
        assertThat(caps.currentStates()).isEmpty()
    }

    @Test
    fun `every setting survives a restart`() = runTest {
        repository.finishFirstRun("EUR", emptyList())
        repository.setCurrency("JPY")
        repository.setMonthStartDay(27)
        repository.setWeekStart(DayOfWeek.SUNDAY)
        repository.setTheme(ThemeMode.DARK)
        repository.setPalette(AppPalette.PAPER)
        repository.setFont(AppFont.SYSTEM)
        repository.setCapsLayout(CapsLayout.LIST)
        repository.setDynamicColor(true)
        repository.setReminder(true)
        repository.setReminderTime(LocalTime.of(21, 15))
        repository.noteNotificationsAsked()
        repository.noteCapsTipSeen()

        restart()

        assertThat(repository.settings.first()).isEqualTo(
            UserSettings(
                currency = "JPY",
                monthStartDay = 27,
                weekStart = DayOfWeek.SUNDAY,
                theme = ThemeMode.DARK,
                palette = AppPalette.PAPER,
                font = AppFont.SYSTEM,
                capsLayout = CapsLayout.LIST,
                dynamicColor = true,
                onboardingCompleted = true,
                reminderOn = true,
                reminderTime = LocalTime.of(21, 15),
                askedNotifications = true,
                capsTipSeen = true,
            ),
        )
        assertThat(repository.currency.first()).isEqualTo(AppCurrency("JPY", 0))
        assertThat(repository.weekStart.first()).isEqualTo(DayOfWeek.SUNDAY)

        repository.setWeekStart(null)
        restart()
        assertThat(repository.weekStart.first()).isEqualTo(DayOfWeek.MONDAY)
    }
}
