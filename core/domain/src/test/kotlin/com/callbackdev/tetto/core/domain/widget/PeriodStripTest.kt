package com.callbackdev.tetto.core.domain.widget

import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.model.DateRange
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The cap's period under «In words» on a wide card: its days, a year's months, or nothing. */
class PeriodStripTest {
    @Test
    fun `a week is seven days, today among them`() {
        val week = DateRange(date(2026, 11, 9), date(2026, 11, 16))
        val strip = PeriodStrip.of(week, date(2026, 11, 12))
        assertThat(strip).isEqualTo(PeriodStrip(StripUnit.DAY, date(2026, 11, 9), 7, 3))
        assertThat(strip?.dayOf(6)).isEqualTo(date(2026, 11, 15))
    }

    @Test
    fun `a month is its days`() {
        val month = DateRange(date(2026, 11, 1), date(2026, 12, 1))
        assertThat(PeriodStrip.of(month, date(2026, 11, 30))?.count).isEqualTo(30)
        assertThat(PeriodStrip.of(month, date(2026, 11, 30))?.today).isEqualTo(29)
    }

    @Test
    fun `a year is its months`() {
        val year = DateRange(date(2026, 1, 1), date(2027, 1, 1))
        val strip = PeriodStrip.of(year, date(2026, 11, 14))
        assertThat(strip).isEqualTo(PeriodStrip(StripUnit.MONTH, date(2026, 1, 1), 12, 10))
        assertThat(strip?.dayOf(11)).isEqualTo(date(2026, 12, 1))
    }

    @Test
    fun `a day, or a one-off too long for days and months, has no strip`() {
        assertThat(PeriodStrip.of(DateRange(date(2026, 11, 14), date(2026, 11, 15)), date(2026, 11, 14))).isNull()
        assertThat(PeriodStrip.of(DateRange(date(2026, 1, 1), date(2027, 6, 1)), date(2026, 3, 1))).isNull()
    }

    @Test
    fun `today stays inside the strip`() {
        val week = DateRange(date(2026, 11, 9), date(2026, 11, 16))
        assertThat(PeriodStrip.of(week, date(2026, 11, 20))?.today).isEqualTo(6)
        assertThat(PeriodStrip.of(week, date(2026, 11, 1))?.today).isEqualTo(0)
    }
}
