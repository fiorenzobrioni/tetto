package com.callbackdev.tetto.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A cap (PLANNING.md §5). Amounts in minor units, dates as epoch days, instants as epoch
 * milliseconds; the icon by its key and the kind by its name, never by an ordinal.
 */
@Entity(tableName = "caps")
data class CapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val amount: Long,
    val kind: String,
    val reserve: Long,
    @ColumnInfo(name = "open_start") val openStart: Long,
    val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * A spend, with the start of the period it counts in, fixed when it was added (§4.3). Indexed by
 * cap and period (a cap's open period, its sum) and by the instant (the reminder's "today").
 */
@Entity(
    tableName = "spends",
    foreignKeys = [
        ForeignKey(
            entity = CapEntity::class,
            parentColumns = ["id"],
            childColumns = ["cap_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cap_id", "period_start"), Index("added_at")],
)
data class SpendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "cap_id") val capId: Long,
    val amount: Long,
    @ColumnInfo(name = "added_at") val addedAt: Long,
    @ColumnInfo(name = "period_start") val periodStart: Long,
)

/**
 * A closed period, or a run of empty ones folded (§4.4). Its end is `end_exclusive`, not `end`:
 * `END` is a keyword of SQLite's, and a column that needs quoting in every query is a bug waiting.
 */
@Entity(
    tableName = "closed_periods",
    primaryKeys = ["cap_id", "start"],
    foreignKeys = [
        ForeignKey(
            entity = CapEntity::class,
            parentColumns = ["id"],
            childColumns = ["cap_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ClosedPeriodEntity(
    @ColumnInfo(name = "cap_id") val capId: Long,
    val start: Long,
    @ColumnInfo(name = "end_exclusive") val endExclusive: Long,
    val cap: Long,
    val spent: Long,
    @ColumnInfo(name = "reserve_before") val reserveBefore: Long,
    @ColumnInfo(name = "reserve_after") val reserveAfter: Long,
    val forgiven: Long,
    val periods: Int,
)

/** A cap with what its open period spent and where its last closed period ended. */
data class CapRow(
    @Embedded val cap: CapEntity,
    val spent: Long,
    @ColumnInfo(name = "last_closed_end") val lastClosedEnd: Long?,
)
