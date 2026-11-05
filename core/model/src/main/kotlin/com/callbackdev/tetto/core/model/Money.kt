package com.callbackdev.tetto.core.model

import java.util.Currency

/**
 * An amount in whole minor units of the app's one currency: cents of the euro, pence, none for
 * the yen (PLANNING.md §4.1). Never a floating-point number, so a sum of small spends is exact to
 * the cent. Arithmetic fails loudly on overflow rather than wrapping: nine digits and three
 * decimals stay far inside a `Long`, so an overflow is a bug, never a reader's amount.
 */
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(Math.addExact(minor, other.minor))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(minor, other.minor))

    operator fun unaryMinus(): Money = Money(Math.negateExact(minor))

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    val isNegative: Boolean get() = minor < 0

    val isPositive: Boolean get() = minor > 0

    override fun toString(): String = "Money($minor)"

    companion object {
        val ZERO = Money(0)
    }
}

fun Iterable<Money>.sum(): Money = fold(Money.ZERO) { total, money -> total + money }

/**
 * The app's one currency (PLANNING.md §4.1): its ISO 4217 code and how many minor units make a
 * whole one. The decimals are the currency's own, never the reader's: none for the yen, three for
 * the Kuwaiti dinar.
 */
data class AppCurrency(val code: String, val fractionDigits: Int) {
    init {
        require(code.length == 3) { "Not an ISO 4217 code: $code" }
        require(fractionDigits in 0..MAX_FRACTION_DIGITS) { "Unsupported decimals: $fractionDigits" }
    }

    /** One whole unit of the currency, in minor units: 100 cents, 1 yen, 1000 fils. */
    val unit: Money get() = Money(POWERS_OF_TEN[fractionDigits])

    companion object {
        /** Four covers every currency Java knows with decimals (the Chilean UF has four). */
        const val MAX_FRACTION_DIGITS = 4

        private val POWERS_OF_TEN = longArrayOf(1, 10, 100, 1_000, 10_000)

        /**
         * The currency [code] names, or null when Java does not know it or it has no decimals
         * of its own (gold, the test code `XXX`): money a cap cannot be counted in.
         */
        fun ofOrNull(code: String): AppCurrency? {
            val currency = runCatching { Currency.getInstance(code) }.getOrNull() ?: return null
            val digits = currency.defaultFractionDigits
            if (digits !in 0..MAX_FRACTION_DIGITS) return null
            return AppCurrency(currency.currencyCode, digits)
        }
    }
}
