package com.callbackdev.tetto.core.domain.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When the evening reminder comes (PLANNING.md §6, §8): always at the reader's local time, worked
 * out again after every clock or zone change, since an alarm is an instant and 20:30 in Rome is
 * not 20:30 in Lisbon.
 *
 * A time that does not exist on a day (the hour the clocks skip in spring) moves forward by the
 * length of the gap, as `java.time` resolves it; a time that happens twice (the hour repeated in
 * autumn) is the first of the two.
 */
object ReminderSchedule {
    /** The next reminder: [time] on the first day whose [time] is still after [now], in [zone]. */
    fun next(now: Instant, zone: ZoneId, time: LocalTime): Instant {
        var day = LocalDate.ofInstant(now, zone)
        while (true) {
            val at = ZonedDateTime.of(day, time, zone).toInstant()
            if (at.isAfter(now)) return at
            day = day.plusDays(1)
        }
    }

    /**
     * Whether a reminder due at [dueAt] still has something to ask at [now]: not early, and still
     * the day it was for. Doze, a phone switched off or a flight can hold an alarm back; "anything
     * to add today?" asked the next morning would be about a day that is over, so it stays quiet.
     */
    fun stillTimely(dueAt: Instant, now: Instant, zone: ZoneId): Boolean =
        !now.isBefore(dueAt) && LocalDate.ofInstant(dueAt, zone) == LocalDate.ofInstant(now, zone)

    /**
     * How long until the local day of [now] ends: how long the reminder stays in the shade. A
     * question about today is not one to find the next morning. The day may be 23 or 25 hours.
     */
    fun untilEndOfDay(now: Instant, zone: ZoneId): Duration =
        Duration.between(now, LocalDate.ofInstant(now, zone).plusDays(1).atStartOfDay(zone).toInstant())
}
