package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.JPY
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.PeriodKind.YEAR
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The reader's month at the end of the caps screen (owner, 6 Nov 2026): its days and the caps' worth. */
class MonthSumsTest {
    private val start = date(2026, 11, 1)

    @Test
    fun `the month from the 1st, and the one before`() {
        val month = MonthSums.month(date(2026, 11, 6), 1)
        assertThat(month).isEqualTo(DateRange(date(2026, 11, 1), date(2026, 12, 1)))
        assertThat(MonthSums.previous(month, 1)).isEqualTo(DateRange(date(2026, 10, 1), date(2026, 11, 1)))
    }

    @Test
    fun `a salary's month, from the 27th, before and after the day`() {
        assertThat(MonthSums.month(date(2026, 11, 6), 27))
            .isEqualTo(DateRange(date(2026, 10, 27), date(2026, 11, 27)))
        val after = MonthSums.month(date(2026, 11, 27), 27)
        assertThat(after).isEqualTo(DateRange(date(2026, 11, 27), date(2026, 12, 27)))
        assertThat(MonthSums.previous(after, 27)).isEqualTo(DateRange(date(2026, 10, 27), date(2026, 11, 27)))
    }

    @Test
    fun `from the 31st, February's month starts on its last day`() {
        val march = MonthSums.month(date(2027, 3, 10), 31)
        assertThat(march).isEqualTo(DateRange(date(2027, 2, 28), date(2027, 3, 31)))
        assertThat(MonthSums.previous(march, 31)).isEqualTo(DateRange(date(2027, 1, 31), date(2027, 2, 28)))
    }

    @Test
    fun `a month's cap is its amount, a day's counts each day, a week's its days over seven, a year's a twelfth`() {
        val november = MonthSums.month(date(2026, 11, 6), 1) // 30 days
        fun worth(kind: PeriodKind, amount: Money) = MonthSums.worth(listOf(cap(kind, start, amount)), november, EUR)

        assertThat(worth(MONTH, euros(300))).isEqualTo(euros(300))
        assertThat(worth(DAY, euros(12))).isEqualTo(euros(360))
        // €20 a week over 30 days: 85.71, about €86.
        assertThat(worth(WEEK, euros(20))).isEqualTo(euros(86))
        assertThat(worth(YEAR, euros(1500))).isEqualTo(euros(125))
    }

    @Test
    fun `a longer month makes a day's and a week's worth more`() {
        val december = MonthSums.month(date(2026, 12, 6), 1) // 31 days
        val caps = listOf(cap(DAY, start, euros(10), id = 1), cap(WEEK, start, euros(70), id = 2))
        assertThat(MonthSums.worth(caps, december, EUR)).isEqualTo(euros(310 + 310))
    }

    @Test
    fun `the caps together, rounded to the whole unit, and the reserves left out`() {
        val november = MonthSums.month(date(2026, 11, 6), 1)
        val caps = listOf(
            cap(MONTH, start, euros(200), id = 1),
            cap(MONTH, start, euros(450), id = 2),
            cap(MONTH, start, euros(99, 60), reserve = euros(80), id = 3),
            cap(WEEK, start, euros(20), id = 4),
        )
        // 200 + 450 + 99.60 + 85.71 = 835.31
        assertThat(MonthSums.worth(caps, november, EUR)).isEqualTo(euros(835))
    }

    @Test
    fun `a currency without decimals rounds to the yen`() {
        val november = MonthSums.month(date(2026, 11, 6), 1)
        val caps = listOf(cap(WEEK, start, Money(1000)))
        assertThat(MonthSums.worth(caps, november, JPY)).isEqualTo(Money(4286))
    }

    @Test
    fun `no caps are worth nothing`() {
        val november = MonthSums.month(date(2026, 11, 6), 1)
        assertThat(MonthSums.worth(emptyList(), november, EUR)).isEqualTo(Money.ZERO)
    }
}
