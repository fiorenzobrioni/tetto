package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.UserSettings
import java.time.Instant
import java.time.LocalDate

/**
 * Everything a new phone could not rebuild (PLANNING.md §5, Backup): the caps in the reader's
 * order, each with every spend it holds and every line of its story, and the settings the reader
 * chose. Not what belongs to one phone: whether its first run is done, whether it ever asked for
 * the notification permission. Ids are not carried either: a restore gives new ones.
 *
 * @property zone the zone the phone was in when the file was written, for whoever reads the file;
 *   nothing is converted with it.
 * @property currency the currency every amount is counted in, with this phone's decimals for it
 *   (a file written where its decimals differed is rescaled as it is read).
 * @property settings the reader's choices: the money's (its [UserSettings.currency] is
 *   [currency]'s code), the appearance and the evening reminder.
 */
data class Backup(
    val exportedAt: Instant,
    val zone: String,
    val appVersion: String,
    val currency: AppCurrency,
    val settings: UserSettings,
    val caps: List<BackupCap>,
) {
    /** Every spend it holds, of every period. */
    val spendCount: Int get() = caps.sumOf { it.spends.size }
}

/** A cap as a backup carries it: its four fields, its reserve and open period, and its history. */
data class BackupCap(
    val name: String,
    val icon: CapIcon,
    val amount: Money,
    val kind: PeriodKind,
    val reserve: Money,
    val openStart: LocalDate,
    val createdAt: Instant,
    val spends: List<BackupSpend>,
    val closed: List<BackupClosed>,
)

/** A spend: its amount, when it was added, and the start of the period it counts in (§4.3). */
data class BackupSpend(val amount: Money, val addedAt: Instant, val periodStart: LocalDate)

/** One line of a cap's story (§4.4), as `ClosedPeriod` holds it, without the cap's id. */
data class BackupClosed(
    val range: DateRange,
    val cap: Money,
    val spent: Money,
    val reserveBefore: Money,
    val reserveAfter: Money,
    val forgiven: Money,
    val periods: Int,
)
