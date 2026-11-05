package com.callbackdev.tetto

import android.app.Application
import com.callbackdev.tetto.core.reminder.EveningReminder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * The application: Hilt's graph root. Nothing ever runs on a timer: Tetto has no service and no
 * background work of its own (PLANNING.md §9). At each start of the process the evening reminder
 * arms its one alarm, if the reader turned it on (a boot or an update start the process, and a
 * boot clears every alarm), and keeps it in step with its setting while the process lives.
 */
@HiltAndroidApp
class TettoApplication : Application() {
    @Inject lateinit var reminder: EveningReminder

    override fun onCreate() {
        super.onCreate()
        reminder.start()
    }
}
