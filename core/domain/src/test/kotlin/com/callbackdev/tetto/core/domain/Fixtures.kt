package com.callbackdev.tetto.core.domain

import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.DayOfWeek
import java.time.LocalDate

val EUR = AppCurrency("EUR", 2)
val JPY = AppCurrency("JPY", 0)
val KWD = AppCurrency("KWD", 3)

/** Euros and cents, written as a reader says them: `euros(12, 50)` is €12.50. */
fun euros(whole: Long, cents: Long = 0): Money = Money(whole * 100 + cents)

fun rule(kind: PeriodKind, weekStart: DayOfWeek = DayOfWeek.MONDAY, monthStartDay: Int = 1) =
    PeriodRule(kind, weekStart, monthStartDay)

fun date(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day)

fun cap(
    kind: PeriodKind,
    openStart: LocalDate,
    amount: Money = euros(20),
    reserve: Money = Money.ZERO,
    id: Long = 1,
    name: String = "Coffee",
    position: Int = 0,
) = Cap(
    id = id,
    name = name,
    icon = CapIcon.COFFEE,
    amount = amount,
    kind = kind,
    reserve = reserve,
    openStart = openStart,
    position = position,
)

/** A one-off cap (owner, 6 Nov 2026): its own dates, [start] to [endExclusive], the day after its last. */
fun oneOff(
    start: LocalDate,
    endExclusive: LocalDate,
    amount: Money = euros(800),
    id: Long = 1,
    name: String = "Lisbon trip",
    position: Int = 0,
    openStart: LocalDate = start,
) = Cap(
    id = id,
    name = name,
    icon = CapIcon.TRAVEL,
    amount = amount,
    kind = PeriodKind.ONE_OFF,
    reserve = Money.ZERO,
    openStart = openStart,
    position = position,
    endExclusive = endExclusive,
)
