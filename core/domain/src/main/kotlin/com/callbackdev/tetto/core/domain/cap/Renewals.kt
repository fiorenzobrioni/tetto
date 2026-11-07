package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.Renewal
import com.callbackdev.tetto.core.model.Renewal.Kind

/**
 * What the day a period renews says of the one that ended (owner, 6 Nov 2026), from its closed
 * line. Forgiveness first, the moment Tetto exists for; then the reserve that covered an
 * overspend; then what went to the reserve; then a reserve already full; else right on the cap.
 * A run of empty periods after an absence ends as the run did: its reserve grew, or was full.
 *
 * Each is read from what the period spent against its cap, never from the reserve alone (owner,
 * 7 Nov 2026): a change of kind closes the days kept with the reserve held to a cap that may be
 * lower (§4.5), so a reserve that went down was not always spent. Covered is only ever the
 * overspend the reserve paid, `spent − cap` (past it, [Kind.FORGIVEN] says the rest); a reserve
 * held down under the cap was held to its new limit, and that limit is full.
 */
object Renewals {
    fun of(line: ClosedPeriod): Renewal = when {
        line.forgiven.isPositive -> Renewal(Kind.FORGIVEN, line.forgiven)

        line.spent > line.cap -> Renewal(Kind.COVERED, line.spent - line.cap)

        line.reserveAfter > line.reserveBefore -> Renewal(Kind.SAVED, line.reserveAfter - line.reserveBefore)

        // Under the cap (or on it) with a reserve no higher: it was at its limit, or held down to
        // a lower one, and is full either way.
        line.spent < line.cap || line.periods > 1 || line.reserveAfter < line.reserveBefore ->
            Renewal(Kind.RESERVE_FULL, line.reserveAfter)

        else -> Renewal(Kind.EVEN, line.cap)
    }
}
