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
 */
object Rescale {
    fun amount(amount: Money, fromDigits: Int, toDigits: Int): Money {
        if (fromDigits == toDigits) return amount
        val rescaled = BigDecimal.valueOf(amount.minor)
            .movePointLeft(fromDigits)
            .setScale(toDigits, RoundingMode.HALF_UP)
            .movePointRight(toDigits)
            .longValueExact()
        return Money(if (amount.isPositive && rescaled == 0L) 1 else rescaled)
    }
}
