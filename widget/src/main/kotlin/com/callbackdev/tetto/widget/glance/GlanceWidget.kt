package com.callbackdev.tetto.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.tetto.core.domain.widget.GlanceFit
import com.callbackdev.tetto.core.domain.widget.GlanceForm
import com.callbackdev.tetto.core.domain.widget.GlancePlan
import com.callbackdev.tetto.core.domain.widget.GlanceSpec
import com.callbackdev.tetto.core.domain.widget.WidgetCaps
import com.callbackdev.tetto.core.domain.widget.WidgetType
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.widget.CardModels
import com.callbackdev.tetto.widget.CardWords
import com.callbackdev.tetto.widget.MessageContent
import com.callbackdev.tetto.widget.R
import com.callbackdev.tetto.widget.RingSpec
import com.callbackdev.tetto.widget.TettoWidgetReceiver
import com.callbackdev.tetto.widget.TextWeight
import com.callbackdev.tetto.widget.WidgetCard
import com.callbackdev.tetto.widget.WidgetFailure
import com.callbackdev.tetto.widget.WidgetIntents
import com.callbackdev.tetto.widget.WidgetModel
import com.callbackdev.tetto.widget.WidgetPalette
import com.callbackdev.tetto.widget.WidgetRefresh
import com.callbackdev.tetto.widget.WidgetSamples
import com.callbackdev.tetto.widget.fontScale
import com.callbackdev.tetto.widget.iconBitmap
import com.callbackdev.tetto.widget.rememberWidgetModel
import com.callbackdev.tetto.widget.textEm
import com.callbackdev.tetto.widget.widgetDressFor

/**
 * «At a glance» (PLANNING.md §7): the caps the card shows, as many as its size allows, each with its
 * icon, what is left and a slim bar with the mark for today; never a scrolling list, the rest
 * counted. A touch on a cap adds a spend to it; the rest of the card opens the caps screen. The
 * forms and their arithmetic are [GlanceFit]'s; this file draws them.
 */
class GlanceWidget : GlanceAppWidget() {
    /** Exact, so [LocalSize] is the size the launcher granted: every form is read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /** The picker's generated preview (API 35+): the default placement and a row. */
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByTwo, WidgetSamples.FourByOne))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            GlanceCardContent(rememberWidgetModel(appWidgetId, initial, loadedAt) { models.load() })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { GlanceCardContent(WidgetSamples.model(context)) }
    }

    /** Tetto's card that says so, and the reason in Tetto's log, instead of Glance's blank box. */
    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        WidgetFailure.show(context, appWidgetId, throwable)
    }
}

class GlanceWidgetReceiver : TettoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceWidget()
}

/**
 * The whole card for [model] at [LocalSize]: split off the widget so the settings screen's preview,
 * the first run's and the tests draw the same composition the launcher does.
 */
@Composable
fun GlanceCardContent(model: WidgetModel) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val states = WidgetCaps.glance(model.caps, model.look.glanceCaps)
    val form = GlanceFit.glanceForm(size.width.value, size.height.value)
    val tall = size.height.value >= GlanceFit.TALL_MIN_HEIGHT
    val (horizontal, vertical) = when (form) {
        GlanceForm.CELL -> GlanceFit.CELL_PADDING to if (tall) GlanceFit.CARD_PADDING else GlanceFit.CARD_PADDING_SNUG
        GlanceForm.ROW -> GlanceFit.CARD_PADDING to GlanceFit.CARD_PADDING_SNUG
        GlanceForm.GRID -> GlanceFit.CARD_PADDING to GlanceFit.CARD_PADDING
    }
    WidgetCard(model, dress, horizontal.dp, vertical.dp) { palette ->
        when {
            model.unavailable -> MessageContent(
                context.getString(R.string.widget_unavailable_title),
                context.getString(R.string.widget_unavailable_hint),
                palette,
            )

            states.isEmpty() -> MessageContent(
                context.getString(R.string.widget_no_caps_title),
                context.getString(R.string.widget_no_caps_hint).takeIf { form != GlanceForm.CELL },
                palette,
            )

            else -> {
                val parts = GlanceParts(context, model, palette)
                val plan = GlanceFit.glancePlan(
                    GlanceSpec(
                        width = size.width.value,
                        height = size.height.value,
                        fontScale = parts.scale,
                        caps = states.size,
                        cellFigureEm = textEm(context, parts.words.amount(states.first()), TextWeight.BOLD),
                    ),
                )
                when (plan.form) {
                    GlanceForm.CELL -> CellContent(parts, plan, states.first(), size)
                    GlanceForm.ROW -> RowContent(parts, plan, states, size)
                    GlanceForm.GRID -> GridContent(parts, plan, states, size)
                }
            }
        }
    }
}

/** What every form draws from. */
internal class GlanceParts(val context: Context, val model: WidgetModel, val palette: WidgetPalette) {
    val words = CardWords(context, model.currency, model.today, hidden = model.look.hideAmounts)
    val scale = fontScale(context)
    val density = context.resources.displayMetrics.density

    fun ink(state: CapState): ColorProvider = if (state.over) palette.overInk else palette.primaryInk

    /** A cap's touch: the numpad over the home screen on it; and what TalkBack reads for it. */
    fun cap(state: CapState): GlanceModifier = GlanceModifier
        .clickable(actionStartActivity(WidgetIntents.quickAdd(context, state.cap.id)))
        .semantics { contentDescription = words.description(state) }
}

@Composable
internal fun Icon(parts: GlanceParts, state: CapState, side: Dp) {
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
internal fun Bar(parts: GlanceParts, state: CapState, width: Dp) {
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
 * One cell, one cap, its figure the hero, sized to the cell; the word ("left", "over") under it
 * (over it, in a language that says it first), the icon on top and the bar at the bottom where the
 * height leaves them.
 */
@Composable
private fun CellContent(parts: GlanceParts, plan: GlancePlan, state: CapState, size: DpSize) {
    val width = (size.width.value - GlanceFit.CELL_PADDING * 2).dp
    // Hidden, the hero already says where the cap is: the line under it names the cap instead.
    val hidden = parts.words.hidden
    val word = @Composable {
        Text(
            text = if (hidden) state.cap.name else parts.words.word(state),
            style = TextStyle(
                color = if (state.over) parts.palette.overInk else parts.palette.secondaryInk,
                fontSize = GlanceFit.CELL_WORD_SP.sp,
                fontWeight = if (state.over) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
    Column(
        modifier = GlanceModifier.fillMaxSize().then(parts.cap(state)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (plan.cellIcon) {
            Icon(parts, state, GlanceFit.CELL_ICON.dp)
            Spacer(modifier = GlanceModifier.height(GlanceFit.CELL_ICON_GAP.dp))
        }
        if (parts.words.wordAbove) word()
        Text(
            text = parts.words.amount(state),
            style = TextStyle(
                color = parts.ink(state),
                fontSize = plan.cellFigureSp.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
        if (!parts.words.wordAbove) word()
        if (plan.cellBar) {
            Spacer(modifier = GlanceModifier.height(GlanceFit.BAR_GAP.dp))
            Bar(parts, state, width)
        }
    }
}

/**
 * One row: the caps side by side, each slot its icon and figure, its name (over, the word "over"
 * in its place: what is over is the first thing to know of it) and its bar; the last slot counts
 * the caps that do not fit.
 */
@Composable
private fun RowContent(parts: GlanceParts, plan: GlancePlan, states: List<CapState>, size: DpSize) {
    val inner = size.width.value - GlanceFit.CARD_PADDING * 2
    val slot = ((inner - GlanceFit.SLOT_GAP * (plan.columns - 1)) / plan.columns).dp
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        states.take(plan.shown).forEachIndexed { index, state ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(GlanceFit.SLOT_GAP.dp))
            Slot(parts, plan, state, slot)
        }
        if (plan.more > 0) {
            Spacer(modifier = GlanceModifier.width(GlanceFit.SLOT_GAP.dp))
            More(parts, plan.more, slot, short = true)
        }
    }
}

@Composable
internal fun Slot(parts: GlanceParts, plan: GlancePlan, state: CapState, width: Dp) {
    // Without a line for the name, an over cap says it in its figure: "€30 over". Hidden, the
    // figure's place says where the cap is ("Over"), and the name keeps its line.
    val hidden = parts.words.hidden
    val figure = if (state.over && !plan.names && !hidden) parts.words.figure(state) else parts.words.amount(state)
    val (sp, icon) = GlanceFit.figureBesideIcon(
        width.value,
        textEm(parts.context, figure, TextWeight.MEDIUM),
        parts.scale,
        GlanceFit.SLOT_FIGURE_SP,
        GlanceFit.SLOT_ICON,
    )
    Column(
        modifier = GlanceModifier.width(width).then(parts.cap(state)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon) {
                Icon(parts, state, GlanceFit.SLOT_ICON.dp)
                Spacer(modifier = GlanceModifier.width(GlanceFit.ICON_GAP.dp))
            }
            Text(
                text = figure,
                style = TextStyle(color = parts.ink(state), fontSize = sp.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (plan.names) {
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
        Bar(parts, state, width)
    }
}

/** The caps that do not fit, counted: "3 more", or "+3" in a row's narrow slot. */
@Composable
internal fun More(parts: GlanceParts, count: Int, width: Dp, short: Boolean) {
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

/**
 * Two rows and up: the caps in rows and columns, each item its icon, name, figure and bar, on one
 * line where the column is wide enough, else the name over the figure; the last item counts the
 * caps that do not fit.
 */
@Composable
private fun GridContent(parts: GlanceParts, plan: GlancePlan, states: List<CapState>, size: DpSize) {
    val inner = size.width.value - GlanceFit.CARD_PADDING * 2
    val column = ((inner - GlanceFit.GRID_COLUMN_GAP * (plan.columns - 1)) / plan.columns).dp
    val shown = states.take(plan.shown)
    val cells: List<CapState?> = shown + if (plan.more > 0) listOf(null) else emptyList()
    Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        cells.chunked(plan.columns).forEachIndexed { row, items ->
            if (row > 0) Spacer(modifier = GlanceModifier.height(GlanceFit.GRID_ROW_GAP.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, state ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(GlanceFit.GRID_COLUMN_GAP.dp))
                    if (state == null) {
                        More(parts, plan.more, column, short = false)
                    } else {
                        Item(parts, plan, state, column)
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(parts: GlanceParts, plan: GlancePlan, state: CapState, width: Dp) {
    val icon = GlanceFit.ITEM_ICON.dp
    // Over is said in words: "€30 over", in the over ink; under, the figure alone reads as what is
    // left. Hidden, the short word for where the cap is, "Over" among them.
    val figure = when {
        plan.inline -> parts.words.besideName(state)
        state.over && !parts.words.hidden -> parts.words.figure(state)
        else -> parts.words.amount(state)
    }
    // On one line with its figure, the name steps down before the figure's words cut it.
    val nameSp = if (plan.inline) {
        GlanceFit.itemNameSp(
            column = width.value,
            nameEm = textEm(parts.context, state.cap.name, TextWeight.REGULAR),
            figureEm = textEm(parts.context, figure, TextWeight.MEDIUM),
            fontScale = parts.scale,
        )
    } else {
        GlanceFit.ITEM_NAME_SP
    }
    val name = @Composable { modifier: GlanceModifier ->
        Text(
            text = state.cap.name,
            style = TextStyle(color = parts.palette.primaryInk, fontSize = nameSp.sp),
            maxLines = 1,
            modifier = modifier,
        )
    }
    val figureText = @Composable {
        Text(
            text = figure,
            style = TextStyle(
                color = parts.ink(state),
                fontSize = GlanceFit.ITEM_FIGURE_SP.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
    }
    Column(modifier = GlanceModifier.width(width).then(parts.cap(state))) {
        Row(modifier = GlanceModifier.width(width), verticalAlignment = Alignment.CenterVertically) {
            Icon(parts, state, icon)
            Spacer(modifier = GlanceModifier.width(GlanceFit.ICON_GAP.dp))
            name(GlanceModifier.defaultWeight())
            if (plan.inline) {
                Spacer(modifier = GlanceModifier.width(GlanceFit.ITEM_FIGURE_GAP.dp))
                figureText()
            }
        }
        if (!plan.inline) figureText()
        Spacer(modifier = GlanceModifier.height(GlanceFit.BAR_GAP.dp))
        Bar(parts, state, width)
    }
}

private val MARK_WIDTH = 2.dp
private val MARK_WITH_HALO = 4.dp
