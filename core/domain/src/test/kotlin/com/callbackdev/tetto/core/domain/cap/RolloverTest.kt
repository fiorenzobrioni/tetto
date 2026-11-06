package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.Money
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Closing one period (PLANNING.md §4.4; §4.7, The rollover). A €20 cap throughout. */
class RolloverTest {
    private val cap = euros(20)

    @Test
    fun `under the cap, the reserve grows`() {
        val outcome = Rollover.close(cap, reserve = euros(5), spent = euros(12))
        assertThat(outcome.reserveAfter).isEqualTo(euros(13))
        assertThat(outcome.forgiven).isEqualTo(Money.ZERO)
    }

    @Test
    fun `exactly at the cap, the reserve stays`() {
        val outcome = Rollover.close(cap, reserve = euros(5), spent = euros(20))
        assertThat(outcome.reserveAfter).isEqualTo(euros(5))
        assertThat(outcome.forgiven).isEqualTo(Money.ZERO)
    }

    @Test
    fun `over within the reserve, the reserve shrinks`() {
        val outcome = Rollover.close(cap, reserve = euros(10), spent = euros(26))
        assertThat(outcome.reserveAfter).isEqualTo(euros(4))
        assertThat(outcome.forgiven).isEqualTo(Money.ZERO)
    }

    @Test
    fun `over the cap and the reserve exactly, the reserve is spent and nothing forgiven`() {
        val outcome = Rollover.close(cap, reserve = euros(10), spent = euros(30))
        assertThat(outcome.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(outcome.forgiven).isEqualTo(Money.ZERO)
    }

    @Test
    fun `over beyond the reserve, the rest is forgiven and the reserve is zero`() {
        val outcome = Rollover.close(cap, reserve = euros(10), spent = euros(45, 50))
        assertThat(outcome.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(outcome.forgiven).isEqualTo(euros(15, 50))
    }

    @Test
    fun `the reserve stops at its limit, one period's cap`() {
        val atLimit = Rollover.close(cap, reserve = euros(20), spent = euros(3))
        assertThat(atLimit.reserveAfter).isEqualTo(euros(20))
        val nearLimit = Rollover.close(cap, reserve = euros(15), spent = euros(1))
        assertThat(nearLimit.reserveAfter).isEqualTo(euros(20))
        val nothingSpent = Rollover.close(cap, reserve = Money.ZERO, spent = Money.ZERO)
        assertThat(nothingSpent.reserveAfter).isEqualTo(euros(20))
    }

    @Test
    fun `the limit after a change of amount is the new cap`() {
        // A €100 cap with €80 in reserve, lowered to €20 mid-period: the editor holds the reserve
        // to €20 at once (CapChanges.heldReserve); a reserve read from an older store is still
        // bounded by the new cap at the close.
        val outcome = Rollover.close(cap = euros(20), reserve = euros(80), spent = euros(10))
        assertThat(outcome.reserveAfter).isEqualTo(euros(20))
        // Spending into the old reserve still draws on it before the close.
        val spentMore = Rollover.close(cap = euros(20), reserve = euros(80), spent = euros(90))
        assertThat(spentMore.reserveAfter).isEqualTo(euros(10))
    }

    @Test
    fun `cents stay exact`() {
        val outcome = Rollover.close(euros(20), reserve = euros(0, 10), spent = euros(19, 80))
        assertThat(outcome.reserveAfter).isEqualTo(euros(0, 30))
    }

    @Test
    fun `days cut short close on their share, held to the cap that runs on`() {
        // Three days of a €20 week (€8.57), €4 spent, a €10 reserve, turned into a €3 day: €14.57
        // left over, held to €3.
        val outcome = Rollover.close(cap = euros(8, 57), reserve = euros(10), spent = euros(4), limit = euros(3))
        assertThat(outcome.reserveAfter).isEqualTo(euros(3))
        assertThat(outcome.forgiven).isEqualTo(Money.ZERO)
        val over = Rollover.close(cap = euros(8, 57), reserve = euros(1), spent = euros(12), limit = euros(3))
        assertThat(over.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(over.forgiven).isEqualTo(euros(2, 43))
    }
}
