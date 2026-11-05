package com.callbackdev.tetto.core.data.time

import com.callbackdev.tetto.core.model.AppCurrency
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale

/**
 * What the phone says where the reader has not chosen (PLANNING.md §4.1, §4.2): the currency of
 * its region, and the first day of its locale's week. Read at every call: a change of the phone's
 * language or region moves them.
 */
interface PhoneDefaults {
    fun currency(): AppCurrency

    fun weekStart(): DayOfWeek

    companion object {
        /**
         * The currency where the region names none (a language set without a country, the
         * Antarctic): the euro, until the first run's choice (owner, 5 Nov 2026).
         */
        val FALLBACK_CURRENCY = AppCurrency("EUR", 2)

        val System: PhoneDefaults = object : PhoneDefaults {
            override fun currency(): AppCurrency {
                val locale = Locale.getDefault(Locale.Category.FORMAT)
                val code = runCatching { Currency.getInstance(locale)?.currencyCode }.getOrNull()
                return code?.let(AppCurrency::ofOrNull) ?: FALLBACK_CURRENCY
            }

            override fun weekStart(): DayOfWeek =
                WeekFields.of(Locale.getDefault(Locale.Category.FORMAT)).firstDayOfWeek
        }
    }
}
