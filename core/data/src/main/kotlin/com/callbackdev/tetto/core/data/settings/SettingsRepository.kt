package com.callbackdev.tetto.core.data.settings

import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The settings, as the screens read and change them (PLANNING.md §5). A change of currency, week
 * or month start goes through [CapsRepository], which knows what it does to the caps (§4.1, §4.5);
 * the appearance touches nothing else, and has a setter each, so no screen can write a money
 * setting past the caps.
 */
@Singleton
class SettingsRepository
@Inject
constructor(
    private val preferences: UserPreferencesDataSource,
    private val caps: CapsRepository,
    private val phone: PhoneDefaults,
) {
    val settings: Flow<UserSettings> = preferences.settings

    /** The app's currency: the reader's choice, else the phone's region's. */
    val currency: Flow<AppCurrency> = settings.map { it.resolvedCurrency(phone) }.distinctUntilChanged()

    /** The first day of the week in force: the reader's, else the locale's. */
    val weekStart: Flow<DayOfWeek> = settings.map { it.weekStart ?: phone.weekStart() }.distinctUntilChanged()

    /** What the phone says where the reader has not chosen: its region's currency, its locale's week. */
    fun phoneCurrency(): AppCurrency = phone.currency()

    fun phoneWeekStart(): DayOfWeek = phone.weekStart()

    suspend fun setCurrency(code: String): UserSettings = caps.changeCurrency(code)

    suspend fun setWeekStart(weekStart: DayOfWeek?): UserSettings = caps.changeWeekStart(weekStart)

    suspend fun setMonthStartDay(day: Int): UserSettings = caps.changeMonthStartDay(day)

    suspend fun setTheme(theme: ThemeMode) {
        preferences.update { it.copy(theme = theme) }
    }

    suspend fun setPalette(palette: AppPalette) {
        preferences.update { it.copy(palette = palette) }
    }

    suspend fun setFont(font: AppFont) {
        preferences.update { it.copy(font = font) }
    }

    suspend fun setCapsLayout(layout: CapsLayout) {
        preferences.update { it.copy(capsLayout = layout) }
    }

    suspend fun setDynamicColor(on: Boolean) {
        preferences.update { it.copy(dynamicColor = on) }
    }

    /** The evening reminder on or off (PLANNING.md §8); its alarm follows the setting by itself. */
    suspend fun setReminder(on: Boolean) {
        preferences.update { it.copy(reminderOn = on) }
    }

    suspend fun setReminderTime(time: LocalTime) {
        preferences.update { it.copy(reminderTime = time) }
    }

    /** Tetto asked for the notification permission: from now on, a refusal may be for good. */
    suspend fun noteNotificationsAsked() {
        preferences.update { it.copy(askedNotifications = true) }
    }

    /** The caps screen's first-day tip was dismissed, or followed to the guide: never shown again. */
    suspend fun noteCapsTipSeen() {
        preferences.update { it.copy(capsTipSeen = true) }
    }

    /**
     * The first run's end (PLANNING.md §11 Phase 3): the currency stored, so a later change of the
     * phone's region relabels nothing by itself (§4.1); the reader's caps, together; then the flag,
     * last, so the caps screen the shell turns to is already shaped by them, never empty first.
     */
    suspend fun finishFirstRun(currency: String, drafts: List<CapDraft>) {
        caps.changeCurrency(currency)
        if (drafts.isNotEmpty()) caps.addCaps(drafts)
        preferences.update { it.copy(onboardingCompleted = true) }
    }
}
