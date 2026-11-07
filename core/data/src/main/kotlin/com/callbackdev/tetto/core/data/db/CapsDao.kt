package com.callbackdev.tetto.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * The caps, their spends and their closed periods (PLANNING.md §5). Plain reads and writes: the
 * rules about which period a spend counts in, what a catch-up writes and what a change may touch
 * are `CapsRepository`'s, which runs them in one transaction.
 */
@Dao
interface CapsDao {
    /**
     * Every cap in the reader's order, with what its open period spent and its last closed end: the
     * end of its latest line, read from the key's own order (a cap's lines never overlap, so the
     * latest to start is the latest to end), one row whatever the cap's age.
     * What a period spent is its spends less its refunds, never below nothing (§4.3): a refund
     * whose spend was deleted after it gives no money that was never spent.
     */
    @Query(
        """
        SELECT c.*,
            MAX(0, COALESCE(
                (SELECT SUM(s.amount) FROM spends s WHERE s.cap_id = c.id AND s.period_start = c.open_start),
                0
            )) AS spent,
            (SELECT p.end_exclusive FROM closed_periods p WHERE p.cap_id = c.id ORDER BY p.start DESC LIMIT 1)
                AS last_closed_end
        FROM caps c
        ORDER BY c.position, c.id
        """,
    )
    suspend fun capRows(): List<CapRow>

    @Query("SELECT * FROM caps WHERE id = :id")
    suspend fun cap(id: Long): CapEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM caps")
    suspend fun lastPosition(): Int

    @Insert
    suspend fun insertCap(cap: CapEntity): Long

    @Query("UPDATE caps SET name = :name, icon = :icon, amount = :amount WHERE id = :id")
    suspend fun setCapLooks(id: Long, name: String, icon: String, amount: Long): Int

    /** The cap's kind and open start, and a one-off's end (null for any other kind). */
    @Query("UPDATE caps SET kind = :kind, open_start = :openStart, end_exclusive = :endExclusive WHERE id = :id")
    suspend fun setCapPeriod(id: Long, kind: String, openStart: Long, endExclusive: Long?): Int

    @Query("UPDATE caps SET open_start = :openStart, reserve = :reserve WHERE id = :id")
    suspend fun setOpenPeriod(id: Long, openStart: Long, reserve: Long): Int

    @Query("UPDATE caps SET reserve = :reserve WHERE id = :id")
    suspend fun setReserve(id: Long, reserve: Long): Int

    @Query("UPDATE caps SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int): Int

    /** Deletes the cap; its spends and closed periods go with it (`ON DELETE CASCADE`). */
    @Query("DELETE FROM caps WHERE id = :id")
    suspend fun deleteCap(id: Long): Int

    @Insert
    suspend fun insertSpend(spend: SpendEntity): Long

    @Query("SELECT * FROM spends WHERE id = :id")
    suspend fun spend(id: Long): SpendEntity?

    /** The spends of the period that starts on [periodStart], newest first. */
    @Query("SELECT * FROM spends WHERE cap_id = :capId AND period_start = :periodStart ORDER BY added_at DESC, id DESC")
    suspend fun spends(capId: Long, periodStart: Long): List<SpendEntity>

    /** The amounts of the cap's last [limit] spends, of any period, newest first: the numpad's habits. */
    @Query("SELECT amount FROM spends WHERE cap_id = :capId ORDER BY added_at DESC, id DESC LIMIT :limit")
    suspend fun lastAmounts(capId: Long, limit: Int): List<Long>

    @Query("UPDATE spends SET amount = :amount WHERE id = :id")
    suspend fun setSpendAmount(id: Long, amount: Long): Int

    @Query("DELETE FROM spends WHERE id = :id")
    suspend fun deleteSpend(id: Long): Int

    /**
     * Moves the open period's spends added at [since] or after (epoch milliseconds) into the
     * period that replaces it (§4.5): all of them where the new period holds the old one's days,
     * those of its own days where it starts later.
     */
    @Query(
        "UPDATE spends SET period_start = :to WHERE cap_id = :capId AND period_start = :from AND added_at >= :since",
    )
    suspend fun moveSpends(capId: Long, from: Long, to: Long, since: Long): Int

    /** What the open period's spends added before [until] (epoch milliseconds) come to: the days kept (§4.5). */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM spends WHERE cap_id = :capId AND period_start = :from AND added_at < :until",
    )
    suspend fun spentBefore(capId: Long, from: Long, until: Long): Long

    /** Whether any spend was added within `[from, until)`, in epoch milliseconds. */
    @Query("SELECT EXISTS(SELECT 1 FROM spends WHERE added_at >= :from AND added_at < :until)")
    suspend fun anySpendBetween(from: Long, until: Long): Boolean

    /** What every cap's spends added between two instants come to, closed periods' included. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM spends WHERE added_at >= :from AND added_at < :until")
    suspend fun addedBetween(from: Long, until: Long): Long

    @Insert
    suspend fun insertClosedPeriods(periods: List<ClosedPeriodEntity>)

    /** A cap's closed periods, the latest first. */
    @Query("SELECT * FROM closed_periods WHERE cap_id = :capId ORDER BY start DESC")
    suspend fun closedPeriods(capId: Long): List<ClosedPeriodEntity>

    /** A cap's [limit] latest closed periods, the latest first: the cap's sheet reads three, whatever its age. */
    @Query("SELECT * FROM closed_periods WHERE cap_id = :capId ORDER BY start DESC LIMIT :limit")
    suspend fun latestClosedPeriods(capId: Long, limit: Int): List<ClosedPeriodEntity>

    /** The cap's closed line that ends on [endExclusive], if one does: the period that renewed. */
    @Query("SELECT * FROM closed_periods WHERE cap_id = :capId AND end_exclusive = :endExclusive")
    suspend fun closedEnding(capId: Long, endExclusive: Long): ClosedPeriodEntity?

    /** Where the cap's latest line ends, from the key's order, as [capRows] reads it. */
    @Query("SELECT end_exclusive FROM closed_periods WHERE cap_id = :capId ORDER BY start DESC LIMIT 1")
    suspend fun lastClosedEnd(capId: Long): Long?

    // A change of currency (§4.1): every stored amount, read and written back rescaled.

    @Query("SELECT * FROM caps")
    suspend fun allCaps(): List<CapEntity>

    @Query("UPDATE caps SET amount = :amount, reserve = :reserve WHERE id = :id")
    suspend fun setCapAmounts(id: Long, amount: Long, reserve: Long): Int

    @Query("SELECT * FROM spends")
    suspend fun allSpends(): List<SpendEntity>

    @Query("SELECT * FROM closed_periods")
    suspend fun allClosedPeriods(): List<ClosedPeriodEntity>

    @Update
    suspend fun updateClosedPeriods(periods: List<ClosedPeriodEntity>)

    // Your data (Phase 4): the backup reads everything, a restore and an erase replace it.

    @Insert
    suspend fun insertSpends(spends: List<SpendEntity>)

    /**
     * Deletes every cap; their spends and closed periods go with them (`ON DELETE CASCADE`). The
     * ids' sequence stays: an id is never given twice, so an Undo still pending for a spend that
     * was here can never reach one restored after it.
     */
    @Query("DELETE FROM caps")
    suspend fun deleteAllCaps(): Int

    @Query("SELECT COUNT(*) FROM caps")
    suspend fun capCount(): Int

    @Query("SELECT COUNT(*) FROM spends")
    suspend fun spendCount(): Int
}
