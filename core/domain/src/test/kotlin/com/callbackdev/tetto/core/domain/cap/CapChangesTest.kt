package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek.SUNDAY

/**
 * The open period a change of rule puts in place (PLANNING.md §4.5; §4.7, Changes). The spends
 * moving with it are the repository's, tested there. Today is Thursday 5 Nov 2026.
 */
class CapChangesTest {
    private val today = date(2026, 11, 5)

    @Test
    fun `a new cap counts the whole period that holds today`() {
        assertThat(CapChanges.openStartForNew(rule(DAY), today)).isEqualTo(today)
        assertThat(CapChanges.openStartForNew(rule(WEEK), today)).isEqualTo(date(2026, 11, 2))
        assertThat(CapChanges.openStartForNew(rule(MONTH), today)).isEqualTo(date(2026, 11, 1))
        assertThat(CapChanges.openStartForNew(rule(YEAR), today)).isEqualTo(date(2026, 1, 1))
    }

    @Test
    fun `the kind mid-period, week to month`() {
        // The open week began on Monday the 2nd; the last closed week ended then.
        assertThat(CapChanges.replacedOpenStart(rule(MONTH), today, lastClosedEnd = date(2026, 11, 2)))
            .isEqualTo(date(2026, 11, 2))
        // With nothing closed yet, the whole month.
        assertThat(CapChanges.replacedOpenStart(rule(MONTH), today, lastClosedEnd = null))
            .isEqualTo(date(2026, 11, 1))
    }

    @Test
    fun `the kind mid-period, month to week`() {
        assertThat(CapChanges.replacedOpenStart(rule(WEEK), today, lastClosedEnd = date(2026, 11, 1)))
            .isEqualTo(date(2026, 11, 2))
    }

    @Test
    fun `the kind mid-period, to a day`() {
        assertThat(CapChanges.replacedOpenStart(rule(DAY), today, lastClosedEnd = date(2026, 11, 1))).isEqualTo(today)
    }

    @Test
    fun `a day to a week after closed days starts where they ended`() {
        // Monday to Wednesday closed as days: the week cannot reach back into them.
        assertThat(CapChanges.replacedOpenStart(rule(WEEK), today, lastClosedEnd = today)).isEqualTo(today)
    }

    @Test
    fun `the month start day changed`() {
        assertThat(
            CapChanges.replacedOpenStart(rule(MONTH, monthStartDay = 27), today, lastClosedEnd = date(2026, 11, 1)),
        )
            .isEqualTo(date(2026, 11, 1))
        assertThat(
            CapChanges.replacedOpenStart(rule(MONTH, monthStartDay = 3), today, lastClosedEnd = date(2026, 11, 1)),
        )
            .isEqualTo(date(2026, 11, 3))
        assertThat(CapChanges.replacedOpenStart(rule(MONTH, monthStartDay = 27), today, lastClosedEnd = null))
            .isEqualTo(date(2026, 10, 27))
    }

    @Test
    fun `the week start changed`() {
        assertThat(
            CapChanges.replacedOpenStart(rule(WEEK, weekStart = SUNDAY), today, lastClosedEnd = date(2026, 11, 2)),
        )
            .isEqualTo(date(2026, 11, 2))
        assertThat(
            CapChanges.replacedOpenStart(rule(WEEK, weekStart = SUNDAY), today, lastClosedEnd = date(2026, 10, 26)),
        )
            .isEqualTo(date(2026, 11, 1))
    }
}
