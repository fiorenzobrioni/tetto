package com.callbackdev.tetto.core.domain.widget

/**
 * **The widgets' type scale**: the sizes a home-screen card prints in, by role, so a line of the
 * same kind is the same size on every card of the family (PLANNING.md §7). Six steps, each a role:
 *
 * - [CAPTION]: read last.
 * - [LABEL]: a list's row, a footnote, the figure under a one-cell ring.
 * - [BODY]: the second thing read: a cap's name, its sentence, "of €20 · until Sunday".
 * - [TITLE]: the words of a card whose hero is a drawing.
 * - [DISPLAY]: a hero wider than a figure.
 * - [HERO]: what is left, on a card two rows high and up.
 *
 * Where a form sizes a line to the grant (the hero steps down until it fits, a narrow row sets its
 * words smaller), the ladder starts on a step; its rungs are not steps. `WidgetTypeTest` holds
 * every fixed size to the scale.
 */
object WidgetType {
    const val CAPTION = 11f
    const val LABEL = 13f
    const val BODY = 16f
    const val TITLE = 22f
    const val DISPLAY = 30f
    const val HERO = 34f

    /** Every step, for the test that holds the cards to them. */
    val steps: Set<Float> = setOf(CAPTION, LABEL, BODY, TITLE, DISPLAY, HERO)
}
