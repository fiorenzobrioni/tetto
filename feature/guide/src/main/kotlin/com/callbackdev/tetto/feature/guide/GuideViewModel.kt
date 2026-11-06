package com.callbackdev.tetto.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.model.AppCurrency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * What the guide's examples take from the reader's settings: the currency their figures are written
 * in, and the week their days are named by ("until Sunday" is the last day of the reader's week).
 * The guide reads nothing else, and writes nothing.
 */
data class GuideUiState(val currency: AppCurrency, val weekStart: DayOfWeek)

@HiltViewModel
class GuideViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<GuideUiState?> = combine(settings.currency, settings.weekStart, ::GuideUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
