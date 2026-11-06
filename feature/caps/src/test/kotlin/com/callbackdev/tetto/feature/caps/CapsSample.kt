package com.callbackdev.tetto.feature.caps

import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.domain.cap.CapsSentence
import com.callbackdev.tetto.core.domain.cap.YourMonth
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Spend
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Realistic caps for the screens' tests, on Thursday 5 Nov 2026, in euros, weeks from Monday. */
internal object CapsSample {
    val today: LocalDate = LocalDate.of(2026, 11, 5)
    val EUR = AppCurrency("EUR", 2)
    val JPY = AppCurrency("JPY", 0)

    fun euros(whole: Long, cents: Long = 0) = Money(whole * 100 + cents)

    fun rule(kind: PeriodKind) = PeriodRule(kind, DayOfWeek.MONDAY, 1)

    fun state(
        id: Long,
        name: String,
        icon: CapIcon,
        amount: Money,
        kind: PeriodKind,
        spent: Money,
        reserve: Money = Money.ZERO,
        lastClosedEnd: LocalDate? = null,
        currency: AppCurrency = EUR,
    ): CapState = CapStates.of(
        cap = Cap(
            id = id,
            name = name,
            icon = icon,
            amount = amount,
            kind = kind,
            reserve = reserve,
            openStart = CapChanges.openStartForNew(rule(kind), today),
            position = id.toInt(),
        ),
        spent = spent,
        rule = rule(kind),
        today = today,
        currency = currency,
        lastClosedEnd = lastClosedEnd,
    )

    val coffee = state(1, "Coffee", CapIcon.COFFEE, euros(20), PeriodKind.WEEK, euros(8), reserve = euros(4))
    val groceries = state(2, "Groceries", CapIcon.GROCERIES, euros(300), PeriodKind.MONTH, euros(96, 40))
    val restaurants = state(3, "Restaurants", CapIcon.EATING_OUT, euros(120), PeriodKind.MONTH, euros(108))
    val shopping = state(4, "Shopping", CapIcon.SHOPPING, euros(100), PeriodKind.MONTH, euros(130))
    val lunch = state(5, "Lunch", CapIcon.EATING_OUT, euros(12), PeriodKind.DAY, euros(4, 50))

    val underCaps = listOf(coffee, groceries, restaurants, lunch)

    fun ui(
        caps: List<CapState>,
        currency: AppCurrency = EUR,
        layout: CapsLayout = CapsLayout.TILES,
        month: YourMonth? = null,
    ) = CapsUiState(today, currency, caps, CapsSentence.of(caps), layout = layout, month = month)

    /** November from the 1st: €842 added so far, €1,130 in October, caps worth about €1,250. */
    fun month(added: Money = euros(842), addedLastMonth: Money = euros(1130)) = YourMonth(
        month = DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1)),
        added = added,
        lastMonth = DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1)),
        addedLastMonth = addedLastMonth,
        worth = euros(1250),
    )

    private val zone = ZoneId.of("Europe/Rome")

    fun spend(
        id: Long,
        amount: Money,
        local: String,
        capId: Long = 1,
        periodStart: LocalDate = LocalDate.of(2026, 11, 2),
    ) = Spend(id, capId, amount, java.time.LocalDateTime.parse(local).atZone(zone).toInstant(), periodStart)

    val coffeeSpends = listOf(
        spend(3, euros(3, 20), "2026-11-05T08:40"),
        spend(2, euros(2, 80), "2026-11-04T15:10"),
        spend(1, euros(2), "2026-11-02T09:05"),
    )

    /** The reserve's story of [coffee]'s €4, the latest first. */
    val coffeeLines = listOf(
        line(
            LocalDate.of(2026, 10, 26),
            LocalDate.of(2026, 11, 2),
            spent = euros(16),
            before = euros(0),
            after = euros(4),
        ),
        line(
            LocalDate.of(2026, 10, 19),
            LocalDate.of(2026, 10, 26),
            spent = euros(26),
            before = euros(6),
            after = euros(0),
        ),
        line(
            LocalDate.of(2026, 10, 12),
            LocalDate.of(2026, 10, 19),
            spent = euros(14),
            before = euros(0),
            after = euros(6),
        ),
    )

    fun line(
        start: LocalDate,
        end: LocalDate,
        spent: Money,
        before: Money,
        after: Money,
        cap: Money = euros(20),
        forgiven: Money = Money.ZERO,
        periods: Int = 1,
    ) = ClosedPeriod(1, DateRange(start, end), cap, spent, before, after, forgiven, periods)
}
