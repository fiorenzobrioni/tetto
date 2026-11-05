package com.callbackdev.tetto.core.domain.widget

import kotlin.math.floor

/*
 * The arithmetic of type on a home-screen card (PLANNING.md §6, `GlanceFit`), in dp and sp as plain
 * floats so it stays pure Kotlin. A launcher draws a card in its own system face, which only the
 * widget's process can measure: what is measured (how wide a figure is, in ems; how many lines a
 * sentence takes) comes in as a number, and everything decided from it is here, where a test pins it.
 */

/**
 * The height one line of a Glance `Text` occupies (font padding on): about 1.32 em in the system
 * face, times the reader's font scale, and the pixel the face's metrics are rounded up to. An
 * estimate, named as one.
 */
fun lineHeight(sizeSp: Float, fontScale: Float): Float = sizeSp * LINE_BOX_EM * fontScale + LINE_ROUNDING

/** A line's height without its font padding: the face's ascent and descent, a line after the first. */
fun bareLineHeight(sizeSp: Float, fontScale: Float): Float = sizeSp * BARE_BOX_EM * fontScale

/**
 * The height of one Glance `Text` over [lines] lines: the first as [lineHeight], each further one
 * the face's ascent and descent alone: the font's padding is the first line's top and the last
 * line's bottom, never paid between two lines.
 */
fun linesHeight(sizeSp: Float, lines: Int, fontScale: Float): Float =
    if (lines <= 0) 0f else lineHeight(sizeSp, fontScale) + bareLineHeight(sizeSp, fontScale) * (lines - 1)

/** [lineHeight] read backwards: the largest size whose line fits [room]. Never negative. */
fun sizeForLine(room: Float, fontScale: Float): Float =
    ((room - LINE_ROUNDING) / (LINE_BOX_EM * fontScale.coerceAtLeast(MIN_SCALE))).coerceAtLeast(0f)

/**
 * The largest size at which a line [em] wide fits [width] whole, the slack a launcher's rounding
 * takes kept out of it. A smaller figure still reads; a cut one does not.
 */
fun spThatFits(width: Float, em: Float, fontScale: Float): Float =
    (width - FIT_SLACK).coerceAtLeast(0f) / (em.coerceAtLeast(MIN_EM) * fontScale.coerceAtLeast(MIN_SCALE))

/** How wide a line [em] wide is at [sizeSp], with the slack a measured width is given. */
fun widthOf(em: Float, sizeSp: Float, fontScale: Float): Float = em * sizeSp * fontScale + FIT_SLACK

/** Rounds a size down to a quarter of a point, so a size that grows with the card does not jitter. */
fun quarterPoint(sp: Float): Float = floor(sp * 4f) / 4f

const val LINE_BOX_EM: Float = 1.32f
const val BARE_BOX_EM: Float = 1.2f

/** The pixel a line's font metrics are rounded up to, at any density. */
const val LINE_ROUNDING: Float = 1f

/** The air a measured width is given before it is used as one. */
const val FIT_SLACK: Float = 4f

private const val MIN_SCALE = 0.1f
private const val MIN_EM = 0.1f
