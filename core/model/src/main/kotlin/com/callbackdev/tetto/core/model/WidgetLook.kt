package com.callbackdev.tetto.core.model

/**
 * The six colours a card can wear (PLANNING.md §7), the family's: their hexes are the design
 * system's (`WidgetPalette.kt`), the one place hexes live.
 */
enum class WidgetCardColor { BLUE, AZURE, GREEN, TEAL, PLUM, CLAY }

/**
 * What a card is painted on: the app's light or dark surface, whichever the phone is in, or one of
 * the six colours ([WidgetLook.cardColor]).
 */
enum class WidgetGround { LIGHT, DARK, SYSTEM, COLOR }

/**
 * One card's look and content, chosen in its settings and kept per card (PLANNING.md §5, the
 * `widgets` file, not backed up: a restore renews the cards' ids), so the same card can sit on a
 * home screen twice, dressed twice. The default is the family's: a solid blue card, worn before
 * anybody configures anything.
 *
 * @property opacityPct the ground's opacity, 0 to 100; the ink never thins with it.
 * @property cardColor kept while another ground is picked, so coming back finds it.
 * @property glanceCaps «At a glance»: the caps it shows, in the reader's order; null for all.
 * @property wordsCap «In words»: its cap; null for the one closest to its cap.
 * @property showSentence «In words»: «The sentence» under the figure on a tall card (owner,
 *   5 Nov 2026); off, the circle takes the band it leaves.
 * @property wordsBelow «In words», on a card two rows high and three cells wide or more: what stands
 *   under its cap (owner, 14 Nov 2026): the days of the cap's period, or the other caps the reader
 *   picks ([wordsBelowCaps]), so the card opens more than one cap.
 * @property wordsBelowCaps «In words»'s other caps, under its own on a wide card, at most as many as
 *   the card holds side by side; empty, the period's days stay.
 * @property hideAmounts no figure on the card (owner, 6 Nov 2026), for a home screen others see:
 *   the bars, the ring, the names, the dates and the words stay, over said in words; the amounts
 *   are absent, never blurred, TalkBack's description included.
 */
data class WidgetLook(
    val ground: WidgetGround = WidgetGround.COLOR,
    val opacityPct: Int = DEFAULT_OPACITY,
    val cardColor: WidgetCardColor = WidgetCardColor.BLUE,
    val glanceCaps: Set<Long>? = null,
    val wordsCap: Long? = null,
    val showSentence: Boolean = true,
    val wordsBelow: WordsBelow = WordsBelow.DAYS,
    val wordsBelowCaps: Set<Long> = emptySet(),
    val hideAmounts: Boolean = false,
) {
    companion object {
        /** Solid: below half, the ink has to ask the wallpaper, a different conversation. */
        const val DEFAULT_OPACITY: Int = 100
    }
}

/** What «In words» shows under its cap on a wide card (owner, 14 Nov 2026). */
enum class WordsBelow {
    /** The days of the cap's period, today marked: how many are left, at a glance. */
    DAYS,

    /** Other caps the reader picks, side by side, each a door to its own numpad. */
    CAPS,
}
