package com.callbackdev.tetto.feature.onboarding

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.testing.RealMainThread
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * The first run's end on a real Room database and settings file (PLANNING.md §11 Phase 3,
 * acceptance): it ends on a caps screen shaped by the reader's picks.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val main = RealMainThread()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: TettoDatabase
    private lateinit var caps: CapsRepository
    private lateinit var settings: SettingsRepository
    private lateinit var viewModel: OnboardingViewModel

    private val clock = object : TodaySource {
        override fun now(): Instant = Instant.parse("2026-11-05T09:00:00Z")

        override fun zone(): ZoneId = ZoneId.of("Europe/Rome")
    }

    // A phone in Switzerland: the run starts in francs.
    private val phone = object : PhoneDefaults {
        override fun currency() = AppCurrency("CHF", 2)

        override fun weekStart(): DayOfWeek = DayOfWeek.MONDAY
    }

    @Before
    fun setUp() {
        main.start()
        database = Room.inMemoryDatabaseBuilder(context, TettoDatabase::class.java).build()
        val store = PreferenceDataStoreFactory.create(scope = io) { File(folder.root, "settings.preferences_pb") }
        val preferences = UserPreferencesDataSource(store)
        caps = CapsRepository(database, preferences, clock, phone)
        settings = SettingsRepository(preferences, caps, phone)
        viewModel = OnboardingViewModel(settings)
    }

    @After
    fun tearDown() {
        main.stop(viewModel)
        database.close()
        io.cancel()
    }

    private suspend fun awaitFinished() = withTimeout(TIMEOUT) { settings.settings.first { it.onboardingCompleted } }

    @Test
    fun `the run starts in the phone's currency`() = runBlocking<Unit> {
        val currency = withTimeout(TIMEOUT) { viewModel.currency.filterNotNull().first() }
        assertThat(currency).isEqualTo(AppCurrency("CHF", 2))
    }

    @Test
    fun `a first run ends on a caps screen shaped by the reader's picks`() = runBlocking<Unit> {
        viewModel.finish(
            "EUR",
            listOf(
                CapDraft("Groceries", Money(30_000), PeriodKind.MONTH, CapIcon.GROCERIES),
                CapDraft("Coffee and bars", Money(2_000), PeriodKind.WEEK, CapIcon.COFFEE),
            ),
        )
        val stored = awaitFinished()
        assertThat(stored.currency).isEqualTo("EUR")
        val states = caps.currentStates()
        assertThat(states.map { it.cap.name }).containsExactly("Groceries", "Coffee and bars").inOrder()
        assertThat(states.map { it.cap.kind }).containsExactly(PeriodKind.MONTH, PeriodKind.WEEK).inOrder()
        assertThat(states.map { it.left }).containsExactly(Money(30_000), Money(2_000)).inOrder()
        assertThat(viewModel.isFinishing.value).isTrue()
    }

    @Test
    fun `a second touch on Done never makes the caps twice`() = runBlocking<Unit> {
        val drafts = listOf(CapDraft("Fun", Money(5_000), PeriodKind.MONTH, CapIcon.FUN))
        viewModel.finish("EUR", drafts)
        viewModel.finish("EUR", drafts)
        awaitFinished()
        assertThat(caps.currentStates()).hasSize(1)
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
