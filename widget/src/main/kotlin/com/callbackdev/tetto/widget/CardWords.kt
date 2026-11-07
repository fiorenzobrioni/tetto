package com.callbackdev.tetto.widget

import android.content.Context
import android.text.format.DateFormat
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.domain.period.Days
import com.callbackdev.tetto.core.domain.widget.CapNote
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.NearDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The words the two cards print, the caps screen's words in a card's register (PLANNING.md §4.6):
 * "€12 left", "of €20 · until Sunday", "Coffee · until Sunday", "about €4 a day". The engine never
 * writes a word; the dates are written with the locale's own patterns, the amounts by
 * [MoneyFormat], from the language the card is drawn in.
 *
 * With the amounts [hidden] (owner, 6 Nov 2026), no figure is written anywhere, TalkBack's
 * description included: where a figure stood, the word for where the cap is ("Under its cap",
 * "Ahead of the days", "Over its cap"); the names, the dates and the words stay, so the forms are
 * measured from what is really drawn and nothing is blurred.
 *
 * A one-off cap (owner, 6 Nov 2026) never starts again: over, it says until when it runs; ended,
 * the card says so, and what its dates spent.
 */
internal class CardWords(
    private val context: Context,
    currency: AppCurrency,
    private val today: LocalDate,
    val hidden: Boolean = false,
) {
    private val res = context.resources
    private val locale = context.widgetLocale()
    val format = MoneyFormat(currency, locale)

    /** "€12 left", "€30 over": what is left, in words; hidden, where the cap is ([status]). */
    fun figure(state: CapState): String = when {
        state.ended -> res.getString(R.string.widget_ended)
        hidden -> status(state)
        state.over -> res.getString(R.string.widget_over, format.short(state.overBy))
        else -> res.getString(R.string.widget_left, format.short(state.left))
    }

    /** "€12", "€30": the figure alone, where the word stands on a line of its own ([word]). */
    fun amount(state: CapState): String = when {
        state.ended -> res.getString(R.string.widget_ended)
        hidden -> status(state, short = true)
        else -> format.short(if (state.over) state.overBy else state.left)
    }

    /**
     * Where a cap is, in the words a hidden card says instead of its figure: under its cap, ahead
     * of the days, over its cap; [short] for a cell's or a slot's narrow room.
     */
    fun status(state: CapState, short: Boolean = false): String = res.getString(
        when {
            state.over -> if (short) R.string.widget_hidden_over_short else R.string.widget_hidden_over
            state.aheadOfDays -> if (short) R.string.widget_hidden_ahead_short else R.string.widget_hidden_ahead
            else -> if (short) R.string.widget_hidden_under_short else R.string.widget_hidden_under
        },
    )

    /** "left", "over". */
    fun word(state: CapState): String =
        res.getString(if (state.over) R.string.widget_word_over else R.string.widget_word_left)

    /** Whether the language puts the word over the figure ("ancora / 12 €") or under it ("€12 / left"). */
    val wordAbove: Boolean get() = res.getBoolean(R.bool.widget_word_above)

    /**
     * "of €20 · until Sunday"; over, "of €100 · starts again on Monday"; ended, "€720 of €800 · on
     * 20 Nov". Hidden, the day alone.
     */
    fun detail(state: CapState): String = when {
        hidden -> day(state)

        state.ended -> res.getString(
            R.string.widget_ended_detail,
            format.short(state.spent),
            format.short(state.amount),
            day(state),
        )

        startsAgain(state) -> res.getString(
            R.string.widget_detail_over,
            format.short(state.amount),
            again(state.startsAgain),
        )

        else -> res.getString(R.string.widget_detail, format.short(state.amount), until(state.lastDay))
    }

    /** "Coffee · until Sunday"; over, "Coffee · starts again on Monday". */
    fun line(state: CapState): String = if (startsAgain(state)) {
        res.getString(R.string.widget_line_over, state.cap.name, again(state.startsAgain))
    } else {
        res.getString(R.string.widget_line, state.cap.name, day(state))
    }

    /** Over a cap that renews: the day it starts again is what to say. A one-off says its last day. */
    private fun startsAgain(state: CapState): Boolean = state.over && state.renews && !state.ended

    /**
     * A tall card's last words, fullest first: on one line, broken at the separator, the day alone
     * ("of €20 · until Sunday", "of €20" over "until Sunday", "until Sunday"); with the name in them
     * ([withName]: "Coffee · until Sunday", "Coffee" over "until Sunday", "Coffee"), where it has no
     * place beside the ring.
     */
    fun lastWords(state: CapState, withName: Boolean): List<String> {
        val day = day(state)
        return when {
            withName -> listOf(line(state), state.cap.name + "\n" + day, state.cap.name)

            // No cap's figure to break off: the day is all there is.
            hidden -> listOf(day)

            else -> listOf(detail(state), detailHead(state) + "\n" + day, day)
        }
    }

    /** The last words' first half, broken off the day: "of €20"; ended, "€720 of €800". */
    private fun detailHead(state: CapState): String = if (state.ended) {
        res.getString(R.string.widget_ended_of, format.short(state.spent), format.short(state.amount))
    } else {
        res.getString(R.string.widget_of, format.short(state.amount))
    }

    /** A row's second line, fullest first: "Coffee · until Sunday", "until Sunday", "Coffee". */
    fun rowLines(state: CapState): List<String> = listOf(line(state), day(state), state.cap.name)

    /**
     * The period's last day, or, over, when the cap starts again: "until Sunday", "starts again on
     * Monday"; an ended one-off's last day, "ended on 20 Nov".
     */
    fun day(state: CapState): String = when {
        state.ended -> res.getString(R.string.widget_ended_on, dateWords(state.lastDay))
        startsAgain(state) -> res.getString(R.string.widget_starts_again, again(state.startsAgain))
        else -> until(state.lastDay)
    }

    /**
     * The pace, fullest first ("about €4 a day", "€4 a day"; under a unit, "less than €1 a day",
     * "under €1 a day"); none where the cap has none, or hidden.
     */
    fun paceForms(state: CapState): List<String> = state.pace?.takeUnless { hidden }?.let {
        if (it.lessThan) {
            listOf(
                res.getString(R.string.widget_pace_less, format.short(it.perDay)),
                res.getString(R.string.widget_pace_less_short, format.short(it.perDay)),
            )
        } else {
            listOf(
                res.getString(R.string.widget_pace, format.short(it.perDay)),
                res.getString(R.string.widget_pace_short, format.short(it.perDay)),
            )
        }
    }.orEmpty()

    /** The cap's sentence on a tall card, fullest first ([CapNote]); none where it has nothing to add. */
    fun noteForms(state: CapState): List<String> = if (hidden) {
        hiddenNotes(state)
    } else {
        when (val note = CapNote.of(state)) {
            null -> emptyList()

            // The day is the last words' ("until Sunday"): the sentence says how much a day, not until when.
            is CapNote.Pace -> when {
                note.ahead -> listOf(
                    res.getString(
                        if (note.lessThan) R.string.widget_note_ahead_pace_less else R.string.widget_note_ahead_pace,
                        format.short(note.perDay),
                    ),
                    res.getString(R.string.widget_note_ahead_short),
                )

                note.lessThan -> listOf(res.getString(R.string.widget_note_pace_less, format.short(note.perDay)))

                else -> listOf(res.getString(R.string.widget_note_pace, format.short(note.perDay)))
            }

            CapNote.Ahead -> listOf(
                res.getString(R.string.widget_note_ahead),
                res.getString(R.string.widget_note_ahead_short),
            )

            is CapNote.Reserve -> listOf(res.getString(R.string.widget_note_reserve, format.short(note.reserve)))

            is CapNote.Forgiven -> listOf(
                res.getString(if (note.renews) R.string.widget_note_forgiven else R.string.widget_note_forgiven_end),
            )

            is CapNote.Ended -> if (note.spent > note.cap) {
                listOf(res.getString(R.string.widget_note_ended_over, format.short(note.spent - note.cap)))
            } else {
                listOf(res.getString(R.string.widget_note_ended_under, format.short(note.cap - note.spent)))
            }
        }
    }

    /**
     * A hidden card's sentence: what can be said without a figure. Over, that it is forgiven; ended,
     * how it ended; under, nothing the hero has not said already.
     */
    private fun hiddenNotes(state: CapState): List<String> = when (val note = CapNote.of(state)) {
        is CapNote.Forgiven -> listOf(
            res.getString(if (note.renews) R.string.widget_note_forgiven else R.string.widget_note_forgiven_end),
        )

        is CapNote.Ended -> listOf(statusWords(state))

        else -> emptyList()
    }

    /** Where a cap is, as words inside a sentence: "under its cap", "ahead of the days", "over its cap". */
    private fun statusWords(state: CapState): String = res.getString(
        when {
            state.over -> R.string.widget_note_hidden_over
            state.aheadOfDays -> R.string.widget_note_ahead_short
            else -> R.string.widget_note_hidden_under
        },
    )

    /** What TalkBack reads for a cap, one sentence: "Coffee, €12 left of €20, until Sunday, about €4 a day". */
    fun description(state: CapState): String {
        if (hidden) {
            return if (state.ended) {
                res.getString(R.string.widget_description_hidden_ended, state.cap.name, day(state), statusWords(state))
            } else {
                res.getString(R.string.widget_description_hidden, state.cap.name, statusWords(state), day(state))
            }
        }
        val main = if (state.ended) {
            res.getString(
                R.string.widget_description_ended,
                state.cap.name,
                day(state),
                format.short(state.spent),
                format.short(state.amount),
            )
        } else if (state.over) {
            res.getString(
                R.string.widget_description_over,
                state.cap.name,
                format.short(state.overBy),
                format.short(state.amount),
                day(state),
            )
        } else {
            res.getString(
                R.string.widget_description_left,
                state.cap.name,
                format.short(state.left),
                format.short(state.amount),
                until(state.lastDay),
            )
        }
        val extras = listOfNotNull(
            res.getString(R.string.widget_description_ahead).takeIf { state.aheadOfDays },
            state.pace?.let {
                res.getString(
                    if (it.lessThan) R.string.widget_description_pace_less else R.string.widget_description_pace,
                    format.short(it.perDay),
                )
            },
            state.cap.reserve.takeIf { it.isPositive }?.let {
                res.getString(R.string.widget_description_reserve, format.short(it))
            },
        )
        return (listOf(main) + extras).joinToString(", ")
    }

    /** "3 more"; [short], "+3". */
    fun more(count: Int, short: Boolean = false): String =
        res.getQuantityString(if (short) R.plurals.widget_more_short else R.plurals.widget_more, count, count)

    /** "until Sunday": the period's last day, included. */
    fun until(lastDay: LocalDate): String = when (Days.near(lastDay, today)) {
        NearDay.TODAY -> res.getString(R.string.widget_until_today)
        NearDay.TOMORROW -> res.getString(R.string.widget_until_tomorrow)
        NearDay.WEEKDAY -> res.getString(R.string.widget_until_day, weekday(lastDay))
        NearDay.DATE -> res.getString(R.string.widget_until_day, dateWords(lastDay))
    }

    /** "on Monday": when a cap over starts again, the next period's first day. */
    fun again(startsAgain: LocalDate): String = when (Days.near(startsAgain, today)) {
        NearDay.TOMORROW -> res.getString(R.string.widget_again_tomorrow)
        NearDay.TODAY, NearDay.WEEKDAY -> res.getString(R.string.widget_again_day, weekday(startsAgain))
        NearDay.DATE -> res.getString(R.string.widget_again_day, dateWords(startsAgain))
    }

    private fun weekday(date: LocalDate): String = pattern("EEEE").format(date)

    private fun dateWords(date: LocalDate): String {
        val skeleton = res.getString(R.string.widget_skeleton_date)
        return pattern(if (date.year == today.year) skeleton else skeleton + "y").format(date)
    }

    private fun pattern(skeleton: String): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}
