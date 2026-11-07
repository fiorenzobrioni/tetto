package com.callbackdev.tetto.widget

import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.DayOfWeek
import java.time.LocalDate

/** A one-off cap for the cards' tests (owner, 6 Nov 2026), on the samples' Wednesday 11 Nov 2026. */
internal object WidgetSamplesForTests {
    private val euro = AppCurrency("EUR", 2)
    private val rule = PeriodRule(PeriodKind.ONE_OFF, DayOfWeek.MONDAY, 1)

    /**
     * Lisbon, €800: [running] from Friday 6 to Sunday 15 Nov, €420 spent; else from 1 to 10 Nov,
     * ended yesterday at €720.
     */
    fun lisbon(running: Boolean): CapState {
        val today = WidgetSamples.Today
        val dates = if (running) {
            DateRange(LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 16))
        } else {
            DateRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 11))
        }
        val line = ClosedPeriod(9, dates, Money(80_000), Money(72_000), Money.ZERO, Money.ZERO, Money.ZERO, 1)
        return CapStates.of(
            cap = Cap(
                id = 9,
                name = "Lisbon trip",
                icon = CapIcon.TRAVEL,
                amount = Money(80_000),
                kind = PeriodKind.ONE_OFF,
                reserve = Money.ZERO,
                openStart = if (running) dates.start else dates.endExclusive,
                position = 0,
                endExclusive = dates.endExclusive,
            ),
            spent = Money(42_000),
            rule = rule,
            today = today,
            currency = euro,
            lastClosedEnd = if (running) null else dates.endExclusive,
            lastClosed = if (running) null else line,
        )
    }
}
