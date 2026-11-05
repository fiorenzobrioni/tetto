package com.callbackdev.tetto.core.data.settings

import com.callbackdev.tetto.core.data.time.PhoneDefaults
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.UserSettings

/** The app's currency: the reader's choice, else the phone's region's (PLANNING.md §4.1). */
internal fun UserSettings.resolvedCurrency(phone: PhoneDefaults): AppCurrency =
    currency?.let(AppCurrency::ofOrNull) ?: phone.currency()

/** The rule a cap of [kind] runs on: the reader's week and month starts, else the locale's week. */
internal fun UserSettings.rule(kind: PeriodKind, phone: PhoneDefaults): PeriodRule =
    PeriodRule(kind, weekStart ?: phone.weekStart(), monthStartDay)
