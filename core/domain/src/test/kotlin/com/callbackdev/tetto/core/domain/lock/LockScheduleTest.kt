package com.callbackdev.tetto.core.domain.lock

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** When the lock asks (owner, 6 Nov 2026): at start, and after a minute away; never in use. */
class LockScheduleTest {
    private val schedule = LockSchedule()

    @Test
    fun `a new process starts locked`() {
        schedule.shown(now = 0)
        assertThat(schedule.unlocked).isFalse()
        schedule.unlock()
        assertThat(schedule.unlocked).isTrue()
    }

    @Test
    fun `away under a minute stays unlocked, a minute or more locks`() {
        schedule.shown(0)
        schedule.unlock()
        schedule.hidden(1_000, turning = false)
        schedule.shown(60_999)
        assertThat(schedule.unlocked).isTrue()
        schedule.hidden(61_000, turning = false)
        schedule.shown(121_000)
        assertThat(schedule.unlocked).isFalse()
    }

    @Test
    fun `a turn of the phone is not away`() {
        schedule.shown(0)
        schedule.unlock()
        schedule.hidden(1_000, turning = true)
        schedule.shown(500_000)
        assertThat(schedule.unlocked).isTrue()
    }

    @Test
    fun `from the caps to the numpad over the home screen is not away`() {
        schedule.shown(0)
        schedule.unlock()
        // The numpad comes on screen before the caps leave it.
        schedule.shown(1_000)
        schedule.hidden(1_100, turning = false)
        schedule.hidden(200_000, turning = false)
        schedule.shown(230_000)
        assertThat(schedule.unlocked).isTrue()
    }

    @Test
    fun `the phone's own lock screen over Tetto is not away, however long the PIN takes`() {
        schedule.shown(0)
        schedule.asking = true
        schedule.hidden(1_000, turning = false)
        schedule.shown(300_000)
        schedule.unlock()
        assertThat(schedule.unlocked).isTrue()
        assertThat(schedule.asking).isFalse()
    }

    @Test
    fun `Tetto asks by itself once per lock, then waits for the reader`() {
        schedule.shown(0)
        assertThat(schedule.askedSinceLocked).isFalse()
        schedule.notedAsking()
        assertThat(schedule.askedSinceLocked).isTrue()
        schedule.unlock()
        schedule.hidden(1_000, turning = false)
        schedule.shown(100_000)
        assertThat(schedule.unlocked).isFalse()
        assertThat(schedule.askedSinceLocked).isFalse()
    }
}
