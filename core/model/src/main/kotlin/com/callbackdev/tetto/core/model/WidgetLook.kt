package com.callbackdev.tetto.core.model

/**
 * The seven colours a card can wear (PLANNING.md §7), the family's: their hexes are the design
 * system's (`WidgetPalette.kt`), the one place hexes live.
 */
enum class WidgetCardColor { BLUE, AZURE, GREEN, TEAL, PLUM, CLAY, OCHRE }

/**
 * What a card is painted on: the app's light or dark surface, whichever the phone is in, or one of
 * the six colours ([WidgetLook.cardColor]).
 */
enum class WidgetGround { LIGHT, DARK, SYSTEM, COLOR }

/**
 * One card's look and content, chosen in its settings and kept per card (PLANNING.md §5, the
 * `widgets` file, not backed up: a restore renews the cards' ids), so the same card can sit on a
 * home screen twice, dressed twice. The default is a solid card in the app's own colour, the
 * family's violet, the nearest to the icon's indigo (owner, 15 Nov 2026), worn before anybody
 * configures anything.
 *
 * @property opacityPct the ground's opacity, 0 to 100; the ink never thins with it.
 * @property cardColor kept while another ground is picked, so coming back finds it.
 * @property glanceCaps «List»: the caps it shows, in the reader's order; null for all.
 * @property wordsCap «At a glance»: its cap; null for the one closest to its cap.
 * @property showSentence «At a glance»: «The sentence» under the figure on a tall card (owner,
 *   5 Nov 2026); off, the circle takes the band it leaves.
 * @property showOthers «At a glance»: «The other caps» (owner, 14 Nov 2026): on a card two rows high
 *   or more, one row of the other caps under the card's own, each a door to its numpad; the ring
 *   keeps the family's size.
 * @property otherCaps «At a glance»: the other caps that row shows, in the reader's order; null for
 *   every one.
 * @property hideAmounts no figure on the card (owner, 6 Nov 2026), for a home screen others see:
 *   the bars, the ring, the names, the dates and the words stay, over said in words; the amounts
 *   are absent, never blurred, TalkBack's description included.
 */
data class WidgetLook(
    val ground: WidgetGround = WidgetGround.COLOR,
    val opacityPct: Int = DEFAULT_OPACITY,
    val cardColor: WidgetCardColor = WidgetCardColor.PLUM,
    val glanceCaps: Set<Long>? = null,
    val wordsCap: Long? = null,
    val showSentence: Boolean = true,
    val showOthers: Boolean = false,
    val otherCaps: Set<Long>? = null,
    val hideAmounts: Boolean = false,
) {
    companion object {
        /** Solid: below half, the ink has to ask the wallpaper, a different conversation. */
        const val DEFAULT_OPACITY: Int = 100
    }
}
