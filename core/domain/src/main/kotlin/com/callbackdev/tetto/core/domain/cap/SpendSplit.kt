package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Spend
import com.callbackdev.tetto.core.model.sum
import java.time.Instant

/**
 * How an open period's spends divide where a change keeps the days before [cut] as a line of
 * their own (PLANNING.md §4.5): the ids that [moving] go into the new period, a refund [divided]
 * across the cut, if one is, and what the days kept spent ([keptSpent]).
 */
data class SpendSplit(val moving: List<Long>, val divided: DividedRefund?, val keptSpent: Money)

/**
 * A refund cut in two at a change (§4.5): the spend [spendId] keeps [moving] and goes into the new
 * period, where the reader can still correct or undo it; [kept], a part of it, stays with the
 * days kept, as a spend of its own with the same instant. Both below zero, as a refund is.
 */
data class DividedRefund(val spendId: Long, val kept: Money, val moving: Money)

/**
 * The division of an open period's spends at a change (§4.5; owner, 7 Nov 2026). Each spend goes
 * by its instant: before [cut], it stays with the days kept; from it on, it moves into the new
 * period. Except where that would leave a side spending less than nothing: a refund gives back a
 * spend, and the two can fall on either side of the cut (Monday's €50, given back on Thursday).
 * Left there, the side's refunds would outweigh its spends: the days kept would count €50 never
 * given back, and the new period would start at −€50, swallowing the next €50 of spends.
 *
 * A refund gives back the spends of its own side first; only what is past them crosses. So the
 * refunds nearest the cut cross it, whole, then the last one in part, until the side is at
 * nothing: a new period below nothing gives the days kept what it owes, and days kept below
 * nothing give the new period what they owe, as far as the new period has spent. Every amount is
 * kept, the sum of both sides is the period's, and neither side is below nothing where the
 * period was not.
 */
object SpendSplits {
    fun at(spends: List<Spend>, cut: Instant): SpendSplit {
        val (before, after) = spends.partition { it.addedAt < cut }
        val keptSum = before.map { it.amount }.sum()
        val movingSum = after.map { it.amount }.sum()
        val moving = after.mapTo(mutableListOf()) { it.id }
        var divided: DividedRefund? = null
        var kept = keptSum

        if (movingSum.isNegative) {
            // The new period's refunds that came first, nearest the cut, give back the days kept's spends.
            var owed = -movingSum
            for (refund in after.filter { it.refund }.sortedWith(compareBy({ it.addedAt }, { it.id }))) {
                if (!owed.isPositive) break
                val part = minOf(refund.size, owed)
                if (part == refund.size) {
                    moving -= refund.id
                } else {
                    divided = DividedRefund(refund.id, kept = -part, moving = refund.amount + part)
                }
                kept -= part
                owed -= part
            }
        } else if (keptSum.isNegative) {
            // The days kept's last refunds, nearest the cut, give back the new period's spends,
            // as far as it has spent: never below nothing there either.
            var owed = minOf(-keptSum, movingSum)
            for (refund in before.filter { it.refund }.sortedWith(compareBy({ it.addedAt }, { it.id })).reversed()) {
                if (!owed.isPositive) break
                val part = minOf(refund.size, owed)
                moving += refund.id
                if (part != refund.size) {
                    divided = DividedRefund(refund.id, kept = refund.amount + part, moving = -part)
                }
                kept += part
                owed -= part
            }
        }
        return SpendSplit(moving, divided, maxOf(Money.ZERO, kept))
    }
}
