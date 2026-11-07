package com.callbackdev.tetto.feature.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.lock.AppLock
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.data.widget.HomeCard
import com.callbackdev.tetto.core.data.widget.HomeScreenCards
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.reminder.EveningReminder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * What Settings shows: the settings as stored, the currency and the week in force (the reader's,
 * else the phone's), today (for the month's dates), how many caps there are (a change of currency
 * asks first when there are some to relabel), the version, and whether the launcher can place a
 * card on request ([canPinCards]: the home screen's group is drawn only then).
 */
data class SettingsUiState(
    val settings: UserSettings,
    val currency: AppCurrency,
    val phoneWeekStart: DayOfWeek,
    val today: LocalDate,
    val capCount: Int,
    val version: String,
    val canPinCards: Boolean = false,
) {
    val weekStart: DayOfWeek get() = settings.weekStart ?: phoneWeekStart
}

/**
 * Settings (PLANNING.md §11 Phases 3 and 4). The money's three settings go through the caps'
 * repository, which moves the open periods or rescales the amounts with them (§4.1, §4.5); the
 * appearance is written as it is chosen, and the app wears it at once; the reminder's alarm
 * follows its setting by itself, and hears when the notifications may have changed.
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val repository: SettingsRepository,
    caps: CapsRepository,
    private val clock: TodaySource,
    private val reminder: EveningReminder,
    private val cards: HomeScreenCards,
    private val lock: AppLock,
) : ViewModel() {
    private val canPin: Boolean = cards.canPin()

    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    val state: StateFlow<SettingsUiState?> =
        combine(
            repository.settings,
            repository.currency,
            caps.states.map { it.size }.distinctUntilChanged(),
        ) { settings, currency, count ->
            SettingsUiState(settings, currency, repository.phoneWeekStart(), clock.today(), count, version, canPin)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setCurrency(code: String) = launch { repository.setCurrency(code) }

    fun setMonthStartDay(day: Int) = launch { repository.setMonthStartDay(day) }

    fun setWeekStart(day: DayOfWeek?) = launch { repository.setWeekStart(day) }

    fun setTheme(theme: ThemeMode) = launch { repository.setTheme(theme) }

    fun setPalette(palette: AppPalette) = launch { repository.setPalette(palette) }

    fun setFont(font: AppFont) = launch { repository.setFont(font) }

    fun setCapsLayout(layout: CapsLayout) = launch { repository.setCapsLayout(layout) }

    fun setDynamicColor(on: Boolean) = launch { repository.setDynamicColor(on) }

    fun setReminder(on: Boolean) = launch { repository.setReminder(on) }

    fun setReminderTime(time: LocalTime) = launch { repository.setReminderTime(time) }

    /** Whether the phone has a screen lock for Tetto's lock to ask for (read again on each return). */
    fun lockAvailable(): Boolean = lock.available()

    /**
     * The lock on or off (owner, 6 Nov 2026). On, at once: the reader is here, and chose it. Off,
     * only once the phone's own lock says yes over [activity] (with [title]): a borrowed phone does
     * not turn it off; on a phone that no longer has a screen lock, there is nothing to ask.
     */
    suspend fun setAppLock(on: Boolean, activity: Activity?, title: String) {
        if (on) {
            lock.markUnlocked()
            repository.setAppLock(true)
        } else if (!lock.available() || (activity != null && lock.ask(activity, title))) {
            repository.setAppLock(false)
        }
    }

    /** Asks the launcher to place [card] (PLANNING.md §8): it shows its own dialog, and the reader decides there. */
    fun pinCard(card: HomeCard) {
        cards.pin(card)
    }

    /** Android is about to ask for the permission: from now on, a refusal may be for good. */
    fun noteNotificationsAsked() = launch { repository.noteNotificationsAsked() }

    /** Back from the permission's question or the system's page: the alarm follows what changed. */
    fun reminderAccessMayHaveChanged() = launch { reminder.accessMayHaveChanged() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
