package com.callbackdev.tetto

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The application: Hilt's graph root. Nothing runs here, and nothing ever runs on a timer: Tetto
 * has no service and no background work of its own (PLANNING.md §9).
 */
@HiltAndroidApp
class TettoApplication : Application()
