package com.callbackdev.tetto.feature.onboarding

import com.callbackdev.tetto.core.domain.money.Rescale
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind

/**
 * The first run's areas (VISION, «First run»): common areas of spending one can steer, the cap
 * palette's first twelve. Bills are left out on purpose: a fixed bill is what it is, and a cap is
 * for spending one can steer; any area can be added later, bills included.
 */
val FirstRunAreas: List<CapIcon> = listOf(
    CapIcon.GROCERIES,
    CapIcon.EATING_OUT,
    CapIcon.COFFEE,
    CapIcon.SHOPPING,
    CapIcon.FUN,
    CapIcon.TRANSPORT,
    CapIcon.FUEL,
    CapIcon.GIFTS,
    CapIcon.PERSONAL_CARE,
    CapIcon.PETS,
    CapIcon.TRAVEL,
    CapIcon.HOME,
)

/** One area picked, with what the reader gave it: an amount (none yet) and a period, a month by default. */
data class AreaPick(val area: CapIcon, val amount: Money = Money.ZERO, val kind: PeriodKind = PeriodKind.MONTH)

/**
 * What the first run has gathered: the areas picked, in the grid's order (the caps screen's order
 * to come), and the currency, once the reader changes it (null: the one the run started with).
 */
data class FirstRunPicks(val areas: List<AreaPick> = emptyList(), val currency: String? = null) {
    fun isPicked(area: CapIcon): Boolean = areas.any { it.area == area }

    /** Every area picked has an amount: the caps can be made. */
    val complete: Boolean get() = areas.isNotEmpty() && areas.all { it.amount.isPositive }

    /** [area] picked, or unpicked; a picked area keeps the grid's place, and what was typed for it is dropped. */
    fun toggle(area: CapIcon): FirstRunPicks = copy(
        areas = if (isPicked(area)) {
            areas.filterNot { it.area == area }
        } else {
            (areas + AreaPick(area)).sortedBy { FirstRunAreas.indexOf(it.area) }
        },
    )

    fun withAmount(area: CapIcon, amount: Money): FirstRunPicks =
        copy(areas = areas.map { if (it.area == area) it.copy(amount = amount) else it })

    fun withKind(area: CapIcon, kind: PeriodKind): FirstRunPicks =
        copy(areas = areas.map { if (it.area == area) it.copy(kind = kind) else it })

    /**
     * The currency becomes [to]: what was typed keeps its figure, rescaled where the decimals
     * differ, as a change of currency does to stored caps (PLANNING.md §4.1): €12.50 becomes ¥13.
     */
    fun withCurrency(from: AppCurrency, to: AppCurrency): FirstRunPicks = copy(
        currency = to.code,
        areas = areas.map { it.copy(amount = Rescale.amount(it.amount, from.fractionDigits, to.fractionDigits)) },
    )
}
