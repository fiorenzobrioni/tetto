package com.callbackdev.tetto.core.domain.period

import com.callbackdev.tetto.core.domain.cap
import com.callbackdev.tetto.core.domain.cap.CatchUp
import com.callbackdev.tetto.core.domain.date
import com.callbackdev.tetto.core.domain.euros
import com.callbackdev.tetto.core.domain.rule
import com.callbackdev.tetto.core.model.NearDay
import com.callbackdev.tetto.core.model.PeriodKind.DAY
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Local dates against instants (PLANNING.md §4.2, §4.3; §4.7: the day of a daylight-saving change,
 * "a spend today" across midnight and after a change of zone). In Rome, 29 Mar 2026 has 23 hours
 * and 25 Oct 2026 has 25.
 */
class DaysTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val newYork = ZoneId.of("America/New_York")

    private fun at(text: String, zone: ZoneId): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    @Test
    fun `the day of a daylight-saving change has 23 hours, and its next midnight comes 23 hours after its first`() {
        val (start, end) = Days.instantsOf(date(2026, 3, 29), rome)
        assertThat(Duration.between(start, end)).isEqualTo(Duration.ofHours(23))
        assertThat(Days.nextMidnight(start, rome)).isEqualTo(end)
        assertThat(Days.nextMidnight(at("2026-03-29T23:59:59", rome), rome)).isEqualTo(end)
        assertThat(Days.dateOf(at("2026-03-29T23:59:59", rome), rome)).isEqualTo(date(2026, 3, 29))
    }

    @Test
    fun `the day of a daylight-saving change has 25 hours the other way`() {
        val (start, end) = Days.instantsOf(date(2026, 10, 25), rome)
        assertThat(Duration.between(start, end)).isEqualTo(Duration.ofHours(25))
        assertThat(Days.nextMidnight(at("2026-10-25T02:30", rome), rome)).isEqualTo(end)
    }

    @Test
    fun `a daily period spans a 23-hour and a 25-hour day whole, and closes at the next midnight`() {
        for (day in listOf(date(2026, 3, 29), date(2026, 10, 25))) {
            val coffee = cap(DAY, openStart = day)
            val (_, end) = Days.instantsOf(day, rome)
            val lastMoment = Days.dateOf(end.minusSeconds(1), rome)
            assertThat(CatchUp.plan(coffee, euros(3), rule(DAY), lastMoment)).isNull()
            val firstMoment = Days.dateOf(end, rome)
            assertThat(CatchUp.plan(coffee, euros(3), rule(DAY), firstMoment)?.closed).hasSize(1)
        }
    }

    @Test
    fun `a zone whose midnight is skipped starts the day at its first instant`() {
        // Find a day whose midnight a clock change skips, wherever the tz data puts it.
        val santiago = ZoneId.of("America/Santiago")
        val transition = generateSequence(santiago.rules.nextTransition(Instant.parse("2026-01-01T00:00:00Z"))) {
            santiago.rules.nextTransition(it.instant)
        }.first { it.isGap && it.dateTimeBefore.toLocalTime().hour == 0 }
        val day = transition.dateTimeAfter.toLocalDate()
        val (start, _) = Days.instantsOf(day, santiago)
        assertThat(start).isEqualTo(transition.instant)
        assertThat(Days.nextMidnight(start.minusSeconds(60), santiago)).isEqualTo(transition.instant)
        assertThat(Days.dateOf(start, santiago)).isEqualTo(day)
    }

    @Test
    fun `a spend added at 23 30 is not today's after midnight`() {
        val added = at("2026-11-05T23:30", rome)
        val (todayFrom, todayUntil) = Days.instantsOf(date(2026, 11, 5), rome)
        assertThat(added).isAtLeast(todayFrom)
        assertThat(added).isLessThan(todayUntil)
        val (tomorrowFrom, _) = Days.instantsOf(date(2026, 11, 6), rome)
        assertThat(added).isLessThan(tomorrowFrom)
    }

    @Test
    fun `a spend today is today in the zone of the moment of asking`() {
        // Added at 23:30 in Rome: 17:30 in New York, the same day there too.
        val added = at("2026-11-05T23:30", rome)
        val (from, until) = Days.instantsOf(date(2026, 11, 5), newYork)
        assertThat(added).isAtLeast(from)
        assertThat(added).isLessThan(until)
        // Added at 02:00 in Rome on the 6th: still the 5th in New York, after the flight.
        val early = at("2026-11-06T02:00", rome)
        assertThat(Days.dateOf(early, newYork)).isEqualTo(date(2026, 11, 5))
        val (nyFrom, nyUntil) = Days.instantsOf(date(2026, 11, 5), newYork)
        assertThat(early).isAtLeast(nyFrom)
        assertThat(early).isLessThan(nyUntil)
    }

    @Test
    fun `how near a day is, for its words`() {
        val today = date(2026, 11, 5)
        assertThat(Days.near(today, today)).isEqualTo(NearDay.TODAY)
        assertThat(Days.near(date(2026, 11, 6), today)).isEqualTo(NearDay.TOMORROW)
        assertThat(Days.near(date(2026, 11, 7), today)).isEqualTo(NearDay.WEEKDAY)
        assertThat(Days.near(date(2026, 11, 11), today)).isEqualTo(NearDay.WEEKDAY)
        // A week ahead would be today's own weekday: a date says it.
        assertThat(Days.near(date(2026, 11, 12), today)).isEqualTo(NearDay.DATE)
        assertThat(Days.near(date(2026, 11, 4), today)).isEqualTo(NearDay.DATE)
    }

    @Test
    fun `zoned instants agree with the dates they come from`() {
        val noon = ZonedDateTime.of(2026, 11, 5, 12, 0, 0, 0, rome).toInstant()
        assertThat(Days.dateOf(noon, rome)).isEqualTo(date(2026, 11, 5))
        assertThat(Days.dateOf(noon, ZoneId.of("Pacific/Auckland"))).isEqualTo(date(2026, 11, 6))
    }
}
