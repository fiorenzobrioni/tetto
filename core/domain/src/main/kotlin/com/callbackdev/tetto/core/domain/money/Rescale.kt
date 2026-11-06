package com.callbackdev.tetto.core.domain.money

import com.callbackdev.tetto.core.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A change of currency relabels the amounts and never converts them (PLANNING.md §4.1); where the
 * two currencies' decimals differ, the stored minor units are rescaled to keep the same figure,
 * rounded half up: euro to yen, 1250 cents become ¥13. Exact decimal arithmetic, no floating point.
 *
 * An amount above zero never becomes zero (owner, 5 Nov 2026): a €0.40 spend becomes ¥1, the
 * smallest amount the new currency has, so no spend and no cap disappears or reads as nothing.
 *
 * What the numpad typed (a cap's amount, its reserve, a spend) never grows past what it can type
 * back ([typed]): rounded half up, the largest amount with three decimals would need a tenth digit
 * with none, and the editor could not show the cap. Held there instead, a unit below the rounding,
 * for an amount nobody spends. A closed line's sums are figures, never typed: [amount] keeps them.
 */
object Rescale {
    fun amount(amount: Money, fromDigits: Int, toDigits: Int): Money {
        if (fromDigits == toDigits) return amount
        // A refund is stored below zero: rescaled as its size is, so it rounds as a spend does.
        if (amount.isNegative) return -amount(-amount, fromDigits, toDigits)
        val rescaled = BigDecimal.valueOf(amount.minor)
            .movePointLeft(fromDigits)
            .setScale(toDigits, RoundingMode.HALF_UP)
            .movePointRight(toDigits)
            .longValueExact()
        return Money(if (amount.isPositive && rescaled == 0L) 1 else rescaled)
    }

    /**
     * [amount], for an amount the numpad typed: held to what it can type back where the decimals
     * change. Where they do not, nothing is rescaled and nothing is held: the amount is as typed.
     */
    fun typed(amount: Money, fromDigits: Int, toDigits: Int): Money {
        if (fromDigits == toDigits) return amount
        if (amount.isNegative) return -typed(-amount, fromDigits, toDigits)
        return minOf(amount(amount, fromDigits, toDigits), Money(MoneyInput.largestMinor(toDigits)))
    }
}
