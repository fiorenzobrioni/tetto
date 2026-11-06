package com.callbackdev.tetto.core.domain.period

import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.NearDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Where local dates meet instants, the only place they do (PLANNING.md §4.2, §4.3): the days a
 * period is made of are local dates, and an instant becomes one only in the zone of the moment of
 * asking.
 */
object Days {
    /** The local date of [instant] in [zone]. */
    fun dateOf(instant: Instant, zone: ZoneId): LocalDate = LocalDate.ofInstant(instant, zone)

    /**
     * The first instant of the day after [now]'s, in [zone]: when the next period may begin. On a
     * day whose midnight a clock change skips, the day's first instant that exists.
     */
    fun nextMidnight(now: Instant, zone: ZoneId): Instant = dateOf(now, zone).plusDays(1).atStartOfDay(zone).toInstant()

    /**
     * The instants of [date] in [zone], from its first included to the next day's first excluded:
     * 23 or 25 hours long on a daylight-saving day. "A spend was added today" (the reminder) is a
     * spend added within them.
     */
    fun instantsOf(date: LocalDate, zone: ZoneId): Pair<Instant, Instant> =
        date.atStartOfDay(zone).toInstant() to date.plusDays(1).atStartOfDay(zone).toInstant()

    /** The instants of [range]'s days in [zone], as [instantsOf] gives a day's: the reader's month. */
    fun instantsOf(range: DateRange, zone: ZoneId): Pair<Instant, Instant> =
        range.start.atStartOfDay(zone).toInstant() to range.endExclusive.atStartOfDay(zone).toInstant()

    /** How near [date] is to [today], for the words that name it. */
    fun near(date: LocalDate, today: LocalDate): NearDay = when (ChronoUnit.DAYS.between(today, date)) {
        0L -> NearDay.TODAY
        1L -> NearDay.TOMORROW
        in 2L..6L -> NearDay.WEEKDAY
        else -> NearDay.DATE
    }
}
