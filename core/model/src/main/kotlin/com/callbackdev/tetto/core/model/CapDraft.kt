package com.callbackdev.tetto.core.model

/**
 * A cap before it is one: its four fields (VISION, «New and edited caps»), as the editor holds them
 * and as the first run makes its caps; for a one-off cap, its [dates] too (owner, 6 Nov 2026),
 * null for every other kind.
 */
data class CapDraft(
    val name: String,
    val amount: Money,
    val kind: PeriodKind,
    val icon: CapIcon,
    val dates: DateRange? = null,
)
