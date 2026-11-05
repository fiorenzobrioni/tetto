package com.callbackdev.tetto.core.model

/**
 * A cap before it is one: its four fields (VISION, «New and edited caps»), as the editor holds them
 * and as the first run makes its caps.
 */
data class CapDraft(val name: String, val amount: Money, val kind: PeriodKind, val icon: CapIcon)
