package com.callbackdev.tetto.core.model

import java.time.DayOfWeek

/**
 * The reader's settings (PLANNING.md §5), as far as the phases built so far use them: the money's.
 * The appearance, the reminder and the first run's flags join with the phases that bring them.
 *
 * @property currency the ISO 4217 code chosen at the first run; null until then, when the
 *   phone's region's stands in.
 * @property monthStartDay the day a month starts, 1 to 31; a year starts on that day of January.
 * @property weekStart the day a week starts, once the reader picks one; null follows the locale.
 */
data class UserSettings(val currency: String? = null, val monthStartDay: Int = 1, val weekStart: DayOfWeek? = null)
