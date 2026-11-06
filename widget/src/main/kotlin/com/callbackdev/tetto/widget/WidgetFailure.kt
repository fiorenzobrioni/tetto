package com.callbackdev.tetto.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews

/**
 * A card whose drawing threw. Glance's own answer is a blank box that says "Can't show content"
 * and nothing else, with the reason only in its own log; here the reason goes to Tetto's
 * (`adb logcat -s TettoWidget`), with the sizes the launcher gave the card, and the card says in
 * Tetto's words that it could not be drawn, the whole of it opening the app. Nothing retries on
 * its own: the next redraw (a spend, midnight, the card's settings) draws the card again.
 */
internal object WidgetFailure {
    fun show(context: Context, appWidgetId: Int, error: Throwable) {
        val manager = AppWidgetManager.getInstance(context)
        val options = runCatching { manager.getAppWidgetOptions(appWidgetId) }.getOrNull()
        logWidgetFailure("Drawing card $appWidgetId failed; the launcher's sizes: ${sizes(options)}", error)
        val views = RemoteViews(context.packageName, R.layout.widget_failure)
        WidgetIntents.openApp(context)?.let { intent ->
            val open = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_failure, open)
        }
        runCatching { manager.updateAppWidget(appWidgetId, views) }
            .onFailure { logWidgetFailure("Card $appWidgetId: the failure card could not be shown either", it) }
    }

    /** What the launcher said of the card's size, the first suspect when a form's arithmetic throws. */
    private fun sizes(options: Bundle?): String {
        if (options == null) return "none"
        val list = runCatching {
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
        }.getOrNull()
        val min = "${options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)}" +
            "x${options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)}"
        val max = "${options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)}" +
            "x${options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)}"
        return "min $min dp, max $max dp, sizes ${list ?: "none"}"
    }
}
