package com.callbackdev.tetto.core.data.settings

import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The settings, as the screens read and change them (PLANNING.md §5). A change of week or month
 * start goes through [CapsRepository], which knows what it does to the caps' open periods (§4.5).
 */
@Singleton
class SettingsRepository
@Inject
constructor(
    preferences: UserPreferencesDataSource,
    private val caps: CapsRepository,
    private val phone: PhoneDefaults,
) {
    val settings: Flow<UserSettings> = preferences.settings

    /** The app's currency: the reader's choice, else the phone's region's. */
    val currency: Flow<AppCurrency> = settings.map { it.resolvedCurrency(phone) }.distinctUntilChanged()

    /** The first day of the week in force: the reader's, else the locale's. */
    val weekStart: Flow<DayOfWeek> = settings.map { it.weekStart ?: phone.weekStart() }.distinctUntilChanged()

    suspend fun setWeekStart(weekStart: DayOfWeek?): UserSettings = caps.changeWeekStart(weekStart)

    suspend fun setMonthStartDay(day: Int): UserSettings = caps.changeMonthStartDay(day)
}
