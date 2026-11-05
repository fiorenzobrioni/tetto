package com.callbackdev.tetto.core.designsystem.format

import com.callbackdev.tetto.core.model.AppCurrency
import java.text.Collator
import java.text.Normalizer
import java.util.Currency
import java.util.Locale

/** A currency as the picker lists it: its code, its name and its symbol in the reader's language. */
data class CurrencyChoice(val code: String, val name: String, val symbol: String) {
    /**
     * Whether [query] finds it: a part of its name, accents and case aside ("dollaro" finds
     * "Dollaro statunitense", "zloty" finds "Złoty polacco"), the start of its code, or its symbol.
     */
    fun matches(query: String): Boolean {
        val wanted = query.trim()
        if (wanted.isEmpty()) return true
        val folded = fold(wanted)
        return fold(name).contains(folded) ||
            code.startsWith(wanted, ignoreCase = true) ||
            symbol.equals(wanted, ignoreCase = true)
    }
}

/**
 * The currencies a cap can count in (PLANNING.md §4.1), as the phone knows them: those some region
 * uses today, each region's own (`Currency.getInstance` of a country is the one it uses now), so
 * the list holds the lira of no one and none of the funds' accounting codes; and only those with
 * decimals of their own ([AppCurrency.ofOrNull]: not gold, not the test code).
 */
object Currencies {
    /** Every such currency, named in [locale], in its alphabetical order; [keep] is listed too, used or not. */
    fun all(locale: Locale, keep: String? = null): List<CurrencyChoice> {
        val codes = inUse + listOfNotNull(keep?.takeIf { AppCurrency.ofOrNull(it) != null })
        val collator = Collator.getInstance(locale)
        return codes.map { choice(it, locale) }.sortedWith(compareBy(collator) { it.name })
    }

    /** [code]'s name in [locale], its first letter up: "Euro", "Dollaro statunitense". */
    fun name(code: String, locale: Locale): String = choice(code, locale).name

    /** [code]'s name as [locale] writes it inside a sentence: "Japanese Yen", "yen giapponese". */
    fun inSentence(code: String, locale: Locale): String = Currency.getInstance(code).getDisplayName(locale)

    private fun choice(code: String, locale: Locale): CurrencyChoice {
        val currency = Currency.getInstance(code)
        val name = currency.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
        return CurrencyChoice(code, name, currency.getSymbol(locale))
    }

    private val inUse: Set<String> by lazy {
        Locale.getISOCountries().mapNotNullTo(sortedSetOf()) { country ->
            val region = Locale.Builder().setRegion(country).build()
            runCatching { Currency.getInstance(region)?.currencyCode }.getOrNull()
                ?.takeIf { AppCurrency.ofOrNull(it) != null }
        }
    }
}

/** Lower case, without accents or marks, so a search typed without them still finds a name. */
private fun fold(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(MARKS, "")
    .replace('ł', 'l')
    .replace('Ł', 'L')
    .lowercase(Locale.ROOT)

private val MARKS = Regex("\\p{Mn}+")
