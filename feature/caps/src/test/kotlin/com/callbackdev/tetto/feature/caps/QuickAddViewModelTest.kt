package com.callbackdev.tetto.feature.caps

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.data.widget.QuickAdd
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
import java.util.concurrent.Executors

/**
 * The numpad over the home screen (PLANNING.md §7, Taps), on a real Room database: from the home
 * screen a spend is two touches and the amount; Undo takes it back and types its amount in again.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class QuickAddViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val main = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: TettoDatabase
    private lateinit var caps: CapsRepository
    private lateinit var settings: SettingsRepository

    private val clock = object : TodaySource {
        override fun now(): Instant = Instant.parse("2026-11-05T09:00:00Z")

        override fun zone(): ZoneId = ZoneId.of("Europe/Rome")
    }

    private val phone = object : PhoneDefaults {
        override fun currency() = AppCurrency("EUR", 2)

        override fun weekStart(): DayOfWeek = DayOfWeek.MONDAY
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(main)
        database = Room.inMemoryDatabaseBuilder(context, TettoDatabase::class.java).build()
        val store = PreferenceDataStoreFactory.create(scope = io) { File(folder.root, "settings.preferences_pb") }
        val preferences = UserPreferencesDataSource(store)
        caps = CapsRepository(database, preferences, clock, phone)
        settings = SettingsRepository(preferences, caps, phone)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
        io.cancel()
        main.close()
    }

    private fun viewModel(capId: Long?) = QuickAddViewModel(
        SavedStateHandle(
            capId?.let {
                mapOf(QuickAdd.EXTRA_CAP_ID to it)
            }.orEmpty(),
        ),
        caps,
        settings,
        clock,
    )

    @Test
    fun `the amount and Add, and the cap says what is left`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        val viewModel = viewModel(coffee)
        val opened = withTimeout(TIMEOUT) { viewModel.state.first { it != null } }
        assertThat(opened?.cap?.cap?.name).isEqualTo("Coffee")

        var heard = 0
        viewModel.add(euros(5)) { heard++ }
        val added = withTimeout(TIMEOUT) { viewModel.phase.first { it is QuickAddPhase.Added } } as QuickAddPhase.Added
        assertThat(added.spend.amount).isEqualTo(euros(5))
        assertThat(heard).isEqualTo(1)
        withTimeout(TIMEOUT) { viewModel.state.first { it?.cap?.left == euros(15) } }
    }

    @Test
    fun `Undo takes the spend back and types its amount in again`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        val viewModel = viewModel(coffee)
        withTimeout(TIMEOUT) { viewModel.state.first { it != null } }
        viewModel.add(euros(50))
        withTimeout(TIMEOUT) { viewModel.phase.first { it is QuickAddPhase.Added } }
        viewModel.undo()
        val typing = withTimeout(TIMEOUT) {
            viewModel.phase.first { it is QuickAddPhase.Typing }
        } as QuickAddPhase.Typing
        assertThat(typing.initial).isEqualTo(euros(50))
        assertThat(typing.round).isEqualTo(1)
        assertThat(caps.openSpends(coffee)).isEmpty()
    }

    @Test
    fun `a cap deleted since the card was drawn is said to be gone`() = runBlocking<Unit> {
        val viewModel = viewModel(capId = 99)
        val state = withTimeout(TIMEOUT) { viewModel.state.first { it != null } }
        assertThat(state?.cap).isNull()
    }

    @Test
    fun `a door with no cap names none`() = runBlocking<Unit> {
        caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        val state = withTimeout(TIMEOUT) { viewModel(capId = null).state.first { it != null } }
        assertThat(state?.cap).isNull()
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
