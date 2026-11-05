package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.PeriodRule
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The spends for a spreadsheet (PLANNING.md §5): one row per spend of every period, oldest first,
 * as RFC 4180 writes CSV (commas, CRLF line ends, quotes only where a field needs them), in UTF-8
 * with a byte-order mark so a spreadsheet reads an accented cap's name right.
 *
 * The header is fixed English identifiers, the same in every language, so a formula written once
 * keeps working: `date` and `time` (when it was added, in [ZoneId] the phone is in now, 24-hour),
 * `cap` (its name), `amount` (in the currency's whole unit, a dot for decimals: `12.50`, `13` for
 * the yen), `currency` (the ISO code), `period_start` and `period_end` (the period it counts in,
 * its first and last day, both included, as the cap's sheet says them).
 *
 * A name that starts like a formula (`=`, `+`, `-`, `@`) is written as text, so a spreadsheet
 * shows it rather than running it.
 */
object CsvExport {
    /** The byte-order mark, written as its code so no editor or formatter can drop it unseen. */
    private val BOM = Char(0xFEFF).toString()
    private const val EOL = "\r\n"
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private val HEADER = listOf("date", "time", "cap", "amount", "currency", "period_start", "period_end")
    private val FORMULA_STARTS = setOf('=', '+', '-', '@')

    /** The name offered for the file: `tetto-spends-2026-11-05.csv`. */
    fun fileName(date: LocalDate): String = "tetto-spends-$date.csv"

    /**
     * Every spend of [backup], the backup's own snapshot, so the two files of one moment agree.
     * [weekStart] is the week in force (the reader's, else the phone's): the open periods' ends
     * follow from it.
     */
    fun spends(backup: Backup, weekStart: DayOfWeek, zone: ZoneId): String {
        val decimals = backup.currency.fractionDigits
        val rows = backup.caps.flatMapIndexed { order, cap ->
            val rule = PeriodRule(cap.kind, backup.settings.weekStart ?: weekStart, backup.settings.monthStartDay)
            val open = PeriodCalendar.openPeriod(cap.openStart, rule)
            cap.spends.map { spend -> Row(order, cap.name, spend, periodOf(spend.periodStart, open, cap.closed)) }
        }.sortedWith(compareBy<Row>({ it.spend.addedAt }, { it.order }))
        return buildString {
            append(BOM)
            append(HEADER.joinToString(",")).append(EOL)
            for (row in rows) {
                val added = row.spend.addedAt.atZone(zone)
                val fields = listOf(
                    added.toLocalDate().toString(),
                    TIME.format(added),
                    row.cap,
                    BigDecimal.valueOf(row.spend.amount.minor, decimals).toPlainString(),
                    backup.currency.code,
                    row.spend.periodStart.toString(),
                    row.period?.lastDay?.toString().orEmpty(),
                )
                append(fields.joinToString(",", transform = ::field)).append(EOL)
            }
        }
    }

    /**
     * The period a spend counts in: the open one, or the closed line that starts on its period's
     * start. Every spend has one; should a file say otherwise, its end is left empty rather than
     * guessed.
     */
    private fun periodOf(start: LocalDate, open: DateRange, closed: List<BackupClosed>): DateRange? = when {
        start == open.start -> open
        else -> closed.firstOrNull { it.range.start == start }?.range ?: closed.firstOrNull { start in it.range }?.range
    }

    /** A field as RFC 4180 wants it, and never a formula. */
    private fun field(value: String): String {
        val safe = if (value.isNotEmpty() && value[0] in FORMULA_STARTS) "'$value" else value
        val quoted = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (quoted) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }

    private data class Row(val order: Int, val cap: String, val spend: BackupSpend, val period: DateRange?)
}
