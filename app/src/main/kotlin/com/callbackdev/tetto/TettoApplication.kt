package com.callbackdev.tetto

import android.app.Application
import android.content.res.Configuration
import androidx.core.content.pm.PackageInfoCompat
import com.callbackdev.tetto.core.reminder.EveningReminder
import com.callbackdev.tetto.quickadd.CapShortcuts
import com.callbackdev.tetto.widget.WidgetPreviews
import com.callbackdev.tetto.widget.WidgetUpdater
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The application: Hilt's graph root. Nothing ever runs on a timer: Tetto has no service and no
 * background work of its own (PLANNING.md §9). At each start of the process the evening reminder
 * arms its one alarm, if the reader turned it on (a boot or an update start the process, and a
 * boot clears every alarm), and keeps it in step with its setting while the process lives. Two
 * listeners start with it, which cost nothing until something is written: the home screen's cards
 * redraw after every write, and the launcher's shortcuts follow the caps (PLANNING.md §7, §8). The
 * widget picker's previews are published once per version.
 */
@HiltAndroidApp
class TettoApplication : Application() {
    @Inject lateinit var reminder: EveningReminder

    @Inject lateinit var widgets: WidgetUpdater

    @Inject lateinit var shortcuts: CapShortcuts

    override fun onCreate() {
        super.onCreate()
        reminder.start()
        widgets.start()
        shortcuts.start()
        val version = runCatching {
            PackageInfoCompat.getLongVersionCode(packageManager.getPackageInfo(packageName, 0))
        }.getOrDefault(0L)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { WidgetPreviews.publishIfNeeded(this@TettoApplication, version) }
        }
    }

    /** The app's language may have changed: the shortcuts' words follow it. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        shortcuts.refresh()
    }
}
