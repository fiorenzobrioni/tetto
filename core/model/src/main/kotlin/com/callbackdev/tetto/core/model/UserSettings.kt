package com.callbackdev.tetto.core.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * The reader's settings (PLANNING.md §5), as far as the phases built so far use them: the money's
 * (Phase 1), the appearance and the first run's flag (Phase 3), the evening reminder (Phase 4),
 * the caps screen's first-day tip (Phase 6).
 *
 * @property currency the ISO 4217 code chosen at the first run; null until then, when the
 *   phone's region's stands in.
 * @property monthStartDay the day a month starts, 1 to 31; a year starts on that day of January.
 * @property weekStart the day a week starts, once the reader picks one; null follows the locale.
 * @property theme light, dark, or the phone's.
 * @property palette the dress the app wears when it does not take the wallpaper's colours.
 * @property font the typeface.
 * @property capsLayout the caps screen's tiles or list (owner, 6 Nov 2026).
 * @property dynamicColor Material's roles from the wallpaper instead of the dress.
 * @property onboardingCompleted the first run is over: the shell shows the caps screen.
 * @property reminderOn the evening reminder (PLANNING.md §8), off until the reader turns it on.
 * @property reminderTime when it comes, the reader's local time, whatever the zone.
 * @property askedNotifications whether Tetto ever asked for the notification permission: Android
 *   says "no need to explain first" both before the first question and after a refusal it will
 *   not let the app repeat, and only this tells the two apart. A fact about this phone, so a
 *   backup does not carry it and erasing everything keeps it.
 * @property capsTipSeen whether the reader dismissed the caps screen's first-day tip ("How to
 *   read a cap") or opened the guide from it: shown over the tiles until then. What this phone
 *   has shown, like [askedNotifications]: a backup does not carry it, a restore and erasing
 *   everything keep it.
 * @property weekInForce the first day of the week the weekly caps' open periods were made by, the
 *   engine's own note, not a choice (PLANNING.md §4.5): while [weekStart] follows the phone, the
 *   phone's week can change under the caps (another language, a backup restored on a phone of
 *   another region), and the open periods are then moved to it as a change in Settings would move
 *   them, rather than judged by a week they were not made for. Null until the caps are first read.
 */
data class UserSettings(
    val currency: String? = null,
    val monthStartDay: Int = 1,
    val weekStart: DayOfWeek? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.VIVID,
    val font: AppFont = AppFont.GOOGLE_SANS,
    val capsLayout: CapsLayout = CapsLayout.TILES,
    val dynamicColor: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val reminderOn: Boolean = false,
    val reminderTime: LocalTime = DEFAULT_REMINDER_TIME,
    val askedNotifications: Boolean = false,
    val capsTipSeen: Boolean = false,
    val weekInForce: DayOfWeek? = null,
) {
    companion object {
        /** Half past eight in the evening: after most days' spending, before most days end. */
        val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(20, 30)
    }
}
