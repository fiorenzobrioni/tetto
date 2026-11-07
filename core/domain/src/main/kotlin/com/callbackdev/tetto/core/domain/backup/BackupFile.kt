package com.callbackdev.tetto.core.domain.backup

import kotlinx.serialization.Serializable

/*
 * The backup file's shape (PLANNING.md §5), kept apart from the app's model so the model can change
 * without changing what a file says. Every field has a default, so a file from an older build
 * that lacks one still reads; whether a value may stand in for a missing one is the codec's call.
 */

@Serializable
internal data class BackupFile(
    val format: String = "",
    val version: Int = 0,
    val exportedAtMillis: Long = 0,
    val zone: String = "",
    val app: String = "",
    val currency: String = "",
    /** The currency's decimals where the file was written; absent, the currency's own. */
    val decimals: Int? = null,
    val settings: SettingsDto = SettingsDto(),
    /**
     * The week the weekly caps' open periods were made by (a `DayOfWeek`'s name); absent in a file
     * written before it was carried, where the restoring phone's week stands in.
     */
    val weekInForce: String? = null,
    val caps: List<CapDto> = emptyList(),
)

/** The reader's choices; not what belongs to the phone (the first run's flag, the asked permission). */
@Serializable
internal data class SettingsDto(
    val monthStartDay: Int = 1,
    val weekStart: String? = null,
    val theme: String? = null,
    val palette: String? = null,
    val font: String? = null,
    /** Absent in a file written before the setting was: the tiles stand in. */
    val capsLayout: String? = null,
    val dynamicColor: Boolean = false,
    val reminderOn: Boolean = false,
    val reminderTime: String? = null,
)

/** A cap, in the reader's order: its fields, then its spends and its closed lines. */
@Serializable
internal data class CapDto(
    val name: String = "",
    val icon: String = "",
    val amount: Long = 0,
    val period: String = "",
    val reserve: Long = 0,
    val openStart: String = "",
    val createdAtMillis: Long = 0,
    val spends: List<SpendDto> = emptyList(),
    val closed: List<ClosedDto> = emptyList(),
    /**
     * A one-off cap's end, the day after its last (owner, 6 Nov 2026); absent for every other
     * kind, and in a file written before one-off caps were. An added field: `version` stays 1.
     */
    val endExclusive: String? = null,
)

@Serializable
internal data class SpendDto(val amount: Long = 0, val addedAtMillis: Long = 0, val periodStart: String = "")

/** A closed period, or a run of empty ones folded into one line; its end is excluded. */
@Serializable
internal data class ClosedDto(
    val start: String = "",
    val endExclusive: String = "",
    val cap: Long = 0,
    val spent: Long = 0,
    val reserveBefore: Long = 0,
    val reserveAfter: Long = 0,
    val forgiven: Long = 0,
    val periods: Int = 1,
)
