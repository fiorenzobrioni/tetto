package com.callbackdev.tetto.core.domain.backup

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** An instant in Rome, written as a reader there says it: `rome("2026-11-03T08:15")`. */
internal fun rome(local: String): Instant = LocalDateTime.parse(local).atZone(ZoneId.of("Europe/Rome")).toInstant()

/**
 * Two caps as a reader in Rome has them on Thursday 5 Nov 2026: Coffee, weekly from Monday, with a
 * week closed and €4 in reserve; Groceries, monthly from the 27th, with an October that went over.
 */
internal fun sampleBackup(): Backup = Backup(
    exportedAt = rome("2026-11-05T21:14"),
    zone = "Europe/Rome",
    appVersion = "0.1.0",
    currency = EUR,
    settings = UserSettings(
        currency = "EUR",
        monthStartDay = 27,
        weekStart = DayOfWeek.MONDAY,
        theme = ThemeMode.DARK,
        palette = AppPalette.PAPER,
        font = AppFont.INTER,
        capsLayout = CapsLayout.LIST,
        dynamicColor = true,
        reminderOn = true,
        reminderTime = LocalTime.of(21, 0),
    ),
    caps = listOf(
        BackupCap(
            name = "Coffee",
            icon = CapIcon.COFFEE,
            amount = euros(20),
            kind = PeriodKind.WEEK,
            reserve = euros(4),
            openStart = date(2026, 11, 2),
            createdAt = rome("2026-10-26T09:00"),
            spends = listOf(
                BackupSpend(euros(16), rome("2026-10-28T08:30"), date(2026, 10, 26)),
                BackupSpend(euros(1, 30), rome("2026-11-03T08:15"), date(2026, 11, 2)),
                BackupSpend(euros(2, 70), rome("2026-11-05T16:40"), date(2026, 11, 2)),
            ),
            closed = listOf(
                BackupClosed(
                    range = DateRange(date(2026, 10, 26), date(2026, 11, 2)),
                    cap = euros(20),
                    spent = euros(16),
                    reserveBefore = Money.ZERO,
                    reserveAfter = euros(4),
                    forgiven = Money.ZERO,
                    periods = 1,
                ),
            ),
        ),
        BackupCap(
            name = "Groceries, market",
            icon = CapIcon.GROCERIES,
            amount = euros(300),
            kind = PeriodKind.MONTH,
            reserve = Money.ZERO,
            openStart = date(2026, 10, 27),
            createdAt = rome("2026-09-27T10:00"),
            spends = listOf(
                BackupSpend(euros(330), rome("2026-10-20T18:00"), date(2026, 9, 27)),
                BackupSpend(euros(42, 5), rome("2026-11-01T11:00"), date(2026, 10, 27)),
            ),
            closed = listOf(
                BackupClosed(
                    range = DateRange(date(2026, 9, 27), date(2026, 10, 27)),
                    cap = euros(300),
                    spent = euros(330),
                    reserveBefore = Money.ZERO,
                    reserveAfter = Money.ZERO,
                    forgiven = euros(30),
                    periods = 1,
                ),
            ),
        ),
    ),
)
