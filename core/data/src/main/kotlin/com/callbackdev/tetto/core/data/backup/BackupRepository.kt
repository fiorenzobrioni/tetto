package com.callbackdev.tetto.core.data.backup

import androidx.room.withTransaction
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.db.CapEntity
import com.callbackdev.tetto.core.data.db.ClosedPeriodEntity
import com.callbackdev.tetto.core.data.db.SpendEntity
import com.callbackdev.tetto.core.data.db.TettoDatabase
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.settings.resolvedCurrency
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.domain.backup.Backup
import com.callbackdev.tetto.core.domain.backup.BackupCap
import com.callbackdev.tetto.core.domain.backup.BackupClosed
import com.callbackdev.tetto.core.domain.backup.BackupSpend
import com.callbackdev.tetto.core.domain.backup.CsvExport
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** What this phone holds, as Your data's rows say it before an export or an erase. */
data class DataContents(val caps: Int, val spends: Int) {
    val isEmpty: Boolean get() = caps == 0
}

/** The spends as a spreadsheet takes them, and how many rows that is. */
data class SpendsTable(val csv: String, val rows: Int)

/**
 * Your data (PLANNING.md §5, §11 Phase 4): the whole of it to a backup and back, the spends for a
 * spreadsheet, and erasing everything. Files are the caller's: the Storage Access Framework hands
 * the screen a stream, so Tetto needs no storage permission and sends nothing; this class makes
 * and takes their contents.
 *
 * Each runs under the caps' one lock ([CapsRepository.exclusively]), with the catch-up first in
 * the same transaction, as every other read and write of the caps does: a backup never holds a
 * period that should have closed, and nothing reads between a restore's settings and its caps.
 */
@Singleton
class BackupRepository
@Inject
constructor(
    private val database: TettoDatabase,
    private val preferences: UserPreferencesDataSource,
    private val caps: CapsRepository,
    private val clock: TodaySource,
    private val phone: PhoneDefaults,
) {
    private val dao = database.capsDao()

    /** How many caps and spends there are, as they change. */
    val contents: Flow<DataContents> = database.invalidationTracker.createFlow(*TettoDatabase.CAP_TABLES)
        .conflate()
        .map { DataContents(dao.capCount(), dao.spendCount()) }
        .distinctUntilChanged()

    /**
     * Everything a new phone could not rebuild, as it stands now, after the catch-up: the caps in
     * the reader's order with every spend and closed line, and the reader's settings, the currency
     * in force written as a choice (a file restored elsewhere must not take that phone's region's).
     */
    suspend fun backup(appVersion: String): Backup = caps.exclusively {
        val settings = preferences.current()
        val moment = caps.moment()
        val backup = database.withTransaction {
            caps.catchUpLocked(settings, moment)
            val spends = dao.allSpends().groupBy { it.capId }
            val closed = dao.allClosedPeriods().groupBy { it.capId }
            val currency = settings.resolvedCurrency(phone)
            Backup(
                exportedAt = moment.now,
                zone = moment.zone.id,
                appVersion = appVersion,
                currency = currency,
                // The reader's choices only: what belongs to this phone stays at its default.
                settings = settings.copy(
                    currency = currency.code,
                    onboardingCompleted = false,
                    askedNotifications = false,
                    capsTipSeen = false,
                    weekInForce = null,
                ),
                // The week the open periods run on now, after the catch-up: what lets a phone that
                // starts its weeks on another day move them rather than misread them (§4.5).
                weekInForce = settings.weekStart ?: phone.weekStart(),
                caps = dao.capRows().map { row ->
                    val cap = row.cap
                    BackupCap(
                        name = cap.name,
                        icon = CapIcon.fromKey(cap.icon),
                        amount = Money(cap.amount),
                        kind = PeriodKind.valueOf(cap.kind),
                        reserve = Money(cap.reserve),
                        openStart = LocalDate.ofEpochDay(cap.openStart),
                        createdAt = Instant.ofEpochMilli(cap.createdAt),
                        spends = spends[cap.id].orEmpty().sortedBy { it.addedAt }.map { it.toBackup() },
                        closed = closed[cap.id].orEmpty().sortedBy { it.start }.map { it.toBackup() },
                        endExclusive = cap.endExclusive?.let(LocalDate::ofEpochDay),
                    )
                },
            )
        }
        caps.keepWeekInForce(settings)
        backup
    }

    /** Every spend, one row each, from the same snapshot a backup takes. */
    suspend fun spendsTable(): SpendsTable {
        val snapshot = backup(appVersion = "")
        return SpendsTable(CsvExport.spends(snapshot, phone.weekStart(), clock.zone()), snapshot.spendCount)
    }

    /**
     * Replaces this phone's caps, spends and closed periods with [backup]'s, and its settings with
     * the backup's choices (PLANNING.md §5: a restore replaces, after the reader's confirmation,
     * which is the screen's). The settings first, since the caps' open periods were made by the
     * backup's week and month starts; then, in one transaction, the old caps out, the backup's in,
     * in its order, and the catch-up, so a backup from last month closes what has ended since.
     *
     * Should the transaction fail, nothing of it is written and the settings are put back: the
     * phone is as it was. What belongs to this phone stays: whether it asked for the permission,
     * whether it showed the caps screen's first-day tip, its lock.
     */
    suspend fun restore(backup: Backup) {
        caps.exclusively {
            val before = preferences.current()
            val after = preferences.update { current ->
                backup.settings.copy(
                    currency = backup.currency.code,
                    onboardingCompleted = true,
                    askedNotifications = current.askedNotifications,
                    capsTipSeen = current.capsTipSeen,
                    // This phone's lock, guarded by this phone's own: never a file's to turn off.
                    appLock = current.appLock,
                    // The week the backup's open periods were made by: the catch-up moves them to
                    // this phone's, where the reader's week follows the phone and the two differ.
                    weekInForce = backup.weekInForce,
                )
            }
            try {
                database.withTransaction {
                    dao.deleteAllCaps()
                    backup.caps.forEachIndexed { position, cap ->
                        val id = dao.insertCap(cap.toEntity(position))
                        dao.insertSpends(cap.spends.map { it.toEntity(id) })
                        dao.insertClosedPeriods(cap.closed.map { it.toEntity(id) })
                    }
                    caps.catchUpLocked(after, caps.moment())
                }
            } catch (failure: Throwable) {
                withContext(NonCancellable) { preferences.update { before } }
                throw failure
            }
            caps.keepWeekInForce(after)
        }
    }

    /**
     * Erases everything the reader made (VISION, «Settings»): every cap with its spends and closed
     * periods, then the settings, back to the first run. The caps first: should the settings fail,
     * the reader sees an empty caps screen, never a first run that would add its caps to the old.
     * Whether the permission was asked, and whether the first-day tip was shown, are the phone's,
     * not the reader's data, and stay: a reader who erases has met the caps screen already. The
     * lock stays too: what it guards is this phone's Tetto, whatever it holds.
     */
    suspend fun erase() {
        caps.exclusively {
            database.withTransaction { dao.deleteAllCaps() }
            preferences.update {
                UserSettings(
                    askedNotifications = it.askedNotifications,
                    capsTipSeen = it.capsTipSeen,
                    appLock = it.appLock,
                )
            }
        }
    }
}

private fun SpendEntity.toBackup() =
    BackupSpend(Money(amount), Instant.ofEpochMilli(addedAt), LocalDate.ofEpochDay(periodStart))

private fun ClosedPeriodEntity.toBackup() = BackupClosed(
    range = DateRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(endExclusive)),
    cap = Money(cap),
    spent = Money(spent),
    reserveBefore = Money(reserveBefore),
    reserveAfter = Money(reserveAfter),
    forgiven = Money(forgiven),
    periods = periods,
)

private fun BackupCap.toEntity(position: Int) = CapEntity(
    name = name,
    icon = icon.key,
    amount = amount.minor,
    kind = kind.name,
    reserve = reserve.minor,
    openStart = openStart.toEpochDay(),
    position = position,
    createdAt = createdAt.toEpochMilli(),
    endExclusive = endExclusive?.toEpochDay(),
)

private fun BackupSpend.toEntity(capId: Long) = SpendEntity(
    capId = capId,
    amount = amount.minor,
    addedAt = addedAt.toEpochMilli(),
    periodStart = periodStart.toEpochDay(),
)

private fun BackupClosed.toEntity(capId: Long) = ClosedPeriodEntity(
    capId = capId,
    start = range.start.toEpochDay(),
    endExclusive = range.endExclusive.toEpochDay(),
    cap = cap.minor,
    spent = spent.minor,
    reserveBefore = reserveBefore.minor,
    reserveAfter = reserveAfter.minor,
    forgiven = forgiven.minor,
    periods = periods,
)
