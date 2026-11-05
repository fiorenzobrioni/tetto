package com.callbackdev.tetto.core.data.time

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.callbackdev.tetto.core.data.FakeToday
import com.callbackdev.tetto.core.data.date
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import java.time.ZoneId

/** The days a visible page lives through (PLANNING.md §4.4): today, then again when the clock or the zone moves. */
@RunWith(AndroidJUnit4::class)
class DayTicksTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = FakeToday(ZoneId.of("Europe/Rome")).apply { at("2026-11-05T15:00") }

    @Test
    fun `today at once, and again after a change of zone, with nothing armed once collection stops`() = runTest {
        // On a real dispatcher: the wait for midnight is real time, which the test scheduler would skip.
        DayTicks(context, clock).days().flowOn(Dispatchers.IO).test {
            assertThat(awaitItem()).isEqualTo(date(2026, 11, 5))
            assertThat(clockReceivers()).hasSize(1)

            // Flown to Auckland: there it is already the 6th.
            clock.zone = ZoneId.of("Pacific/Auckland")
            context.sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
            ShadowLooper.idleMainLooper()
            assertThat(awaitItem()).isEqualTo(date(2026, 11, 6))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(clockReceivers()).isEmpty()
    }

    /** The receivers listening for the clock: the ticker's, and none other's in this test. */
    private fun clockReceivers() = shadowOf(context as Application).registeredReceivers
        .filter { it.intentFilter.hasAction(Intent.ACTION_TIMEZONE_CHANGED) }
}
