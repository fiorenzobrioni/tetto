package com.callbackdev.tetto.core.data.time

import com.callbackdev.tetto.core.domain.period.Days
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Now, and the zone in effect now (PLANNING.md §4.2): today is the local date of the two, read at
 * every call, so a change of zone moves today with it. Injected, so that the tests can set the
 * clock and the zone the engine runs on.
 */
interface TodaySource {
    fun now(): Instant

    fun zone(): ZoneId

    fun today(): LocalDate = Days.dateOf(now(), zone())

    /** When today ends, in the zone in effect now: the moment the next period may begin. */
    fun nextMidnight(): Instant = Days.nextMidnight(now(), zone())

    companion object {
        /** The phone's clock and its current zone, read at every call. */
        val System: TodaySource = object : TodaySource {
            override fun now(): Instant = Instant.now()

            override fun zone(): ZoneId = ZoneId.systemDefault()
        }
    }
}
