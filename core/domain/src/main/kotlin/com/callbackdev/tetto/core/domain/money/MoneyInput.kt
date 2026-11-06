package com.callbackdev.tetto.core.domain.money

import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Money

/**
 * The numpad's buffer (PLANNING.md §4.1): digits and at most one separator, at most the
 * currency's decimals after it, at most [MAX_WHOLE_DIGITS] before it. Parsed to minor units digit
 * by digit, never through a floating-point number. Immutable: each key gives a new buffer, and a
 * key the buffer refuses gives back the same one ([accepts] tells which, for the haptic reject).
 *
 * The separator is a key, not a glyph: the screen draws the locale's, and [keyFor] reads a
 * hardware keyboard's.
 *
 * @property whole the digits before the separator, without leading zeros; "0" before a fraction.
 * @property fraction the digits after the separator, or null while no separator was typed.
 */
class MoneyInput private constructor(val currency: AppCurrency, val whole: String, val fraction: String?) {
    sealed interface Key {
        data class Digit(val value: Int) : Key {
            init {
                require(value in 0..9)
            }
        }

        data object Separator : Key

        data object Delete : Key

        data object Clear : Key
    }

    /** Nothing typed: the caret alone. */
    val isEmpty: Boolean get() = whole.isEmpty() && fraction == null

    /** Whether the currency has a separator key at all: none for the yen. */
    val hasSeparator: Boolean get() = currency.fractionDigits > 0

    /** The amount typed, exact, in minor units. */
    val amount: Money
        get() {
            val digits = currency.fractionDigits
            val wholePart = whole.ifEmpty { "0" }.toLong()
            val fractionPart = (fraction ?: "").padEnd(digits, '0').ifEmpty { "0" }.toLong()
            return Money(wholePart * currency.unit.minor + fractionPart)
        }

    /** A zero amount cannot be added: the Add button waits for one above zero. */
    val canAdd: Boolean get() = amount.isPositive

    fun accepts(key: Key): Boolean = press(key) !== this

    fun press(key: Key): MoneyInput = when (key) {
        is Key.Digit -> digit(key.value)
        Key.Separator -> separator()
        Key.Delete -> delete()
        Key.Clear -> if (isEmpty) this else MoneyInput(currency, "", null)
    }

    private fun digit(value: Int): MoneyInput {
        if (fraction != null) {
            return if (fraction.length < currency.fractionDigits) copy(fraction = fraction + value) else this
        }
        return when {
            // A leading zero is replaced, never kept: "05" is 5.
            whole == "0" -> if (value == 0) this else copy(whole = value.toString())

            whole.length >= MAX_WHOLE_DIGITS -> this

            else -> copy(whole = whole + value)
        }
    }

    private fun separator(): MoneyInput = if (!hasSeparator || fraction != null) {
        this
    } else {
        copy(whole = whole.ifEmpty { "0" }, fraction = "")
    }

    private fun delete(): MoneyInput = when {
        fraction == null -> if (whole.isEmpty()) this else copy(whole = whole.dropLast(1))
        fraction.isNotEmpty() -> copy(fraction = fraction.dropLast(1))
        else -> copy(fraction = null)
    }

    private fun copy(whole: String = this.whole, fraction: String? = this.fraction) =
        MoneyInput(currency, whole, fraction)

    override fun equals(other: Any?): Boolean =
        other is MoneyInput && other.currency == currency && other.whole == whole && other.fraction == fraction

    override fun hashCode(): Int = (currency.hashCode() * 31 + whole.hashCode()) * 31 + fraction.hashCode()

    override fun toString(): String = "MoneyInput(${currency.code} $whole${fraction?.let { ".$it" } ?: ""})"

    companion object {
        /** Up to 999,999,999 whole units: far inside a `Long`, sums included (§4.1). */
        const val MAX_WHOLE_DIGITS = 9

        fun empty(currency: AppCurrency): MoneyInput = MoneyInput(currency, "", null)

        /**
         * The buffer that types [amount] back (a spend to correct, a spend undone over the home
         * screen, a cap's amount in its editor): whole units alone where the decimals are zero
         * ("5" for 5.00), else every decimal the currency has ("5,50" for 5.50, "3,20" for 3.20),
         * so the figure reads as the cap's sheet lists the spend (owner, 6 Nov 2026).
         */
        fun of(amount: Money, currency: AppCurrency): MoneyInput {
            require(!amount.isNegative) { "A negative amount cannot be typed" }
            val unit = currency.unit.minor
            val whole = (amount.minor / unit).toString()
            val rest = amount.minor % unit
            val fraction = if (rest == 0L) null else rest.toString().padStart(currency.fractionDigits, '0')
            require(whole.length <= MAX_WHOLE_DIGITS) { "More than nine digits cannot be typed" }
            return MoneyInput(currency, whole, fraction)
        }

        /**
         * The key a hardware keyboard's [char] stands for: a digit of any script, or a separator.
         * Besides the locale's own [decimalSeparator], a dot and a comma are both taken for it:
         * the buffer never holds a grouping separator, so neither can mean anything else, and a
         * reader typing "1.50" on an Italian phone means one and a half.
         */
        fun keyFor(char: Char, decimalSeparator: Char): Key? {
            val digit = Character.digit(char, 10)
            return when {
                digit >= 0 -> Key.Digit(digit)
                char == decimalSeparator || char == '.' || char == ',' -> Key.Separator
                else -> null
            }
        }
    }
}
