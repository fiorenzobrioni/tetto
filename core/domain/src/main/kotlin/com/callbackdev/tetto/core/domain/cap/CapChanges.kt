package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.period.PeriodCalendar
import com.callbackdev.tetto.core.model.PeriodRule
import java.time.LocalDate

/** The dates a change of cap or of settings decides (PLANNING.md §4.5). */
object CapChanges {
    /**
     * Where a new cap's open period starts: the start of the rule's period that holds today, so a
     * weekly cap made on a Thursday counts the whole week.
     */
    fun openStartForNew(rule: PeriodRule, today: LocalDate): LocalDate = PeriodCalendar.containing(today, rule).start

    /**
     * Where a cap's open period starts once its rule changes (its kind, or the week's or the
     * month's start day): the new rule's period that holds today replaces it, and the open spends
     * move into it. Never before [lastClosedEnd]: a closed period is never changed, so a new period
     * that would reach back into one starts where it ended (a daily cap turned weekly on a
     * Thursday, after closing Monday to Wednesday, counts Thursday to Sunday).
     */
    fun replacedOpenStart(rule: PeriodRule, today: LocalDate, lastClosedEnd: LocalDate?): LocalDate {
        val start = PeriodCalendar.containing(today, rule).start
        return if (lastClosedEnd != null && lastClosedEnd > start) lastClosedEnd else start
    }
}
