package com.callbackdev.tetto.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.tetto.core.domain.widget.GlanceFit
import com.callbackdev.tetto.core.domain.widget.WidgetType
import com.callbackdev.tetto.core.model.CapState

/*
 * A cap as a card's row draws it (PLANNING.md §7): its icon, its figure, its name, its slim bar
 * with the mark for today; and the count of the caps that do not fit. «At a glance»'s row and the
 * other caps at the foot of «In words» (owner, 14 Nov 2026) draw them alike.
 */

/**
 * What a card's caps are drawn from: their words, the reader's text size, the inks; and each cap's
 * touch, the numpad over the home screen on it, with the sentence TalkBack reads for it. Shared by
 * «At a glance» and the row of other caps under «In words», so a cap reads the same on both.
 */
internal class SlotParts(val context: Context, val model: WidgetModel, val palette: WidgetPalette) {
    val words = CardWords(context, model.currency, model.today, hidden = model.look.hideAmounts)
    val scale = fontScale(context)
    val density = context.resources.displayMetrics.density

    fun ink(state: CapState): ColorProvider = if (state.over) palette.overInk else palette.primaryInk

    /** A cap's touch: the numpad over the home screen on it; and what TalkBack reads for it. */
    fun cap(state: CapState): GlanceModifier = GlanceModifier
        .clickable(actionStartActivity(WidgetIntents.quickAdd(context, state.cap.id)))
        .semantics { contentDescription = words.description(state) }
}

/** A cap's symbol in the card's primary ink, [side] square. */
@Composable
internal fun CapIcon(parts: SlotParts, state: CapState, side: Dp) {
    val color = parts.palette.primary
    val bitmap = remember(state.cap.icon, side, parts.density, color) {
        iconBitmap(state.cap.icon, side, parts.density, color)
    }
    Image(provider = ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(side))
}

/**
 * The slim bar: the track, what was spent in the accent (the *attention* ink when spending runs
 * ahead of the days, the over ink full past the cap), and a thin mark where today falls in the
 * period, over a sliver of the card's own ground so it reads across the fill. Three boxes, no
 * bitmap: a bar is what a card can build from boxes.
 */
@Composable
internal fun CapBar(parts: SlotParts, state: CapState, width: Dp) {
    val palette = parts.palette
    val spec = RingSpec.of(state)
    val ink = when {
        spec.over -> palette.over
        spec.ahead -> palette.attention
        else -> palette.accent
    }
    val bar = GlanceFit.BAR_HEIGHT.dp
    val overhang = GlanceFit.MARK_OVERHANG.dp
    Box(
        modifier = GlanceModifier.width(width).height(bar + overhang * 2),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxWidth().height(
                bar,
            ).cornerRadius(bar / 2).background(ColorProvider(palette.track)),
        ) {}
        val fill = width * spec.spent
        if (fill.value >= 1f) {
            Box(
                modifier = GlanceModifier.width(fill).height(bar).cornerRadius(bar / 2).background(ColorProvider(ink)),
            ) {
            }
        }
        val mark = spec.mark
        if (mark != null) {
            val halo = palette.halo
            val markWidth = if (halo != null) MARK_WITH_HALO else MARK_WIDTH
            val at = (width * mark - markWidth / 2).coerceIn(0.dp, width - markWidth)
            Row(modifier = GlanceModifier.width(width).height(bar + overhang * 2)) {
                Spacer(modifier = GlanceModifier.width(at))
                Box(
                    modifier = GlanceModifier.width(markWidth).height(bar + overhang * 2)
                        .background(ColorProvider(halo ?: palette.primary)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (halo != null) {
                        Box(
                            modifier = GlanceModifier.width(MARK_WIDTH).height(bar + overhang * 2)
                                .background(ColorProvider(palette.primary)),
                        ) {}
                    }
                }
            }
        }
    }
}

/**
 * A cap in a row's slot, [width] wide: its icon and figure, its name where [names] (over, the word
 * "over" in its place: what is over is the first thing to know of it), and its bar.
 */
@Composable
internal fun CapSlot(parts: SlotParts, state: CapState, width: Dp, names: Boolean) {
    // Without a line for the name, an over cap says it in its figure: "€30 over". Hidden, the
    // figure's place says where the cap is ("Over"), and the name keeps its line.
    val hidden = parts.words.hidden
    val figure = if (state.over && !names && !hidden) parts.words.figure(state) else parts.words.amount(state)
    val em = textEm(parts.context, figure, TextWeight.MEDIUM)
    val (sp, icon) = if (names) {
        GlanceFit.figureBesideIcon(width.value, em, parts.scale, GlanceFit.SLOT_FIGURE_SP, GlanceFit.SLOT_ICON)
    } else {
        // Without its name, the icon is all that says which cap it is: it stays, the figure yields.
        GlanceFit.figureKeepingIcon(width.value, em, parts.scale, GlanceFit.SLOT_FIGURE_SP, GlanceFit.SLOT_ICON) to true
    }
    Column(
        modifier = GlanceModifier.width(width).then(parts.cap(state)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon) {
                CapIcon(parts, state, GlanceFit.SLOT_ICON.dp)
                Spacer(modifier = GlanceModifier.width(GlanceFit.ICON_GAP.dp))
            }
            Text(
                text = figure,
                style = TextStyle(color = parts.ink(state), fontSize = sp.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (names) {
            Text(
                text = if (state.over && !hidden) parts.words.word(state) else state.cap.name,
                style = TextStyle(
                    color = if (state.over && !hidden) parts.palette.overInk else parts.palette.secondaryInk,
                    fontSize = GlanceFit.SLOT_NAME_SP.sp,
                    fontWeight = if (state.over && !hidden) FontWeight.Medium else FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(GlanceFit.BAR_GAP.dp))
        CapBar(parts, state, width)
    }
}

/** The caps that do not fit, counted: "3 more", or "+3" in a row's narrow slot. */
@Composable
internal fun MoreCaps(parts: SlotParts, count: Int, width: Dp, short: Boolean) {
    Box(modifier = GlanceModifier.width(width), contentAlignment = Alignment.CenterStart) {
        Text(
            text = parts.words.more(count, short),
            style = TextStyle(
                color = parts.palette.secondaryInk,
                fontSize = WidgetType.BODY.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
}

private val MARK_WIDTH = 2.dp
private val MARK_WITH_HALO = 4.dp
