package com.callbackdev.tetto.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.time.TodaySource
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
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
import javax.inject.Inject

/**
 * What Settings shows: the settings as stored, the currency and the week in force (the reader's,
 * else the phone's), today (for the month's dates), how many caps there are (a change of currency
 * asks first when there are some to relabel), and the version.
 */
data class SettingsUiState(
    val settings: UserSettings,
    val currency: AppCurrency,
    val phoneWeekStart: DayOfWeek,
    val today: LocalDate,
    val capCount: Int,
    val version: String,
) {
    val weekStart: DayOfWeek get() = settings.weekStart ?: phoneWeekStart
}

/**
 * Settings (PLANNING.md §11 Phase 3). The money's three settings go through the caps' repository,
 * which moves the open periods or rescales the amounts with them (§4.1, §4.5); the appearance is
 * written as it is chosen, and the app wears it at once.
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    private val repository: SettingsRepository,
    caps: CapsRepository,
    private val clock: TodaySource,
) : ViewModel() {
    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    val state: StateFlow<SettingsUiState?> =
        combine(
            repository.settings,
            repository.currency,
            caps.states.map { it.size }.distinctUntilChanged(),
        ) { settings, currency, count ->
            SettingsUiState(settings, currency, repository.phoneWeekStart(), clock.today(), count, version)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun setCurrency(code: String) = launch { repository.setCurrency(code) }

    fun setMonthStartDay(day: Int) = launch { repository.setMonthStartDay(day) }

    fun setWeekStart(day: DayOfWeek?) = launch { repository.setWeekStart(day) }

    fun setTheme(theme: ThemeMode) = launch { repository.setTheme(theme) }

    fun setPalette(palette: AppPalette) = launch { repository.setPalette(palette) }

    fun setFont(font: AppFont) = launch { repository.setFont(font) }

    fun setDynamicColor(on: Boolean) = launch { repository.setDynamicColor(on) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
