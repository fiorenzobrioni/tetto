package com.callbackdev.tetto.feature.caps

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.domain.cap.CapsSentence
import com.callbackdev.tetto.core.domain.cap.SpendPreview
import com.callbackdev.tetto.core.domain.period.Days
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.NearDay
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Renewal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Formatter
import java.util.Locale

/*
 * The caps' words (PLANNING.md §4.6): every sentence the caps screen, its tiles, its sheets and the
 * numpad say, from the engine's structures. The engine never writes a word; the dates are written
 * with the locale's own patterns, the amounts by [MoneyFormat].
 */

/** "until Sunday": the period's last day, included (owner, 5 Nov 2026). */
@Composable
@ReadOnlyComposable
internal fun untilWords(lastDay: LocalDate, today: LocalDate): String = when (Days.near(lastDay, today)) {
    NearDay.TODAY -> stringResource(R.string.until_today)
    NearDay.TOMORROW -> stringResource(R.string.until_tomorrow)
    NearDay.WEEKDAY -> stringResource(R.string.until_day, weekday(lastDay))
    NearDay.DATE -> stringResource(R.string.until_day, dateWords(lastDay, today))
}

/** "on Monday": when a cap over starts again, the next period's first day. */
@Composable
@ReadOnlyComposable
internal fun againWords(startsAgain: LocalDate, today: LocalDate): String = when (Days.near(startsAgain, today)) {
    NearDay.TOMORROW -> stringResource(R.string.again_tomorrow)
    NearDay.TODAY, NearDay.WEEKDAY -> stringResource(R.string.again_day, weekday(startsAgain))
    NearDay.DATE -> stringResource(R.string.again_day, dateWords(startsAgain, today))
}

/**
 * What a cap over says of its next day: when it starts again ("on Monday"); a one-off, which never
 * does, until when it runs ("until Friday").
 */
@Composable
@ReadOnlyComposable
internal fun overDayWords(state: CapState, today: LocalDate): String =
    if (state.renews) againWords(state.startsAgain, today) else untilWords(state.lastDay, today)

/** The sentence on top of the caps screen; null with no cap (the empty page says it its own way). */
@Composable
@ReadOnlyComposable
internal fun sentenceWords(sentence: CapsSentence, format: MoneyFormat, today: LocalDate): String? = when (sentence) {
    CapsSentence.NoCaps -> null

    is CapsSentence.AllEnded -> stringResource(R.string.sentence_all_ended)

    is CapsSentence.Ended -> {
        val first = sentence.first
        when {
            sentence.others > 0 -> pluralStringResource(
                R.plurals.sentence_ended_some,
                sentence.others,
                first.cap.name,
                sentence.others,
            )

            first.over -> stringResource(
                R.string.sentence_ended_over,
                first.cap.name,
                format.short(first.spent),
                format.short(first.cap.amount),
                format.short(first.overBy),
            )

            else -> stringResource(
                R.string.sentence_ended_under,
                first.cap.name,
                format.short(first.spent),
                format.short(first.cap.amount),
            )
        }
    }

    is CapsSentence.NothingSpent -> stringResource(R.string.sentence_nothing_spent)

    is CapsSentence.AllUnder -> {
        val closest = sentence.closest
        val args = arrayOf(closest.cap.name, format.short(closest.left), untilWords(closest.lastDay, today))
        if (sentence.caps == 1) {
            stringResource(R.string.sentence_one_under, *args)
        } else {
            stringResource(R.string.sentence_all_under, *args)
        }
    }

    is CapsSentence.Over -> {
        val first = sentence.first
        val again = overDayWords(first, today)
        // A one-off never starts again: it says until when it runs (owner, 6 Nov 2026).
        val renews = first.renews
        when {
            sentence.others == 0 -> stringResource(
                if (renews) R.string.sentence_over_one else R.string.sentence_over_one_until,
                first.cap.name,
                format.short(first.overBy),
                again,
            )

            sentence.others + 1 == sentence.caps -> stringResource(
                if (renews) R.string.sentence_over_every else R.string.sentence_over_every_until,
                first.cap.name,
                again,
            )

            else -> pluralStringResource(
                if (renews) R.plurals.sentence_over_some else R.plurals.sentence_over_some_until,
                sentence.others,
                first.cap.name,
                sentence.others,
                again,
            )
        }
    }

    is CapsSentence.Renewed -> {
        val first = sentence.first
        if (sentence.others == 0) {
            // How the period that ended went, said the day it happens (owner, 6 Nov 2026):
            // forgiveness above all, the moment Tetto is kind.
            val renewal = first.renewal
            val words = when (renewal?.kind) {
                Renewal.Kind.FORGIVEN -> R.string.sentence_renewed_forgiven
                Renewal.Kind.COVERED -> R.string.sentence_renewed_covered
                Renewal.Kind.SAVED -> R.string.sentence_renewed_saved
                Renewal.Kind.RESERVE_FULL -> R.string.sentence_renewed_full
                Renewal.Kind.EVEN, null -> R.string.sentence_renewed_one
            }
            stringResource(
                words,
                first.cap.name,
                format.short(first.left),
                untilWords(first.lastDay, today),
                format.short(renewal?.amount ?: first.left),
            )
        } else {
            pluralStringResource(R.plurals.sentence_renewed_some, sentence.others, first.cap.name, sentence.others)
        }
    }
}

/** A tile's figure alone, what is left or by how much it is over: its word is the tile's. */
@Composable
@ReadOnlyComposable
internal fun figureWords(state: CapState, format: MoneyFormat): String =
    format.short(if (state.over) state.overBy else state.left)

/** A cap's figure in words, on one line: "€12 left", "€30 over" (the numpad, the home screen's numpad). */
@Composable
@ReadOnlyComposable
internal fun leftWords(state: CapState, format: MoneyFormat): String = if (state.over) {
    stringResource(R.string.tile_over, format.short(state.overBy))
} else {
    stringResource(R.string.tile_left, format.short(state.left))
}

/** A tile's second line: "of €20 · until Sunday", or, over, "of €100 · starts again on Monday". */
@Composable
@ReadOnlyComposable
internal fun detailWords(state: CapState, format: MoneyFormat, today: LocalDate): String = if (state.over &&
    state.renews
) {
    stringResource(R.string.tile_detail_over, format.short(state.cap.amount), againWords(state.startsAgain, today))
} else {
    stringResource(R.string.tile_detail, format.short(state.cap.amount), untilWords(state.lastDay, today))
}

/** A tile's two lines under its figure: "of €20" and "until Sunday", or, over, "starts again on Monday". */
@Composable
@ReadOnlyComposable
internal fun tileLines(state: CapState, format: MoneyFormat, today: LocalDate): Pair<String, String> =
    stringResource(R.string.tile_of, format.short(state.cap.amount)) to if (state.over && state.renews) {
        stringResource(R.string.tile_again, againWords(state.startsAgain, today))
    } else {
        untilWords(state.lastDay, today)
    }

@Composable
@ReadOnlyComposable
internal fun paceWords(state: CapState, format: MoneyFormat): String? =
    state.pace?.let { stringResource(R.string.tile_pace, format.short(it)) }

@Composable
@ReadOnlyComposable
internal fun reserveWords(state: CapState, format: MoneyFormat): String? =
    state.cap.reserve.takeIf { it.isPositive }?.let { stringResource(R.string.tile_reserve, format.short(it)) }

/** What TalkBack reads for a tile, one sentence: "Coffee, €12 left of €20, until Sunday". */
@Composable
@ReadOnlyComposable
internal fun tileDescription(state: CapState, format: MoneyFormat, today: LocalDate): String {
    val main = if (state.over) {
        stringResource(
            if (state.renews) R.string.tile_description_over else R.string.tile_description_over_until,
            state.cap.name,
            format.short(state.overBy),
            format.short(state.cap.amount),
            overDayWords(state, today),
        )
    } else {
        stringResource(
            R.string.tile_description_left,
            state.cap.name,
            format.short(state.left),
            format.short(state.cap.amount),
            untilWords(state.lastDay, today),
        )
    }
    val extras = listOfNotNull(
        stringResource(R.string.tile_description_ahead).takeIf { state.aheadOfDays },
        paceWords(state, format),
        state.cap.reserve.takeIf { it.isPositive }?.let {
            stringResource(R.string.tile_description_reserve, format.short(it))
        },
    )
    return (listOf(main) + extras).joinToString(", ")
}

/**
 * An ended one-off's two lines (owner, 6 Nov 2026): "Ended on 20 Nov" and "€720 of €800", or, over,
 * "€850 of €800 · €50 over": over said in words.
 */
@Composable
@ReadOnlyComposable
internal fun endedLines(state: CapState, format: MoneyFormat, today: LocalDate): Pair<String, String> =
    stringResource(R.string.tile_ended, dayWords(state.lastDay, today)) to if (state.over) {
        stringResource(
            R.string.tile_ended_over,
            format.short(state.spent),
            format.short(state.cap.amount),
            format.short(state.overBy),
        )
    } else {
        stringResource(R.string.tile_ended_of, format.short(state.spent), format.short(state.cap.amount))
    }

/** What TalkBack reads for an ended one-off: "Lisbon trip, ended on 20 Nov, €720 spent of €800". */
@Composable
@ReadOnlyComposable
internal fun endedDescription(state: CapState, format: MoneyFormat, today: LocalDate): String = if (state.over) {
    stringResource(
        R.string.tile_ended_description_over,
        state.cap.name,
        dayWords(state.lastDay, today),
        format.short(state.spent),
        format.short(state.cap.amount),
        format.short(state.overBy),
    )
} else {
    stringResource(
        R.string.tile_ended_description,
        state.cap.name,
        dayWords(state.lastDay, today),
        format.short(state.spent),
        format.short(state.cap.amount),
    )
}

/** A day as a date, "20 Nov", in the locale's pattern: an ended cap's last day, a one-off's dates. */
@Composable
@ReadOnlyComposable
internal fun dayWords(date: LocalDate, today: LocalDate): String = dateWords(date, today)

/** The numpad's preview: what the spend typed would leave. */
@Composable
@ReadOnlyComposable
internal fun previewWords(preview: SpendPreview, format: MoneyFormat): String = when (preview.kind) {
    SpendPreview.Kind.FITS -> stringResource(R.string.preview_left, format.short(preview.amount))
    SpendPreview.Kind.GOES_OVER -> stringResource(R.string.preview_over, format.short(preview.amount))
    SpendPreview.Kind.STILL_OVER -> stringResource(R.string.preview_still_over, format.short(preview.amount))
}

/**
 * One line of a cap's story: "2 – 8 Nov: €18 of €20, +€2 to the reserve"; a run of empty periods
 * "26 Oct – 15 Nov: nothing spent in 3 weeks, the reserve at €20".
 */
@Composable
@ReadOnlyComposable
internal fun lineWords(line: ClosedPeriod, kind: PeriodKind, format: MoneyFormat, today: LocalDate): String {
    val range = rangeWords(line.range, today)
    if (line.periods > 1) {
        val plurals = when (kind) {
            PeriodKind.DAY -> R.plurals.line_run_days

            PeriodKind.WEEK -> R.plurals.line_run_weeks

            PeriodKind.MONTH -> R.plurals.line_run_months

            PeriodKind.YEAR -> R.plurals.line_run_years

            // A one-off closes once, never a run: its line is never folded.
            PeriodKind.ONE_OFF -> R.plurals.line_run_days
        }
        return pluralStringResource(plurals, line.periods, range, line.periods, format.short(line.reserveAfter))
    }
    val spent = format.short(line.spent)
    val cap = format.short(line.cap)
    return when {
        line.forgiven.isPositive -> stringResource(
            R.string.line_forgiven,
            range,
            spent,
            cap,
            format.short(line.forgiven),
        )

        line.reserveAfter > line.reserveBefore ->
            stringResource(
                R.string.line_to_reserve,
                range,
                spent,
                cap,
                format.short(line.reserveAfter - line.reserveBefore),
            )

        line.reserveAfter < line.reserveBefore ->
            stringResource(
                R.string.line_from_reserve,
                range,
                spent,
                cap,
                format.short(line.reserveBefore - line.reserveAfter),
            )

        else -> stringResource(R.string.line_even, range, spent, cap)
    }
}

/** A range of days as the locale writes one: "2 – 8 Nov", one day alone for a day; the year only when it is not this one. */
@Composable
@ReadOnlyComposable
internal fun rangeWords(range: DateRange, today: LocalDate): String {
    val context = LocalContext.current
    val locale = locale()
    val last = range.lastDay
    var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    flags = flags or if (range.start.year == today.year && last.year == today.year) {
        DateUtils.FORMAT_NO_YEAR
    } else {
        DateUtils.FORMAT_SHOW_YEAR
    }
    val start = range.start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    // Noon of the last day: a range ending at a midnight would read as ending the day before.
    val end = last.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    return DateUtils.formatDateRange(context, Formatter(StringBuilder(), locale), start, end, flags, "UTC").toString()
}

/** A spend's moment in the cap's sheet: "Thu 14:32", in the zone of now and the phone's 12 or 24 hours. */
@Composable
@ReadOnlyComposable
internal fun momentWords(instant: Instant): String {
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "EEEHmm" else "EEEhmma"
    return pattern(skeleton).format(instant.atZone(ZoneId.systemDefault()))
}

/** The caps screen's heading: today, "Thursday, 5 November". */
@Composable
@ReadOnlyComposable
internal fun headingDate(today: LocalDate): String = pattern("EEEEdMMMM").format(today)

@Composable
@ReadOnlyComposable
private fun weekday(date: LocalDate): String = pattern("EEEE").format(date)

/**
 * A date as the locale writes one, "30 Nov", "Nov 30", "lunedì 30 nov", held together by no-break
 * spaces: a line that ends between a month and its day ("until Nov / 30") reads as two things.
 */
@Composable
@ReadOnlyComposable
private fun dateWords(date: LocalDate, today: LocalDate): String {
    val skeleton = stringResource(R.string.skeleton_date)
    return pattern(if (date.year == today.year) skeleton else skeleton + "y").format(date).replace(' ', NO_BREAK)
}

private const val NO_BREAK = '\u00A0'

@Composable
@ReadOnlyComposable
private fun pattern(skeleton: String): DateTimeFormatter {
    val locale = locale()
    return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
@ReadOnlyComposable
private fun locale(): Locale = LocalConfiguration.current.locales[0]
