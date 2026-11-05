package com.callbackdev.tetto.core.data.db

import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Spend
import java.time.Instant
import java.time.LocalDate

internal fun CapEntity.toModel(): Cap = Cap(
    id = id,
    name = name,
    icon = CapIcon.fromKey(icon),
    amount = Money(amount),
    kind = PeriodKind.valueOf(kind),
    reserve = Money(reserve),
    openStart = LocalDate.ofEpochDay(openStart),
    position = position,
)

internal fun SpendEntity.toModel(): Spend = Spend(
    id = id,
    capId = capId,
    amount = Money(amount),
    addedAt = Instant.ofEpochMilli(addedAt),
    periodStart = LocalDate.ofEpochDay(periodStart),
)

internal fun Spend.toEntity(): SpendEntity = SpendEntity(
    id = id,
    capId = capId,
    amount = amount.minor,
    addedAt = addedAt.toEpochMilli(),
    periodStart = periodStart.toEpochDay(),
)

internal fun ClosedPeriodEntity.toModel(): ClosedPeriod = ClosedPeriod(
    capId = capId,
    range = DateRange(LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(endExclusive)),
    cap = Money(cap),
    spent = Money(spent),
    reserveBefore = Money(reserveBefore),
    reserveAfter = Money(reserveAfter),
    forgiven = Money(forgiven),
    periods = periods,
)

internal fun ClosedPeriod.toEntity(): ClosedPeriodEntity = ClosedPeriodEntity(
    capId = capId,
    start = range.start.toEpochDay(),
    endExclusive = range.endExclusive.toEpochDay(),
    cap = cap.minor,
    spent = spent.minor,
    reserveBefore = reserveBefore.minor,
    reserveAfter = reserveAfter.minor,
    forgiven = forgiven.minor,
    periods = periods,
)
