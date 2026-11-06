package com.callbackdev.tetto.widget

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test

/** Whatever asks for a redraw stays until the cards have drawn it, and no longer than it said. */
class WidgetRefreshTest {
    @After
    fun forgetTheCards() = WidgetRefresh.forget(intArrayOf(CARD, OTHER))

    @Test
    fun `a redraw is waited for until every card has drawn its revision`() = runTest {
        val revision = WidgetRefresh.invalidate()
        val waiting = async { WidgetRefresh.awaitDrawn(listOf(CARD, OTHER), revision, timeoutMillis = 5_000) }
        WidgetRefresh.drawn(CARD, revision)
        // An older revision does not count: that card has not drawn the change yet.
        WidgetRefresh.drawn(OTHER, revision - 1)
        testScheduler.advanceTimeBy(1_000)
        assertThat(waiting.isCompleted).isFalse()
        WidgetRefresh.drawn(OTHER, revision)
        assertThat(waiting.await()).isTrue()
    }

    @Test
    fun `a card that never draws is waited for no longer than the timeout`() = runTest {
        val revision = WidgetRefresh.invalidate()
        assertThat(WidgetRefresh.awaitDrawn(listOf(CARD), revision, timeoutMillis = 2_000)).isFalse()
    }

    private companion object {
        const val CARD = 9_001
        const val OTHER = 9_002
    }
}
