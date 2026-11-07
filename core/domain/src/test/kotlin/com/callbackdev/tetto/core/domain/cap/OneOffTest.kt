package com.callbackdev.tetto.core.domain.cap

import com.callbackdev.tetto.core.domain.EUR
import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.oneOff
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.domain.widget.CapNote
import com.callbackdev.tetto.core.domain.widget.WidgetCaps
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.ClosedPeriod
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.callbackdev.tetto.core.model.PeriodKind.MONTH
import com.callbackdev.tetto.core.model.PeriodKind.ONE_OFF
import com.callbackdev.tetto.core.model.PeriodKind.WEEK
import com.callbackdev.tetto.core.model.Share
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

/**
 * The one-off cap (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): its own dates, no reserve, no
 * renewal; a mark, a pace and «ahead of the days» over its days; one closed line at its end, and
 * then the cap has ended. Every edge case in §4.7's manner.
 */
class OneOffTest {
    /** Lisbon, Tuesday 10 to Friday 20 Nov 2026: eleven days, €800. */
    private val lisbon = oneOff(start = date(2026, 11, 10), endExclusive = date(2026, 11, 21))

    private fun state(cap: Cap, spent: Money, today: LocalDate, line: ClosedPeriod? = null) = CapStates.of(
        cap,
        spent,
        rule(ONE_OFF),
        today,
        EUR,
        lastClosedEnd = line?.range?.endExclusive,
        lastClosed = line,
    )

    private fun Cap.after(plan: CatchUpPlan) = copy(openStart = plan.openStart, reserve = plan.reserve)

    // The catch-up.

    @Test
    fun `a one-off that ends today is still open, and closes the next day`() {
        val lastDay = date(2026, 11, 20)
        assertThat(CatchUp.plan(lisbon, euros(500), rule(ONE_OFF), today = lastDay)).isNull()
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(720), rule(ONE_OFF), today = lastDay.plusDays(1)))
        assertThat(plan.closed).containsExactly(
            ClosedPeriod(
                capId = 1,
                range = DateRange(date(2026, 11, 10), date(2026, 11, 21)),
                cap = euros(800),
                spent = euros(720),
                reserveBefore = Money.ZERO,
                reserveAfter = Money.ZERO,
                forgiven = Money.ZERO,
                periods = 1,
            ),
        )
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 21))
        assertThat(plan.reserve).isEqualTo(Money.ZERO)
        assertThat(lisbon.after(plan).ended).isTrue()
    }

    @Test
    fun `an overspend is forgiven at the end, and nothing is kept`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(850), rule(ONE_OFF), today = date(2026, 11, 21)))
        val line = plan.closed.single()
        assertThat(line.forgiven).isEqualTo(euros(50))
        assertThat(line.reserveAfter).isEqualTo(Money.ZERO)
    }

    @Test
    fun `an absence past its end writes the one line, never a new period`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(300), rule(ONE_OFF), today = date(2027, 3, 1)))
        assertThat(plan.closed).hasSize(1)
        assertThat(plan.closed.single().range).isEqualTo(DateRange(date(2026, 11, 10), date(2026, 11, 21)))
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 21))
    }

    @Test
    fun `an ended one-off is idempotent, nothing more to close ever`() {
        val ended = lisbon.after(checkNotNull(CatchUp.plan(lisbon, euros(1), rule(ONE_OFF), date(2026, 11, 21))))
        assertThat(CatchUp.plan(ended, Money.ZERO, rule(ONE_OFF), date(2026, 11, 21))).isNull()
        assertThat(CatchUp.plan(ended, Money.ZERO, rule(ONE_OFF), date(2030, 1, 1))).isNull()
        // A clock set back before its end reopens nothing.
        assertThat(CatchUp.plan(ended, Money.ZERO, rule(ONE_OFF), date(2026, 11, 15))).isNull()
        assertThat(ended.ended).isTrue()
    }

    @Test
    fun `a one-off of one day closes the next`() {
        val party = oneOff(start = date(2026, 12, 31), endExclusive = date(2027, 1, 1), amount = euros(60))
        assertThat(CatchUp.plan(party, euros(70), rule(ONE_OFF), date(2026, 12, 31))).isNull()
        val plan = checkNotNull(CatchUp.plan(party, euros(70), rule(ONE_OFF), date(2027, 1, 1)))
        assertThat(plan.closed.single().forgiven).isEqualTo(euros(10))
    }

    @Test
    fun `a one-off far in the future closes nothing until it has been`() {
        val summer = oneOff(start = date(2027, 8, 1), endExclusive = date(2027, 8, 15))
        assertThat(CatchUp.plan(summer, euros(200), rule(ONE_OFF), date(2026, 11, 6))).isNull()
    }

    @Test
    fun `a rule never decides a one-off's dates`() {
        assertThrows(IllegalArgumentException::class.java) {
            com.callbackdev.tetto.core.domain.period.PeriodCalendar.containing(date(2026, 11, 6), rule(ONE_OFF))
        }
        assertThat(lisbon.oneOffDates).isEqualTo(DateRange(date(2026, 11, 10), date(2026, 11, 21)))
    }

    @Test
    fun `only a one-off has an end`() {
        assertThrows(IllegalArgumentException::class.java) { cap(MONTH, date(2026, 11, 1)).copy(kind = ONE_OFF) }
        assertThrows(IllegalArgumentException::class.java) { lisbon.copy(kind = WEEK) }
    }

    // What it says.

    @Test
    fun `its own mark and pace over its days, no reserve, no renewal`() {
        // Sunday 15 Nov: day 6 of 11, six days left counting today.
        val now = state(lisbon, euros(400), today = date(2026, 11, 15))
        assertThat(now.period).isEqualTo(DateRange(date(2026, 11, 10), date(2026, 11, 21)))
        assertThat(now.left).isEqualTo(euros(400))
        assertThat(now.daysLeft).isEqualTo(6)
        assertThat(now.todayMark).isEqualTo(Share(6, 11))
        assertThat(now.pace).isEqualTo(euros(66))
        assertThat(now.renewedToday).isFalse()
        assertThat(now.renews).isFalse()
        assertThat(now.ended).isFalse()
    }

    @Test
    fun `ahead of the days with a slack of a seventh of its length, fading`() {
        // Eleven days: a slack of 11/7 days. On its first day the line is (1 + 11/7) / 11 = 18/77.
        val first = date(2026, 11, 10)
        assertThat(CapStates.aheadLine(ONE_OFF, Share(1, 11), daysLeft = 11)).isEqualTo(Share(18 * 11, 77 * 11))
        // €800 × 18/77 is €187.01: €187 is under the line, €188 past it.
        assertThat(state(lisbon, euros(187), first).aheadOfDays).isFalse()
        assertThat(state(lisbon, euros(188), first).aheadOfDays).isTrue()
        // On its last day, the mark is the whole and nothing short of over is ahead.
        assertThat(state(lisbon, euros(799), date(2026, 11, 20)).aheadOfDays).isFalse()
    }

    @Test
    fun `a short one-off's slack is a day at least`() {
        // A weekend of three days: a slack of one day, not three sevenths.
        assertThat(CapStates.aheadLine(ONE_OFF, Share(1, 3), daysLeft = 3)).isEqualTo(Share(1 * 3 * 7 + 7 * 3, 7 * 9))
    }

    @Test
    fun `a one-off of one day has no mark and no pace, as a day`() {
        val party = oneOff(start = date(2026, 12, 31), endExclusive = date(2027, 1, 1), amount = euros(60))
        val now = state(party, euros(59), date(2026, 12, 31))
        assertThat(now.todayMark).isNull()
        assertThat(now.pace).isNull()
        assertThat(now.aheadOfDays).isFalse()
    }

    @Test
    fun `a one-off ahead of today counts what is added now, its pace over all its days`() {
        val summer = oneOff(start = date(2027, 8, 1), endExclusive = date(2027, 8, 15), amount = euros(1400))
        val now = state(summer, euros(400), date(2026, 11, 6))
        assertThat(now.daysLeft).isEqualTo(14)
        assertThat(now.pace).isEqualTo(euros(71))
        assertThat(now.lastDay).isEqualTo(date(2027, 8, 14))
    }

    @Test
    fun `ended, it says what its dates spent of its cap, and the day after its last that it ended`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(720), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = lisbon.after(plan)
        val line = plan.closed.single()
        val dayAfter = state(ended, Money.ZERO, date(2026, 11, 21), line)
        assertThat(dayAfter.ended).isTrue()
        assertThat(dayAfter.endedToday).isTrue()
        assertThat(dayAfter.period).isEqualTo(DateRange(date(2026, 11, 10), date(2026, 11, 21)))
        assertThat(dayAfter.spent).isEqualTo(euros(720))
        assertThat(dayAfter.left).isEqualTo(euros(80))
        assertThat(dayAfter.lastDay).isEqualTo(date(2026, 11, 20))
        assertThat(dayAfter.todayMark).isNull()
        assertThat(dayAfter.pace).isNull()
        assertThat(dayAfter.aheadOfDays).isFalse()

        val later = state(ended, Money.ZERO, date(2026, 11, 25), line)
        assertThat(later.endedToday).isFalse()
        assertThat(later.spent).isEqualTo(euros(720))
    }

    @Test
    fun `ended over, it says so`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(850), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = state(lisbon.after(plan), Money.ZERO, date(2026, 11, 22), plan.closed.single())
        assertThat(ended.over).isTrue()
        assertThat(ended.overBy).isEqualTo(euros(50))
    }

    @Test
    fun `a one-off begun the day an earlier run of it ended is not a renewal`() {
        val again = oneOff(start = date(2026, 11, 21), endExclusive = date(2026, 11, 28))
        val now = CapStates.of(
            again,
            Money.ZERO,
            rule(ONE_OFF),
            date(2026, 11, 21),
            EUR,
            lastClosedEnd = date(2026, 11, 21),
        )
        assertThat(now.renewedToday).isFalse()
        assertThat(now.renewal).isNull()
    }

    // The sentence, the cards, the month.

    @Test
    fun `the sentence says how a one-off ended, the day after its last, and then leaves it out`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(720), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = lisbon.after(plan)
        val coffee = cap(WEEK, openStart = date(2026, 11, 16), id = 2)
        val coffeeNow = CapStates.of(coffee, euros(5), rule(WEEK), date(2026, 11, 21), EUR)

        val dayAfter = CapsSentence.of(
            listOf(state(ended, Money.ZERO, date(2026, 11, 21), plan.closed.single()), coffeeNow),
        )
        assertThat(dayAfter).isInstanceOf(CapsSentence.Ended::class.java)
        assertThat((dayAfter as CapsSentence.Ended).first.cap.id).isEqualTo(1)
        assertThat(dayAfter.caps).isEqualTo(1)

        val later = CapsSentence.of(
            listOf(state(ended, Money.ZERO, date(2026, 11, 25), plan.closed.single()), coffeeNow.copy()),
        )
        assertThat(later).isInstanceOf(CapsSentence.AllUnder::class.java)
        assertThat((later as CapsSentence.AllUnder).closest.cap.id).isEqualTo(2)
        assertThat(later.caps).isEqualTo(1)
    }

    @Test
    fun `a cap over is said before a one-off that ended`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(720), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = state(lisbon.after(plan), Money.ZERO, date(2026, 11, 21), plan.closed.single())
        val coffee = CapStates.of(cap(WEEK, date(2026, 11, 16), id = 2), euros(30), rule(WEEK), date(2026, 11, 21), EUR)
        assertThat(CapsSentence.of(listOf(ended, coffee))).isInstanceOf(CapsSentence.Over::class.java)
    }

    @Test
    fun `every cap ended and none yesterday, nothing runs`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(720), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = state(lisbon.after(plan), Money.ZERO, date(2026, 12, 1), plan.closed.single())
        assertThat(CapsSentence.of(listOf(ended))).isEqualTo(CapsSentence.AllEnded(1))
    }

    @Test
    fun `the cards leave an ended one-off out, unless it was chosen for «In words»`() {
        val plan = checkNotNull(CatchUp.plan(lisbon, euros(799), rule(ONE_OFF), date(2026, 11, 21)))
        val ended = state(lisbon.after(plan), Money.ZERO, date(2026, 11, 22), plan.closed.single())
        val coffee = CapStates.of(cap(WEEK, date(2026, 11, 16), id = 2), euros(2), rule(WEEK), date(2026, 11, 22), EUR)
        assertThat(WidgetCaps.glance(listOf(ended, coffee), chosen = null)).containsExactly(coffee)
        assertThat(WidgetCaps.glance(listOf(ended, coffee), chosen = setOf(1L))).containsExactly(coffee)
        assertThat(WidgetCaps.words(listOf(ended, coffee), chosen = null)).isEqualTo(coffee)
        assertThat(WidgetCaps.words(listOf(ended, coffee), chosen = 1L)).isEqualTo(ended)
        assertThat(CapNote.of(ended)).isEqualTo(CapNote.Ended(euros(799), euros(800)))
    }

    @Test
    fun `a one-off over is forgiven when it ends, not when it starts again`() {
        val over = state(lisbon, euros(900), date(2026, 11, 15))
        assertThat(CapNote.of(over)).isEqualTo(CapNote.Forgiven(date(2026, 11, 21), renews = false))
    }

    @Test
    fun `its share of the month is its amount over its days, for the days in the month`() {
        val november = DateRange(date(2026, 11, 1), date(2026, 12, 1))
        // Eleven days of €800, all in November.
        assertThat(MonthSums.worth(listOf(lisbon), november, EUR)).isEqualTo(euros(800))
        // 28 Nov to 7 Dec, €1,000 over ten days: three in November, €300.
        val ski = oneOff(start = date(2026, 11, 28), endExclusive = date(2026, 12, 8), amount = euros(1000), id = 2)
        assertThat(MonthSums.worth(listOf(ski), november, EUR)).isEqualTo(euros(300))
        // Ended, its dates come from its closed line; unknown, nothing.
        val ended = lisbon.copy(openStart = date(2026, 11, 21))
        assertThat(MonthSums.worth(listOf(ended), november, EUR)).isEqualTo(Money.ZERO)
        val dates = mapOf(1L to DateRange(date(2026, 11, 10), date(2026, 11, 21)))
        assertThat(MonthSums.worth(listOf(ended), november, EUR, dates)).isEqualTo(euros(800))
    }

    // Changes (§4.5's rules).

    @Test
    fun `a month turned into a one-off from today keeps the days past as a line, without its reserve`() {
        val groceries = cap(MONTH, openStart = date(2026, 11, 1), amount = euros(300), reserve = euros(40))
        val start = date(2026, 11, 11)
        val plan = CapChanges.replace(
            groceries,
            rule(MONTH),
            start,
            euros(800),
            spentBefore = euros(90),
            reserveLimit = Money.ZERO,
        )
        assertThat(plan.openStart).isEqualTo(start)
        assertThat(plan.reserve).isEqualTo(Money.ZERO)
        val kept = checkNotNull(plan.closed)
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 1), start))
        // Ten days of thirty: €100 of the old cap; €90 spent, €40 of reserve let go.
        assertThat(kept.cap).isEqualTo(euros(100))
        assertThat(kept.reserveAfter).isEqualTo(Money.ZERO)
        assertThat(kept.forgiven).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a one-off from before the open period's start takes its spends, nothing closes`() {
        val coffee = cap(WEEK, openStart = date(2026, 11, 9), reserve = euros(5))
        val plan = CapChanges.replace(
            coffee,
            rule(WEEK),
            date(2026, 11, 7),
            euros(100),
            euros(0),
            reserveLimit = Money.ZERO,
        )
        assertThat(plan.closed).isNull()
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 7))
        assertThat(plan.reserve).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a one-off whose new dates start ahead keeps only the days already past`() {
        val today = date(2026, 11, 13)
        val plan = CapChanges.replace(
            lisbon,
            rule(ONE_OFF),
            start = date(2026, 11, 20),
            newAmount = euros(800),
            spentBefore = euros(50),
            keptUntil = today,
            reserveLimit = Money.ZERO,
        )
        val kept = checkNotNull(plan.closed)
        assertThat(kept.range).isEqualTo(DateRange(date(2026, 11, 10), today))
        // Three days of eleven: €218.18 of the €800.
        assertThat(kept.cap).isEqualTo(Money(21818))
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 20))
    }

    @Test
    fun `a one-off turned weekly keeps the days past as a line on its share`() {
        val today = date(2026, 11, 13)
        val start = CapChanges.replacedOpenStart(rule(WEEK), today, lastClosedEnd = null)
        assertThat(start).isEqualTo(date(2026, 11, 9))
        // The week starts before the one-off: the spends move, nothing closes.
        val plan = CapChanges.replace(lisbon, rule(ONE_OFF), start, euros(50), euros(120))
        assertThat(plan.closed).isNull()
        assertThat(plan.openStart).isEqualTo(date(2026, 11, 9))
        // A day turned out of it on its fourth day keeps three days of eleven, which close as any
        // period does: what they left goes to the day's reserve, held to the day's cap.
        val day = CapChanges.replace(lisbon, rule(ONE_OFF), today, euros(10), euros(50))
        assertThat(checkNotNull(day.closed).cap).isEqualTo(Money(21818))
        assertThat(day.reserve).isEqualTo(euros(10))
    }

    @Test
    fun `chosen dates never reach into a closed period, and never end before today`() {
        val today = date(2026, 11, 22)
        val chosen = DateRange(date(2026, 11, 15), date(2026, 11, 30))
        assertThat(CapChanges.oneOffDates(chosen, today, lastClosedEnd = date(2026, 11, 21)))
            .isEqualTo(DateRange(date(2026, 11, 21), date(2026, 11, 30)))
        assertThat(CapChanges.oneOffDates(DateRange(date(2026, 11, 1), date(2026, 11, 5)), today, null))
            .isEqualTo(DateRange(date(2026, 11, 1), date(2026, 11, 23)))
        assertThat(CapChanges.oneOffDates(chosen, today, lastClosedEnd = null)).isEqualTo(chosen)
        // A closed period that ends after the chosen end: one day, today.
        assertThat(
            CapChanges.oneOffDates(DateRange(today, today.plusDays(1)), today, lastClosedEnd = today.plusDays(3)),
        )
            .isEqualTo(DateRange(today, today.plusDays(1)))
    }

    @Test
    fun `a day cap is never a one-off by mistake`() {
        assertThat(DAY.renews).isTrue()
        assertThat(ONE_OFF.renews).isFalse()
    }
}
