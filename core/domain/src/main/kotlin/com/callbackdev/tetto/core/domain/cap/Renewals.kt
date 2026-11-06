package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.Renewal
import com.callbackdev.tetto.core.model.Renewal.Kind

/**
 * What the day a period renews says of the one that ended (owner, 6 Nov 2026), from its closed
 * line. Forgiveness first, the moment Tetto exists for; then the reserve that covered an
 * overspend; then what went to the reserve; then a reserve already full; else right on the cap.
 * A run of empty periods after an absence ends as the run did: its reserve grew, or was full.
 */
object Renewals {
    fun of(line: ClosedPeriod): Renewal = when {
        line.forgiven.isPositive -> Renewal(Kind.FORGIVEN, line.forgiven)
        line.reserveAfter < line.reserveBefore -> Renewal(Kind.COVERED, line.reserveBefore - line.reserveAfter)
        line.reserveAfter > line.reserveBefore -> Renewal(Kind.SAVED, line.reserveAfter - line.reserveBefore)
        line.spent < line.cap || line.periods > 1 -> Renewal(Kind.RESERVE_FULL, line.reserveAfter)
        else -> Renewal(Kind.EVEN, line.cap)
    }
}
