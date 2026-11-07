package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
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

    @Test
    fun `a longer period holds the old one's start, nothing closes, and the reserve is held to the new cap`() {
        val month = cap(MONTH, openStart = date(2026, 11, 1), amount = euros(300), reserve = euros(260))
        val replaced = CapChanges.replace(month, rule(MONTH), date(2026, 11, 1), euros(80), spentBefore = Money.ZERO)
        assertThat(replaced).isEqualTo(Replacement(date(2026, 11, 1), euros(80), closed = null))
        val small = month.copy(reserve = euros(30))
        assertThat(CapChanges.replace(small, rule(MONTH), date(2026, 10, 26), euros(80), Money.ZERO).reserve)
            .isEqualTo(euros(30))
    }

    @Test
    fun `a shorter period keeps the days before it as a line, on their share of the cap`() {
        val week = cap(WEEK, openStart = date(2026, 11, 2), amount = euros(20), reserve = euros(2))
        val replaced = CapChanges.replace(week, rule(WEEK), today, euros(3), spentBefore = euros(4))
        assertThat(replaced.openStart).isEqualTo(today)
        val kept = checkNotNull(replaced.closed)
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 2), today))
        // Three days of seven of €20, rounded down: €8.57.
        assertThat(kept.cap).isEqualTo(euros(8, 57))
        assertThat(kept.spent).isEqualTo(euros(4))
        assertThat(kept.reserveBefore).isEqualTo(euros(2))
        // €6.57 left over, held to the day's €3.
        assertThat(kept.reserveAfter).isEqualTo(euros(3))
        assertThat(kept.periods).isEqualTo(1)
        assertThat(replaced.reserve).isEqualTo(euros(3))
    }

    @Test
    fun `a period already cut short shares out its own days`() {
        // An open period from Thursday to Sunday (four days, after closed days), cut on Saturday:
        // its own share is four sevenths of €20, €11.42, and Thursday and Friday half of it,
        // €5.71, two sevenths of the week's cap (owner, 7 Nov 2026).
        val rest = cap(WEEK, openStart = today, amount = euros(20))
        val kept = checkNotNull(CapChanges.replace(rest, rule(WEEK), date(2026, 11, 7), euros(20), euros(1)).closed)
        assertThat(kept.cap).isEqualTo(euros(5, 71))
    }

    @Test
    fun `the amount changed holds the reserve to it`() {
        assertThat(CapChanges.heldReserve(euros(20), euros(12))).isEqualTo(euros(12))
        assertThat(CapChanges.heldReserve(euros(8), euros(12))).isEqualTo(euros(8))
    }
}
