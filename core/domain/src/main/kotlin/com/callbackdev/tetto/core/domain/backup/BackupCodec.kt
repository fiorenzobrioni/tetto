package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.money.MoneyInput
import com.callbackdev.tetto.core.domain.money.Rescale
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** What reading a file came to. */
sealed interface BackupRead {
    data class Ok(val backup: Backup) : BackupRead

    /** Readable or not, not a file Tetto wrote. */
    data object NotTetto : BackupRead

    /** A Tetto backup in a format this version does not know yet: the app needs an update. */
    data class TooNew(val version: Int) : BackupRead

    /** A Tetto backup that cannot be read to its end, or says something no Tetto could have written. */
    data object Damaged : BackupRead
}

/**
 * The backup file (PLANNING.md §5): JSON, UTF-8, one object, indented so it can be read without
 * the app.
 *
 * - `format` is always `tetto-backup`; `version` is [VERSION], raised only for a change an older
 *   reader would misread (a new period kind among them). An added field does not raise it: a
 *   reader skips what it does not know.
 * - Amounts are whole minor units of `currency` (an ISO 4217 code), with the `decimals` it had
 *   where the file was written; dates are ISO (`2026-11-02`), instants epoch milliseconds, a
 *   time of day `20:30`. The caps come in the reader's order, each holding its spends and its
 *   closed lines; a closed line's end is excluded, as a period's is.
 *
 * **Read whole and checked before anything is written**: [decode] returns a backup only when every
 * part of it is one Tetto could have written, so a restore never writes half a file, and never a
 * number that would lie. It is strict where a value decides a figure or a period (an amount, a
 * date, a period's kind, the month or week start) and lenient where a default cannot mislead (an
 * icon this build does not know reads as "other", an unknown palette as the default).
 */
object BackupCodec {
    const val FORMAT: String = "tetto-backup"
    const val VERSION: Int = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** The name offered for the file, dated so that several backups sort and tell apart. */
    fun fileName(date: LocalDate): String = "tetto-backup-$date.json"

    fun encode(backup: Backup): String = json.encodeToString(BackupFile.serializer(), backup.toFile())

    fun decode(text: String): BackupRead {
        // An editor may have put a byte-order mark in front: it says nothing about the content.
        val body = text.removePrefix(BOM)
        val root = try {
            json.parseToJsonElement(body) as? JsonObject ?: return BackupRead.NotTetto
        } catch (_: SerializationException) {
            // Not JSON: a file of something else, or one of Tetto's cut short on its way here.
            return if (body.contains("\"$FORMAT\"")) BackupRead.Damaged else BackupRead.NotTetto
        }
        val format = runCatching { root["format"]?.jsonPrimitive?.content }.getOrNull()
        if (format != FORMAT) return BackupRead.NotTetto
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: return BackupRead.Damaged
        if (version > VERSION) return BackupRead.TooNew(version)
        if (version < 1) return BackupRead.Damaged
        return try {
            BackupRead.Ok(json.decodeFromJsonElement(BackupFile.serializer(), root).toBackup())
        } catch (_: SerializationException) {
            BackupRead.Damaged
        } catch (_: IllegalArgumentException) {
            BackupRead.Damaged
        } catch (_: DateTimeException) {
            BackupRead.Damaged
        } catch (_: ArithmeticException) {
            BackupRead.Damaged
        }
    }

    private val BOM = Char(0xFEFF).toString()
}

private fun Backup.toFile() = BackupFile(
    format = BackupCodec.FORMAT,
    version = BackupCodec.VERSION,
    exportedAtMillis = exportedAt.toEpochMilli(),
    zone = zone,
    app = appVersion,
    currency = currency.code,
    decimals = currency.fractionDigits,
    settings = SettingsDto(
        monthStartDay = settings.monthStartDay,
        weekStart = settings.weekStart?.name,
        theme = settings.theme.name,
        palette = settings.palette.name,
        font = settings.font.name,
        capsLayout = settings.capsLayout.name,
        dynamicColor = settings.dynamicColor,
        reminderOn = settings.reminderOn,
        reminderTime = settings.reminderTime.withSecond(0).withNano(0).toString(),
    ),
    weekInForce = weekInForce?.name,
    caps = caps.map { cap ->
        CapDto(
            name = cap.name,
            icon = cap.icon.key,
            amount = cap.amount.minor,
            period = cap.kind.name,
            reserve = cap.reserve.minor,
            openStart = cap.openStart.toString(),
            createdAtMillis = cap.createdAt.toEpochMilli(),
            spends = cap.spends.sortedBy { it.addedAt }.map {
                SpendDto(it.amount.minor, it.addedAt.toEpochMilli(), it.periodStart.toString())
            },
            closed = cap.closed.sortedBy { it.range.start }.map {
                ClosedDto(
                    start = it.range.start.toString(),
                    endExclusive = it.range.endExclusive.toString(),
                    cap = it.cap.minor,
                    spent = it.spent.minor,
                    reserveBefore = it.reserveBefore.minor,
                    reserveAfter = it.reserveAfter.minor,
                    forgiven = it.forgiven.minor,
                    periods = it.periods,
                )
            },
        )
    },
)

/**
 * The file as a backup, every part checked: [require] fails it (as damaged) on anything Tetto
 * could not have written. Amounts are rescaled where the file's decimals for its currency are
 * not this phone's (a currency whose decimals changed between the two), as a change of currency
 * would rescale them (§4.1).
 */
private fun BackupFile.toBackup(): Backup {
    val currency = requireNotNull(AppCurrency.ofOrNull(currency)) { "Not a currency: $currency" }
    val fileDigits = decimals ?: currency.fractionDigits
    require(fileDigits in 0..AppCurrency.MAX_FRACTION_DIGITS) { "Decimals: $fileDigits" }
    fun money(minor: Long): Money = Rescale.amount(Money(minor), fileDigits, currency.fractionDigits)

    // What the numpad typed, held to what it can type back, as a change of currency holds it.
    fun typed(minor: Long): Money = Rescale.typed(Money(minor), fileDigits, currency.fractionDigits)

    val defaults = UserSettings()
    require(settings.monthStartDay in 1..31) { "Month start: ${settings.monthStartDay}" }
    val settings = UserSettings(
        currency = currency.code,
        monthStartDay = settings.monthStartDay,
        weekStart = settings.weekStart?.let { requireNotNull(enumOrNull<DayOfWeek>(it)) { "Week start: $it" } },
        theme = enumOrNull<ThemeMode>(settings.theme) ?: defaults.theme,
        palette = enumOrNull<AppPalette>(settings.palette) ?: defaults.palette,
        font = enumOrNull<AppFont>(settings.font) ?: defaults.font,
        capsLayout = enumOrNull<CapsLayout>(settings.capsLayout) ?: defaults.capsLayout,
        dynamicColor = settings.dynamicColor,
        reminderOn = settings.reminderOn,
        reminderTime = settings.reminderTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?.withSecond(0)?.withNano(0)
            ?: defaults.reminderTime,
    )

    val caps = caps.map { dto ->
        require(dto.name.isNotBlank()) { "A cap without a name" }
        val kind = requireNotNull(enumOrNull<PeriodKind>(dto.period)) { "Period: ${dto.period}" }
        val openStart = LocalDate.parse(dto.openStart)
        val amount = typed(dto.amount)
        require(amount.isPositive) { "A cap of nothing" }
        // The editor types a cap's amount back, the numpad a spend's: one it cannot type is no
        // amount Tetto wrote (PLANNING.md §4.1).
        require(MoneyInput.canType(amount, currency)) { "A cap past the numpad's digits" }
        val reserve = typed(dto.reserve)
        require(!reserve.isNegative) { "A negative reserve" }
        val spends = dto.spends.map { spend ->
            val periodStart = LocalDate.parse(spend.periodStart)
            // A spend counts in the open period or in one before it, never in one to come.
            require(periodStart <= openStart) { "A spend after the open period" }
            val spent = typed(spend.amount)
            // Below zero is a refund (owner, 6 Nov 2026); nothing is no spend at all.
            require(spent != Money.ZERO) { "A spend of nothing" }
            val size = if (spent.isNegative) -spent else spent
            require(MoneyInput.canType(size, currency)) { "A spend past the numpad's digits" }
            BackupSpend(spent, Instant.ofEpochMilli(spend.addedAtMillis), periodStart)
        }
        val closed = dto.closed.map { line ->
            val range = DateRange(LocalDate.parse(line.start), LocalDate.parse(line.endExclusive))
            require(range.endExclusive <= openStart) { "A closed line after the open period" }
            require(line.periods >= 1) { "A closed line of no period" }
            BackupClosed(
                range = range,
                cap = money(line.cap),
                spent = money(line.spent),
                reserveBefore = money(line.reserveBefore),
                reserveAfter = money(line.reserveAfter),
                forgiven = money(line.forgiven),
                periods = line.periods,
            ).also {
                require(
                    listOf(it.cap, it.spent, it.reserveBefore, it.reserveAfter, it.forgiven).none(Money::isNegative),
                ) { "A negative figure in a closed line" }
            }
        }
        // One line per period start: the database's key, and a story told once.
        require(closed.map { it.range.start }.toSet().size == closed.size) { "A closed period twice" }
        BackupCap(
            name = dto.name.trim(),
            icon = CapIcon.fromKey(dto.icon),
            amount = amount,
            kind = kind,
            reserve = reserve,
            openStart = openStart,
            createdAt = Instant.ofEpochMilli(dto.createdAtMillis),
            spends = spends.sortedBy { it.addedAt },
            closed = closed.sortedBy { it.range.start },
        )
    }
    // Every figure together still fits a Long, so no sum the engine makes of them can overflow:
    // Math.addExact throws, and the file reads as damaged.
    caps.fold(0L) { total, cap ->
        val figures = listOf(cap.amount, cap.reserve) + cap.spends.map { it.amount } +
            cap.closed.flatMap { listOf(it.cap, it.spent, it.reserveBefore, it.reserveAfter, it.forgiven) }
        figures.fold(total) { sum, money -> Math.addExact(sum, money.minor) }
    }

    return Backup(
        exportedAt = Instant.ofEpochMilli(exportedAtMillis),
        zone = zone,
        appVersion = app,
        currency = currency,
        settings = settings,
        caps = caps,
        weekInForce = weekInForce?.let { requireNotNull(enumOrNull<DayOfWeek>(it)) { "Week in force: $it" } },
    )
}

private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
    name?.let { enumValues<E>().firstOrNull { value -> value.name == it } }
