package com.callbackdev.tetto.core.domain.lock

/**
 * When the lock asks (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): at start, and on coming back
 * after [awayMillis] or more away, a minute; never while Tetto is in use, nor on a turn of the
 * phone. Pure: the pages' comings and goings in, on a clock that never goes back (the time since
 * boot), whether Tetto is unlocked out.
 *
 * "Away" is from the moment no page of Tetto's is on screen (the caps, the numpad over the home
 * screen) to the moment one is again: going from one to the other is not away. The phone's own
 * lock screen, shown over Tetto while it asks ([asking]), is not away either: a reader slow to type
 * their PIN is not locked out again for it.
 *
 * One instance for the process: a new process starts locked.
 */
class LockSchedule(private val awayMillis: Long = AWAY_MILLIS) {
    /** Whether the reader has unlocked Tetto since it last locked. */
    var unlocked: Boolean = false
        private set

    /** Whether the phone's own lock is being asked now: what covers Tetto meanwhile is not away. */
    var asking: Boolean = false

    /** Whether Tetto has asked by itself since it last locked: after a refusal, the reader asks. */
    var askedSinceLocked: Boolean = false
        private set

    private var visible = 0
    private var awaySince: Long? = null

    /** A page of Tetto's came on screen at [now]: away long enough, it locks again. */
    fun shown(now: Long) {
        if (visible++ == 0) {
            val since = awaySince
            if (since != null && now - since >= awayMillis) lock()
            awaySince = null
        }
    }

    /**
     * A page of Tetto's left the screen at [now]; [turning] for a change of configuration (a
     * rotation, a fold), which is not away.
     */
    fun hidden(now: Long, turning: Boolean) {
        visible = (visible - 1).coerceAtLeast(0)
        if (visible == 0 && !turning && !asking) awaySince = now
    }

    /** The phone's own lock said yes. */
    fun unlock() {
        unlocked = true
        asking = false
        awaySince = null
    }

    /** Tetto asked by itself, once for this lock: a refusal waits for the reader's own touch. */
    fun notedAsking() {
        askedSinceLocked = true
    }

    private fun lock() {
        unlocked = false
        askedSinceLocked = false
    }

    companion object {
        /** A minute away (owner, 6 Nov 2026): long enough for a glance at a message, short enough for a phone left on a table. */
        const val AWAY_MILLIS: Long = 60_000L
    }
}
