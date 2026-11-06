package com.callbackdev.tetto.widget

import android.content.Context
import android.content.Intent
import com.callbackdev.tetto.core.data.widget.QuickAdd

/**
 * The doors a card's touches open (PLANNING.md §7, Taps): a cap, the numpad over the home screen
 * on that cap; the rest of the card, the caps screen.
 */
internal object WidgetIntents {
    /** The app, by the launcher's own intent: a touch brings back its task rather than a second one. */
    fun openApp(context: Context): Intent? = context.packageManager.getLaunchIntentForPackage(context.packageName)

    fun quickAdd(context: Context, capId: Long): Intent = QuickAdd.intent(context, capId)
}
