package com.callbackdev.tetto.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The first run's one write (PLANNING.md §11 Phase 3): the currency, the reader's caps and the
 * flag that ends the run, in that order ([SettingsRepository.finishFirstRun]). The shell turns to
 * the caps screen as soon as the flag is stored.
 */
@HiltViewModel
class OnboardingViewModel
@Inject
constructor(private val settings: SettingsRepository) : ViewModel() {
    /** The currency the run starts with: one stored already, else the phone's region's. */
    val currency: StateFlow<AppCurrency?> =
        settings.currency.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private val finishing = MutableStateFlow(false)

    /** True from the first touch on Done: a second touch never makes the caps twice. */
    val isFinishing: StateFlow<Boolean> = finishing.asStateFlow()

    fun finish(currency: String, drafts: List<CapDraft>) {
        if (!finishing.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch { settings.finishFirstRun(currency, drafts) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
