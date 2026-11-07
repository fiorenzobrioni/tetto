package com.callbackdev.tetto.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Tetto's database (PLANNING.md §5). The schema is exported to `core/data/schemas` and
 * committed: the database is a contract, every later version comes with a migration and its test,
 * and nothing is ever dropped to make an upgrade pass.
 *
 * - v1: the caps, their spends, the closed periods (Phase 1); a one-off cap's end (Phase 6b),
 *   added to version 1 in place before the first release (owner, 6 Nov 2026: no reader has a
 *   database to migrate yet).
 */
@Database(
    entities = [CapEntity::class, SpendEntity::class, ClosedPeriodEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TettoDatabase : RoomDatabase() {
    abstract fun capsDao(): CapsDao

    companion object {
        /** Named in the backup's allowlist (`data_extraction_rules.xml`): renaming it drops it from the backup. */
        const val NAME = "tetto.db"

        /** The tables the caps' flow watches. */
        val CAP_TABLES = arrayOf("caps", "spends", "closed_periods")
    }
}
