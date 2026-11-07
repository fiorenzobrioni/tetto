package com.callbackdev.tetto.core.data.lock

import android.app.Activity
import android.os.SystemClock
import com.callbackdev.tetto.core.domain.lock.LockSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tetto's lock for this process (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): whether its pages
 * wait for the phone's own lock, and asking it. The pages ([Activity.onStart], [Activity.onStop]) tell
 * it when they come and go; [LockSchedule] decides, on the time since boot, which a changed clock
 * never moves. One for the process, the caps screen's and the numpad over the home screen's: going
 * from one to the other is never away.
 */
@Singleton
class AppLock
@Inject
constructor(private val device: DeviceLock) {
    private val schedule = LockSchedule()
    private val unlockedState = MutableStateFlow(false)

    /** Whether the reader has unlocked Tetto since it last locked. */
    val unlocked: StateFlow<Boolean> = unlockedState.asStateFlow()

    /**
     * Whether a page must wait for the lock now, with the lock [on] in Settings: never on a phone
     * with no screen lock, which has nothing to ask (Settings says so).
     */
    fun guards(on: Boolean): Boolean = on && !schedule.unlocked && device.available()

    fun pageShown() {
        schedule.shown(SystemClock.elapsedRealtime())
        unlockedState.value = schedule.unlocked
    }

    fun pageHidden(turning: Boolean) {
        schedule.hidden(SystemClock.elapsedRealtime(), turning)
    }

    /** The reader is here and chose the lock themselves (Settings): no need to ask them now. */
    fun markUnlocked() {
        schedule.unlock()
        unlockedState.value = true
    }

    /**
     * Asks once by itself for a page that has just come on screen locked; after a refusal, the
     * page's own button asks ([ask]), never a prompt that comes back by itself.
     */
    suspend fun askOnce(activity: Activity, title: String): Boolean {
        if (schedule.askedSinceLocked) return false
        schedule.notedAsking()
        return ask(activity, title)
    }

    /** Asks for the phone's lock over [activity]; unlocked once it says yes. */
    suspend fun ask(activity: Activity, title: String, subtitle: String? = null): Boolean {
        schedule.asking = true
        val yes = try {
            device.ask(activity, title, subtitle)
        } finally {
            schedule.asking = false
        }
        if (yes) markUnlocked()
        return yes
    }

    /** Whether the phone's own lock is on screen now, over Tetto: what a page must not take as leaving. */
    val asking: Boolean get() = schedule.asking

    /** Whether the phone has a lock to ask for. */
    fun available(): Boolean = device.available()
}
