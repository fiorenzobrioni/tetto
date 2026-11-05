package com.callbackdev.tetto.core.model

import java.time.DayOfWeek

/**
 * The reader's settings (PLANNING.md §5), as far as the phases built so far use them: the money's
 * (Phase 1), the appearance and the first run's flag (Phase 3). The reminder joins with Phase 4,
 * the caps screen's first-day tip with Phase 6.
 *
 * @property currency the ISO 4217 code chosen at the first run; null until then, when the
 *   phone's region's stands in.
 * @property monthStartDay the day a month starts, 1 to 31; a year starts on that day of January.
 * @property weekStart the day a week starts, once the reader picks one; null follows the locale.
 * @property theme light, dark, or the phone's.
 * @property palette the dress the app wears when it does not take the wallpaper's colours.
 * @property font the typeface.
 * @property dynamicColor Material's roles from the wallpaper instead of the dress.
 * @property onboardingCompleted the first run is over: the shell shows the caps screen.
 */
data class UserSettings(
    val currency: String? = null,
    val monthStartDay: Int = 1,
    val weekStart: DayOfWeek? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.VIVID,
    val font: AppFont = AppFont.GOOGLE_SANS,
    val dynamicColor: Boolean = false,
    val onboardingCompleted: Boolean = false,
)
