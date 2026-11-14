package com.callbackdev.tetto.widget.words

import android.content.Context
import android.text.format.DateFormat
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
import androidx.glance.layout.padding
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
import com.callbackdev.tetto.core.domain.widget.PeriodStrip
import com.callbackdev.tetto.core.domain.widget.StripUnit
import com.callbackdev.tetto.core.domain.widget.WidgetCaps
import com.callbackdev.tetto.core.domain.widget.WidgetType
import com.callbackdev.tetto.core.domain.widget.WordsForm
import com.callbackdev.tetto.core.domain.widget.WordsPlan
import com.callbackdev.tetto.core.domain.widget.WordsSpec
import com.callbackdev.tetto.core.domain.widget.widthOf
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.WordsBelow
import com.callbackdev.tetto.widget.CardModels
import com.callbackdev.tetto.widget.CardWords
import com.callbackdev.tetto.widget.MessageContent
import com.callbackdev.tetto.widget.R
import com.callbackdev.tetto.widget.RingInks
import com.callbackdev.tetto.widget.RingPainter
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
import com.callbackdev.tetto.widget.glance.GlanceParts
import com.callbackdev.tetto.widget.glance.More
import com.callbackdev.tetto.widget.glance.Slot
import com.callbackdev.tetto.widget.measureWidgetLines
import com.callbackdev.tetto.widget.rememberWidgetModel
import com.callbackdev.tetto.widget.textEm
import com.callbackdev.tetto.widget.widgetDressFor
import com.callbackdev.tetto.widget.widgetLocale
import java.time.format.DateTimeFormatter

/**
 * «In words», the family's ring card (PLANNING.md §7): one cap, its ring the family's circle for
 * the card's size, the cap's icon inside it, and the words. The forms and their arithmetic are
 * [GlanceFit]'s; this file draws them. The whole card is the cap: a touch adds a spend to it.
 *
 * The family's ranks (owner, 14 Nov 2026, PLANNING.md §15 «The family's ring card»): the figure
 * alone is the hero, the word that says what it is opens the line under it, the sentence is Medium
 * in the strong ink, the cap's name is the eyebrow beside a tall card's ring.
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the ring: what was spent, the mark for today, the icon (one cell: the figure) | the family's circle | – | accent, attention, over |
 * | 2 | the figure ("€12", "€30") | [WidgetType.HERO], smaller only where the width cannot hold it | Bold | strong; over |
 * | 3 | what it is: "left of €20 · until Sunday", "left · Coffee · until Sunday" | [WidgetType.BODY] | Regular | quiet |
 * | 4 | the cap's sentence (a tall card), the pace (a row's far edge) | [WidgetType.BODY] | Medium | strong |
 * | 5 | the cap's name, beside a tall card's ring | [WidgetType.BODY] | Medium | quiet |
 * | 6 | under the cap on a wide card: the period's days, or the other caps | labels [WidgetType.CAPTION] | – | – |
 */
class WordsWidget : GlanceAppWidget() {
    /** Exact, so [LocalSize] is the size the launcher granted: every form is read off it. */
    override val sizeMode: SizeMode = SizeMode.Exact

    /** The picker's generated preview (API 35+): the default placement and a tall card. */
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetSamples.FourByOne, WidgetSamples.TwoByTwo))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrDefault(0)
        val models = CardModels(context, appWidgetId)
        // Read before the load, so only a change after it reloads (WidgetRefresh).
        val loadedAt = WidgetRefresh.revision.value
        val initial = models.load()
        provideContent {
            WordsCardContent(rememberWidgetModel(appWidgetId, initial, loadedAt) { models.load() })
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent { WordsCardContent(WidgetSamples.model(context, words = true)) }
    }

    /** Tetto's card that says so, and the reason in Tetto's log, instead of Glance's blank box. */
    override fun onCompositionError(context: Context, glanceId: GlanceId, appWidgetId: Int, throwable: Throwable) {
        WidgetFailure.show(context, appWidgetId, throwable)
    }
}

class WordsWidgetReceiver : TettoWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WordsWidget()
}

/**
 * The whole card for [model] at [LocalSize]: split off the widget so the settings screen's preview
 * and the tests draw the same composition the launcher does.
 */
@Composable
fun WordsCardContent(model: WidgetModel) {
    val context = LocalContext.current
    val size = LocalSize.current
    val dress =
        remember(model.settings.palette, model.settings.dynamicColor) { widgetDressFor(context, model.settings) }
    val state = WidgetCaps.words(model.caps, model.look.wordsCap)
    val form = GlanceFit.wordsForm(size.width.value, size.height.value)
    val tall = size.height.value >= GlanceFit.TALL_MIN_HEIGHT
    val (horizontal, vertical) = when (form) {
        WordsForm.CELL -> GlanceFit.CELL_PADDING to if (tall) GlanceFit.CARD_PADDING else GlanceFit.CARD_PADDING_SNUG
        WordsForm.ROW -> GlanceFit.CARD_PADDING to GlanceFit.CARD_PADDING_SNUG
        WordsForm.TALL, WordsForm.PANEL -> GlanceFit.CARD_PADDING to GlanceFit.CARD_PADDING
    }
    WidgetCard(model, dress, horizontal.dp, vertical.dp) { palette ->
        when {
            model.unavailable -> MessageContent(
                context.getString(R.string.widget_unavailable_title),
                context.getString(R.string.widget_unavailable_hint),
                palette,
            )

            state == null && model.caps.isEmpty() -> MessageContent(
                context.getString(R.string.widget_no_caps_title),
                context.getString(R.string.widget_no_caps_hint),
                palette,
            )

            state == null -> MessageContent(
                context.getString(R.string.widget_gone_title),
                context.getString(R.string.widget_gone_hint).takeIf { form != WordsForm.CELL },
                palette,
            )

            else -> {
                val parts = WordsParts(context, model, state, palette)
                val plan = GlanceFit.wordsPlan(parts.spec(size))
                // On a panel of caps each cap below is a door of its own; the card's door is the
                // header's, so the touches do not nest.
                val panelCaps = plan.form == WordsForm.PANEL && plan.columns > 0
                val door = GlanceModifier
                    .clickable(actionStartActivity(WidgetIntents.quickAdd(context, state.cap.id)))
                    .semantics { contentDescription = parts.words.description(state) }
                Box(modifier = GlanceModifier.fillMaxSize().then(if (panelCaps) GlanceModifier else door)) {
                    when (plan.form) {
                        WordsForm.CELL -> CellContent(parts, plan)
                        WordsForm.ROW -> RowContent(parts, plan)
                        WordsForm.TALL -> TallContent(parts, plan, size)
                        WordsForm.PANEL -> PanelContent(parts, plan, size, if (panelCaps) door else GlanceModifier)
                    }
                }
            }
        }
    }
}

/** What every form draws from: the cap, its words, measured in the launcher's face. */
private class WordsParts(
    val context: Context,
    val model: WidgetModel,
    val state: CapState,
    val palette: WidgetPalette,
) {
    val words = CardWords(context, model.currency, model.today, hidden = model.look.hideAmounts)
    val scale = fontScale(context)
    val density = context.resources.displayMetrics.density

    /** The figure alone ("€12"): the hero of every form but a tall card with no room for the name. */
    val amount = words.amount(state)

    /** "€12 left": the hero where the name takes the line the word would open ([GlanceFit.tallNameBeside]). */
    val figure = words.figure(state)

    /** A row's hero: the figure alone; hidden, where the cap is in its full words ("Ahead of the days"). */
    val rowHero = if (words.hidden) figure else amount
    val rowLines = words.rowLinesWithWord(state)
    val paceForms = words.paceForms(state)
    val noteForms = words.noteForms(state)

    /** Inside a one-cell ring, fullest first ([CardWords.insideForms]). */
    val insideForms: List<String> = words.insideForms(state)

    val figureInk get() = if (state.over) palette.overInk else palette.primaryInk

    /** The other caps a wide card shows under this one, as the reader picked them. */
    val below: List<CapState> = if (model.look.wordsBelow == WordsBelow.CAPS) {
        WidgetCaps.below(model.caps, state, model.look.wordsBelowCaps)
    } else {
        emptyList()
    }

    /** The cap's period, box by box, for a wide card that shows it. */
    val strip: PeriodStrip? = if (model.look.wordsBelow == WordsBelow.DAYS && !state.ended) {
        PeriodStrip.of(state.period, model.today)
    } else {
        null
    }

    /** A tall card's hero and last words, set by whether the name stands beside the ring. */
    var tallHero: String = amount
        private set
    var lastWords: List<String> = emptyList()
        private set

    fun spec(size: DpSize): WordsSpec {
        val inner = (size.width.value - GlanceFit.CARD_PADDING * 2).dp
        val base = WordsSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            sentence = model.look.showSentence,
            heroEm = 0f,
            rowFigureEm = 0f,
            cellFigureEms = emptyList(),
            rowLineEms = emptyList(),
        )
        val nameBeside = GlanceFit.tallNameBeside(base)
        // With the name beside the ring the figure stands alone and the word opens the last words;
        // with the name in the last words, the figure keeps its word ("€12 left").
        // Hidden, the hero is where the cap is, in its full words ("Over its cap"): no figure to
        // qualify, the room is the words'.
        tallHero = if (nameBeside && !words.hidden) amount else figure
        lastWords = if (nameBeside) words.lastWordsWithWord(state) else words.lastWords(state, withName = true)
        return WordsSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            sentence = model.look.showSentence,
            heroEm = textEm(context, tallHero, TextWeight.BOLD),
            rowFigureEm = textEm(context, rowHero, TextWeight.BOLD),
            cellFigureEms = insideForms.map { textEm(context, it, TextWeight.BOLD) },
            rowLineEms = rowLines.map { textEm(context, it, TextWeight.REGULAR) },
            nameWordEm = state.cap.name.split(' ').maxOfOrNull { textEm(context, it, TextWeight.MEDIUM) } ?: 0f,
            paceEms = paceForms.map { textEm(context, it, TextWeight.MEDIUM) },
            paceTwoLineEms = paceForms.map { twoLineEm(context, it, TextWeight.MEDIUM) },
            sentenceLines = noteForms.map {
                measureWidgetLines(context, it, GlanceFit.SENTENCE_SP, inner, TextWeight.MEDIUM)
            },
            detailLines = lastWords.mapIndexed { index, text ->
                val lines = measureWidgetLines(context, text, WidgetType.BODY, inner, TextWeight.REGULAR)
                when (index) {
                    // On one line, or not this form; broken at its separator, each part on its own line.
                    0 -> if (lines == 1) 1 else Int.MAX_VALUE

                    1 -> if (lines == 2) 2 else Int.MAX_VALUE

                    else -> lines
                }
            },
            stripDays = strip?.count ?: 0,
            belowCaps = below.size,
        )
    }
}

/** The ring, the cap's icon in its middle unless [withIcon] is false; the bitmap at the size shown. */
@Composable
private fun Ring(parts: WordsParts, side: Dp, withIcon: Boolean = true) {
    val spec = RingSpec.of(parts.state)
    val inks = RingInks.of(parts.palette).let {
        if (withIcon) it else it.copy(icon = androidx.compose.ui.graphics.Color.Transparent)
    }
    val bitmap = remember(side, parts.density, spec, inks) { RingPainter.paint(side, parts.density, spec, inks) }
    Image(provider = ImageProvider(bitmap), contentDescription = null, modifier = GlanceModifier.size(side))
}

/**
 * One cell: the cell's circle, centred, and inside it the figure ("€12"; over, "€30 over", or the
 * word alone where only it fits: over is said in words, never by the ring's colour alone), as every
 * ring card of the family draws one cell (owner, 14 Nov 2026). The icon where no figure fits.
 */
@Composable
private fun CellContent(parts: WordsParts, plan: WordsPlan) {
    val side = plan.circle.dp
    val inside = parts.insideForms.getOrNull(plan.cellFigure)
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(modifier = GlanceModifier.size(side), contentAlignment = Alignment.Center) {
            Ring(parts, side, withIcon = inside == null)
            if (inside != null) {
                Text(
                    text = inside,
                    style = TextStyle(
                        color = parts.figureInk,
                        fontSize = plan.cellFigureSp.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * One row (and the panel's header, at its height): the ring at the leading edge, the figure over
 * what it is ("left · Coffee · until Sunday"), and the pace at the far edge where the row has the
 * room, Medium in the strong ink, as every ring card's sentence there.
 */
@Composable
private fun RowContent(parts: WordsParts, plan: WordsPlan, modifier: GlanceModifier = GlanceModifier.fillMaxSize()) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (plan.circle > 0f) {
            Ring(parts, plan.circle.dp)
            Spacer(modifier = GlanceModifier.width(GlanceFit.CIRCLE_GAP.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parts.rowHero,
                style = TextStyle(color = parts.figureInk, fontSize = plan.figureSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = parts.rowLines[plan.line],
                style = TextStyle(color = parts.palette.secondaryInk, fontSize = plan.lineSp.sp),
                maxLines = 1,
            )
        }
        if (plan.pace >= 0) {
            Spacer(modifier = GlanceModifier.width(GlanceFit.PACE_GAP.dp))
            Text(
                text = parts.paceForms[plan.pace],
                style = TextStyle(
                    color = parts.palette.primaryInk,
                    fontSize = plan.paceSp.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                ),
                maxLines = plan.paceLines,
                modifier = GlanceModifier.width(plan.paceWidth.dp),
            )
        }
    }
}

/**
 * Two rows and up: the ring in the top trailing corner, the cap's name beside it (the eyebrow,
 * quiet) where it has the room; from the bottom leading corner, the figure at the hero size, what
 * it is ("left of €20 · until Sunday", broken at the separator, or the day alone, where the card is
 * narrow; with the name nowhere else, "Coffee · until Sunday" under "€12 left"), and the cap's
 * sentence: the family's order (owner, 14 Nov 2026).
 */
@Composable
private fun TallContent(parts: WordsParts, plan: WordsPlan, size: DpSize) {
    val inner = size.width.value - GlanceFit.CARD_PADDING * 2
    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        if (plan.circle > 0f) Ring(parts, plan.circle.dp)
        if (plan.nameBeside) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                Text(
                    text = parts.state.cap.name,
                    style = TextStyle(
                        color = parts.palette.secondaryInk,
                        fontSize = plan.nameSp.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = plan.nameLines,
                    modifier = GlanceModifier.width((inner - plan.circle - GlanceFit.CIRCLE_GAP).dp),
                )
            }
        }
        Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = parts.tallHero,
                style = TextStyle(color = parts.figureInk, fontSize = plan.heroSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (plan.detail >= 0) {
                Text(
                    text = parts.lastWords[plan.detail],
                    style = TextStyle(color = parts.palette.secondaryInk, fontSize = WidgetType.BODY.sp),
                    maxLines = plan.detailLines,
                )
            }
            if (plan.sentence >= 0) {
                Text(
                    text = parts.noteForms[plan.sentence],
                    style = TextStyle(
                        color = parts.palette.primaryInk,
                        fontSize = GlanceFit.SENTENCE_SP.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = plan.sentenceLines,
                )
            }
        }
    }
}

/**
 * Two rows and three cells and up, with something to stand under the cap (owner, 14 Nov 2026):
 * the one-row card on top at [WordsPlan.header], its ring the tall card's; under it the cap's
 * period box by box ([DaysStrip]) or the other caps side by side, each its own door ([CapsStrip]).
 * [door] is the cap's touch where the card's own cannot be (under a strip of doors).
 */
@Composable
private fun PanelContent(parts: WordsParts, plan: WordsPlan, size: DpSize, door: GlanceModifier) {
    val inner = (size.width.value - GlanceFit.CARD_PADDING * 2).dp
    Column(modifier = GlanceModifier.fillMaxSize()) {
        RowContent(parts, plan, GlanceModifier.fillMaxWidth().height(plan.header.dp).then(door))
        Spacer(modifier = GlanceModifier.defaultWeight())
        val strip = parts.strip
        if (plan.columns > 0) {
            CapsStrip(parts, plan, inner)
        } else if (strip != null) {
            val tallest = GlanceFit.STRIP_BOXES_MAX + GlanceFit.stripLabelHeight(parts.scale)
            DaysStrip(parts, strip, inner, minOf(plan.strip, tallest).dp)
        }
    }
}

/**
 * The cap's period, a box a day (a year's, a box a month), over their labels: the days gone in the
 * faint ink, today in the ring's own (the attention ink ahead of the days, the over ink past the
 * cap), the days left in the strong ink, half strength. Built from boxes, a row of at most
 * [STRIP_CHUNK] in each container (Glance drops a container's eleventh child).
 */
@Composable
private fun DaysStrip(parts: WordsParts, strip: PeriodStrip, width: Dp, height: Dp) {
    val palette = parts.palette
    val gap = GlanceFit.STRIP_GAP.dp
    val box = (width - gap * (strip.count - 1)) / strip.count
    val labels = GlanceFit.stripLabelHeight(parts.scale).dp
    val today = when {
        parts.state.over -> palette.over
        parts.state.aheadOfDays -> palette.attention
        else -> palette.accent
    }
    val left = palette.primary.copy(alpha = DAYS_LEFT_ALPHA)
    Column(
        modifier = GlanceModifier.width(width).height(height)
            .semantics { contentDescription = parts.words.day(parts.state) },
        verticalAlignment = Alignment.Bottom,
    ) {
        StripChunks(strip.count) { index ->
            val color = when {
                index < strip.today -> palette.track
                index == strip.today -> today
                else -> left
            }
            Box(
                modifier = GlanceModifier
                    .width(box + if (index > 0) gap else 0.dp)
                    .height(height - labels)
                    .padding(start = if (index > 0) gap else 0.dp),
            ) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(STRIP_CORNER)
                        .background(ColorProvider(color)),
                ) {}
            }
        }
        Spacer(modifier = GlanceModifier.height(GlanceFit.STRIP_LABEL_GAP.dp))
        val step = if (strip.count <= STRIP_LABEL_EVERY_MAX) 1 else STRIP_LABEL_STEP
        val names = stripLabels(parts, strip, box)
        val cells = (0 until strip.count step step).toList()
        StripChunks(cells.size) { i ->
            val index = cells[i]
            val span = minOf(step, strip.count - index)
            val cell = box * span + gap * (span - 1)
            val isToday = strip.today in index until index + span && step == 1
            Box(
                modifier = GlanceModifier
                    .width(cell + if (index > 0) gap else 0.dp)
                    .padding(start = if (index > 0) gap else 0.dp),
                contentAlignment = if (step == 1) Alignment.TopCenter else Alignment.TopStart,
            ) {
                Text(
                    // A month's last days, too few for a week's span, keep their boxes unlabelled.
                    text = if (span == step) names[index] else "",
                    style = TextStyle(
                        color = if (isToday) palette.primaryInk else palette.secondaryInk,
                        fontSize = GlanceFit.STRIP_LABEL_SP.sp,
                        fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal,
                        textAlign = if (step == 1) TextAlign.Center else TextAlign.Start,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * Each box's label: a week's day names, short where they fit their box ("Mon"), else a letter; a
 * month's day numbers, every [STRIP_LABEL_STEP]; a year's month names, short or a letter. The
 * locale's own patterns.
 */
private fun stripLabels(parts: WordsParts, strip: PeriodStrip, box: Dp): List<String> {
    val locale = parts.context.widgetLocale()
    fun pattern(skeleton: String) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    if (strip.count > STRIP_LABEL_EVERY_MAX) {
        val day = pattern("d")
        return List(strip.count) { day.format(strip.dayOf(it)) }
    }
    val (short, letter) = when (strip.unit) {
        StripUnit.DAY -> "EEE" to "EEEEE"
        StripUnit.MONTH -> "LLL" to "LLLLL"
    }
    val full = List(strip.count) { pattern(short).format(strip.dayOf(it)).trimEnd('.') }
    val fits = full.all {
        widthOf(textEm(parts.context, it, TextWeight.MEDIUM), GlanceFit.STRIP_LABEL_SP, parts.scale) <= box.value
    }
    return if (fits) full else List(strip.count) { pattern(letter).format(strip.dayOf(it)) }
}

/** [count] children laid in a row, [STRIP_CHUNK] to an inner row, so no container holds eleven. */
@Composable
private fun StripChunks(count: Int, item: @Composable (Int) -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        (0 until count).chunked(STRIP_CHUNK).forEach { chunk ->
            Row(verticalAlignment = Alignment.Bottom) { chunk.forEach { item(it) } }
        }
    }
}

/**
 * The other caps the reader picked, side by side as «At a glance»'s row draws them: the figure, the
 * name (over, the word) and the bar, each opening its own numpad; the last counts the ones that
 * do not fit.
 */
@Composable
private fun CapsStrip(parts: WordsParts, plan: WordsPlan, width: Dp) {
    val glance = GlanceParts(parts.context, parts.model, parts.palette)
    val slots =
        GlancePlan(form = GlanceForm.ROW, snug = false, shown = plan.shown, more = plan.more, columns = plan.columns)
    val slot = (width - GlanceFit.SLOT_GAP.dp * (plan.columns - 1)) / plan.columns
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        parts.below.take(plan.shown).forEachIndexed { index, state ->
            if (index > 0) Spacer(modifier = GlanceModifier.width(GlanceFit.SLOT_GAP.dp))
            Slot(glance, slots, state, slot)
        }
        if (plan.more > 0) {
            Spacer(modifier = GlanceModifier.width(GlanceFit.SLOT_GAP.dp))
            More(glance, plan.more, slot, short = true)
        }
    }
}

/**
 * The width of [text] over two lines, in ems: the narrower of its splits at a space, the wider of
 * each split's two halves. One line's width where it has no space.
 */
internal fun twoLineEm(context: Context, text: String, weight: TextWeight = TextWeight.REGULAR): Float {
    val spaces = text.indices.filter { text[it] == ' ' }
    if (spaces.isEmpty()) return textEm(context, text, weight)
    return spaces.minOf { at ->
        maxOf(
            textEm(context, text.substring(0, at), weight),
            textEm(context, text.substring(at + 1), weight),
        )
    }
}

/** The days left, the strong ink at half strength: what is still to come, next to today's box. */
private const val DAYS_LEFT_ALPHA = 0.5f
private val STRIP_CORNER = 4.dp
private const val STRIP_CHUNK = 8

/** Up to a year's months, every box has its label; a month's days, every seventh. */
private const val STRIP_LABEL_EVERY_MAX = 12
private const val STRIP_LABEL_STEP = 7
