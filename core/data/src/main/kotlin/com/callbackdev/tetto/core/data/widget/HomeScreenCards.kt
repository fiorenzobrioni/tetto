package com.callbackdev.tetto.core.data.widget

/** The two cards, as a screen outside the widgets' module names them (PLANNING.md §7). */
enum class HomeCard {
    /** «At a glance»: the caps, each its icon, what is left and a slim bar. */
    AT_A_GLANCE,

    /** «In words»: one cap in the family's ring, and the words. */
    IN_WORDS,
}

/**
 * The home screen's side of the cards, for the screens that offer them (the first run's last
 * page, Settings; PLANNING.md §8): whether the launcher can place one on request, and the request.
 * Implemented by `:widget`, which the feature modules cannot see.
 */
interface HomeScreenCards {
    /** Whether the launcher accepts a pin request (`AppWidgetManager.isRequestPinAppWidgetSupported`). */
    fun canPin(): Boolean

    /** Asks the launcher to place [card]; it shows its own dialog, and the reader decides there. */
    fun pin(card: HomeCard): Boolean

    /** Whether a card of Tetto's is on a home screen now. */
    fun placed(): Boolean
}
