package com.callbackdev.tetto.core.data.widget

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The door to the numpad over the home screen (PLANNING.md §7, Taps; §8, the launcher's shortcuts):
 * `QuickAddActivity`, in `:app`, on one cap. Named here, where the cards, the shortcuts and the
 * numpad's ViewModel can all see it; `:app`'s test holds the class name to the activity.
 */
object QuickAdd {
    /** The activity, by name: the cards' module cannot see `:app`. */
    const val ACTIVITY: String = "com.callbackdev.tetto.quickadd.QuickAddActivity"

    /** The cap the numpad opens on, a `Long`. */
    const val EXTRA_CAP_ID: String = "com.callbackdev.tetto.extra.CAP_ID"

    /**
     * The numpad on [capId], in its own task over whatever is on screen, without the window's own
     * animation (the sheet rises by itself). Each cap's intent carries its own data, so two caps'
     * touches on one card are two pending intents, never one.
     */
    fun intent(context: Context, capId: Long): Intent = Intent(Intent.ACTION_VIEW)
        .setComponent(ComponentName(context.packageName, ACTIVITY))
        .setData(Uri.parse("tetto://cap/$capId"))
        .putExtra(EXTRA_CAP_ID, capId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
}
