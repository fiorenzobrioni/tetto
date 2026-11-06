package com.callbackdev.tetto.widget

import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetLook
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

/** A card must always reach `provideContent`: Glance shows its spinner until it does. */
class GuardedLoadTest {
    private val sample =
        WidgetModel(WidgetLook(), UserSettings(), AppCurrency("EUR", 2), LocalDate.of(2026, 11, 11), emptyList())

    @Test
    fun `a read that works is the card`() = runTest {
        assertThat(guardedLoad(1_000, "test") { sample }).isEqualTo(sample)
    }

    @Test
    fun `a read that fails is a card that says so`() = runTest {
        assertThat(guardedLoad(1_000, "test") { error("the database is gone") }.unavailable).isTrue()
    }

    @Test
    fun `a read that never ends is a card that says so, in time`() = runTest {
        assertThat(guardedLoad(1_000, "test") { awaitCancellation() }.unavailable).isTrue()
    }

    @Test
    fun `cancellation is not swallowed`() {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { guardedLoad(1_000, "test") { throw CancellationException("gone") } }
        }
    }
}
