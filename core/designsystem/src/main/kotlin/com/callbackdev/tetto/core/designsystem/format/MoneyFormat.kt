package com.callbackdev.tetto.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Money
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Amounts as the reader's language writes them, in the app's currency (PLANNING.md §4.1): the
 * only place an amount becomes text. From whole minor units through `BigDecimal`, never through a
 * floating-point number; the symbol, its side, its spacing and the separators are the locale's.
 *
 * Two lengths, as VISION writes them: a cap's figures drop zero cents ("€12 left", "of €200"),
 * while a spend is said in full ("€5.00 added to Coffee", "Add €5.00").
 */
class MoneyFormat(val currency: AppCurrency, val locale: Locale) {
    private val full: DecimalFormat = currencyFormat(currency.fractionDigits)
    private val whole: DecimalFormat = currencyFormat(0)
    private val symbols: DecimalFormatSymbols = full.decimalFormatSymbols

    /** The locale's decimal separator for money: the numpad's separator key. */
    val decimalSeparator: Char get() = symbols.monetaryDecimalSeparator

    /** What the locale writes before an amount ("€" in English, nothing in Italian). */
    val prefix: String get() = full.positivePrefix

    /** What the locale writes after an amount (nothing in English, " €" in Italian). */
    val suffix: String get() = full.positiveSuffix

    /** In full, with the currency's decimals: a spend. */
    fun full(amount: Money): String = full.format(decimal(amount))

    /** Zero cents dropped, other cents kept: a cap's figures. */
    fun short(amount: Money): String =
        if (amount.minor % currency.unit.minor == 0L) whole.format(decimal(amount)) else full(amount)

    /**
     * What the numpad shows while the reader types: the locale's prefix and suffix around the
     * digits typed, the whole part grouped, the decimals as typed so far ("€1,250.5").
     */
    fun typed(wholeDigits: String, fractionDigits: String?): String {
        val grouped = NumberFormat.getIntegerInstance(locale).format(wholeDigits.ifEmpty { "0" }.toLong())
        val fraction = fractionDigits?.let { decimalSeparator + localDigits(it) }.orEmpty()
        return full.positivePrefix + grouped + fraction + full.positiveSuffix
    }

    private fun decimal(amount: Money): BigDecimal =
        BigDecimal.valueOf(amount.minor).movePointLeft(currency.fractionDigits)

    private fun localDigits(ascii: String): String {
        val zero = symbols.zeroDigit
        return ascii.map { if (it in '0'..'9') zero + (it - '0') else it }.joinToString("")
    }

    private fun currencyFormat(digits: Int): DecimalFormat =
        (NumberFormat.getCurrencyInstance(locale) as DecimalFormat).apply {
            currency = Currency.getInstance(this@MoneyFormat.currency.code)
            minimumFractionDigits = digits
            maximumFractionDigits = digits
        }
}

/** The format for [currency] in the language the screen is drawn in. */
@Composable
fun rememberMoneyFormat(currency: AppCurrency): MoneyFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(currency, locale) { MoneyFormat(currency, locale) }
}
