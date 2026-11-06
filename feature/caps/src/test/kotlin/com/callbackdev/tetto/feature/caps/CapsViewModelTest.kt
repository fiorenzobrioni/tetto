package com.callbackdev.tetto.feature.caps

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.DayTicks
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.testing.RealMainThread
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
 * The caps screen's ViewModel on a real Room database (PLANNING.md §11 Phase 2, acceptance): a
 * spend is two touches and the amount, and Undo puts the figure back. On real threads: the page's
 * midnight wait is real time, which a test scheduler would skip round and round.
 */
@RunWith(AndroidJUnit4::class)
class CapsViewModelTest {
    @get:Rule val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val main = RealMainThread()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: TettoDatabase
    private lateinit var caps: CapsRepository
    private lateinit var viewModel: CapsViewModel

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
        main.start()
        database = Room.inMemoryDatabaseBuilder(context, TettoDatabase::class.java).build()
        val store = PreferenceDataStoreFactory.create(scope = io) { File(folder.root, "settings.preferences_pb") }
        val preferences = UserPreferencesDataSource(store)
        caps = CapsRepository(database, preferences, clock, phone)
        viewModel = CapsViewModel(caps, SettingsRepository(preferences, caps, phone), DayTicks(context, clock), clock)
    }

    @After
    fun tearDown() {
        main.stop(viewModel)
        database.close()
        io.cancel()
    }

    private suspend fun awaitLeft(capId: Long, cents: Long) = withTimeout(TIMEOUT) {
        viewModel.state.first { it?.cap(capId)?.left == euros(0, cents) }
    }

    @Test
    fun `a spend is two touches and the amount, and Undo puts the figure back`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        awaitLeft(coffee, 2_000)

        // The first touch: the tile opens the numpad on its cap.
        viewModel.openNumpad(coffee)
        val target = checkNotNull(viewModel.numpad.value)
        assertThat(target).isEqualTo(NumpadTarget(coffee))
        // The amount, and the second touch: Add.
        viewModel.confirmNumpad(target, euros(5))
        assertThat(viewModel.numpad.value).isNull()

        val notice = withTimeout(TIMEOUT) { viewModel.notices.first() }
        assertThat(notice).isInstanceOf(CapsNotice.Added::class.java)
        assertThat((notice as CapsNotice.Added).capName).isEqualTo("Coffee")
        awaitLeft(coffee, 1_500)

        viewModel.undo(notice)
        awaitLeft(coffee, 2_000)
        assertThat(caps.openSpends(coffee)).isEmpty()
    }

    @Test
    fun `a second Add from the same numpad adds nothing`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        awaitLeft(coffee, 2_000)

        viewModel.openNumpad(coffee)
        val target = checkNotNull(viewModel.numpad.value)
        // Two touches in one frame, or Enter held on a keyboard: the numpad is still drawn.
        viewModel.confirmNumpad(target, euros(5))
        viewModel.confirmNumpad(target, euros(5))

        withTimeout(TIMEOUT) { viewModel.notices.first() }
        awaitLeft(coffee, 1_500)
        assertThat(caps.openSpends(coffee)).hasSize(1)
    }

    @Test
    fun `a refund from the cap's sheet, with Undo`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        caps.addSpend(coffee, euros(8))
        awaitLeft(coffee, 1_200)

        viewModel.openRefund(coffee)
        val target = checkNotNull(viewModel.numpad.value)
        assertThat(target.refund).isTrue()
        viewModel.confirmNumpad(target, euros(3))
        val notice = withTimeout(TIMEOUT) { viewModel.notices.first() } as CapsNotice.Added
        assertThat(notice.spend.refund).isTrue()
        awaitLeft(coffee, 1_500)

        viewModel.undo(notice)
        awaitLeft(coffee, 1_200)
    }

    @Test
    fun `a spend corrected and deleted from the cap's sheet, each with Undo`() = runBlocking<Unit> {
        val coffee = caps.addCap("Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK)
        val spend = checkNotNull(caps.addSpend(coffee, euros(5)))
        awaitLeft(coffee, 1_500)

        viewModel.openNumpad(coffee, correcting = spend)
        viewModel.confirmNumpad(checkNotNull(viewModel.numpad.value), euros(3))
        val corrected = withTimeout(TIMEOUT) { viewModel.notices.first() }
        assertThat(corrected).isEqualTo(CapsNotice.Corrected(spend.id, euros(3), euros(5)))
        awaitLeft(coffee, 1_700)

        viewModel.deleteSpend(spend.copy(amount = euros(3)))
        val deleted = withTimeout(TIMEOUT) { viewModel.notices.first() }
        awaitLeft(coffee, 2_000)
        viewModel.undo(deleted)
        awaitLeft(coffee, 1_700)
    }

    private companion object {
        const val TIMEOUT = 5_000L
    }
}
