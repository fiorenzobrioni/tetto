package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.euros
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The numpad's amounts, one touch each (PLANNING.md §4.6; owner, 6 Nov 2026). */
class RecentAmountsTest {
    @Test
    fun `nothing spent offers nothing`() {
        assertThat(RecentAmounts.pick(emptyList())).isEmpty()
    }

    @Test
    fun `the most frequent amounts, smallest first`() {
        val newestFirst = listOf(euros(1, 20), euros(3), euros(1, 20), euros(2, 50), euros(3), euros(1, 20), euros(9))
        assertThat(RecentAmounts.pick(newestFirst)).containsExactly(euros(1, 20), euros(2, 50), euros(3)).inOrder()
    }

    @Test
    fun `a tie goes to the amount used last`() {
        // Each used once: the three newest.
        val newestFirst = listOf(euros(4), euros(1), euros(7), euros(2))
        assertThat(RecentAmounts.pick(newestFirst)).containsExactly(euros(1), euros(4), euros(7)).inOrder()
    }

    @Test
    fun `only the last twenty spends make the habits`() {
        val newestFirst = List(20) { euros(2) } + List(30) { euros(5) }
        assertThat(RecentAmounts.pick(newestFirst)).containsExactly(euros(2))
    }
}
