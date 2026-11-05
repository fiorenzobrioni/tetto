package com.callbackdev.tetto.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * The caps, their spends and their closed periods (PLANNING.md §5). Plain reads and writes: the
 * rules about which period a spend counts in, what a catch-up writes and what a change may touch
 * are `CapsRepository`'s, which runs them in one transaction.
 */
@Dao
interface CapsDao {
    /** Every cap in the reader's order, with what its open period spent and its last closed end. */
    @Query(
        """
        SELECT c.*,
            COALESCE(
                (SELECT SUM(s.amount) FROM spends s WHERE s.cap_id = c.id AND s.period_start = c.open_start),
                0
            ) AS spent,
            (SELECT MAX(p.end_exclusive) FROM closed_periods p WHERE p.cap_id = c.id) AS last_closed_end
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

    @Query("UPDATE caps SET kind = :kind, open_start = :openStart WHERE id = :id")
    suspend fun setCapPeriod(id: Long, kind: String, openStart: Long): Int

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

    @Query("UPDATE spends SET amount = :amount WHERE id = :id")
    suspend fun setSpendAmount(id: Long, amount: Long): Int

    @Query("DELETE FROM spends WHERE id = :id")
    suspend fun deleteSpend(id: Long): Int

    /** Moves the open period's spends into the period that replaces it (§4.5). */
    @Query("UPDATE spends SET period_start = :to WHERE cap_id = :capId AND period_start = :from")
    suspend fun moveSpends(capId: Long, from: Long, to: Long): Int

    /** Whether any spend was added within `[from, until)`, in epoch milliseconds. */
    @Query("SELECT EXISTS(SELECT 1 FROM spends WHERE added_at >= :from AND added_at < :until)")
    suspend fun anySpendBetween(from: Long, until: Long): Boolean

    @Insert
    suspend fun insertClosedPeriods(periods: List<ClosedPeriodEntity>)

    /** A cap's closed periods, the latest first. */
    @Query("SELECT * FROM closed_periods WHERE cap_id = :capId ORDER BY start DESC")
    suspend fun closedPeriods(capId: Long): List<ClosedPeriodEntity>

    @Query("SELECT MAX(end_exclusive) FROM closed_periods WHERE cap_id = :capId")
    suspend fun lastClosedEnd(capId: Long): Long?
}
