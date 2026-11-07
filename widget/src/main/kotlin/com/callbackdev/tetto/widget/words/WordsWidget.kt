package com.callbackdev.tetto.widget.words

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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.callbackdev.tetto.core.domain.widget.GlanceFit
import com.callbackdev.tetto.core.domain.widget.WidgetCaps
import com.callbackdev.tetto.core.domain.widget.WidgetType
import com.callbackdev.tetto.core.domain.widget.WordsForm
import com.callbackdev.tetto.core.domain.widget.WordsPlan
import com.callbackdev.tetto.core.domain.widget.WordsSpec
import com.callbackdev.tetto.core.domain.widget.quarterPoint
import com.callbackdev.tetto.core.model.CapState
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
import com.callbackdev.tetto.widget.measureWidgetLines
import com.callbackdev.tetto.widget.rememberWidgetModel
import com.callbackdev.tetto.widget.textEm
import com.callbackdev.tetto.widget.widgetDressFor

/**
 * «In words», the family's ring card (PLANNING.md §7): one cap, its ring the family's circle for
 * the card's size, the cap's icon inside it, and the words. The forms and their arithmetic are
 * [GlanceFit]'s; this file draws them. The whole card is the cap: a touch adds a spend to it.
 *
 * | rank | what | size | weight | ink |
 * |---|---|---|---|---|
 * | 1 | the ring: what was spent, the mark for today, the icon | the family's circle | – | accent, attention, over |
 * | 2 | what is left ("€12 left", "€30 over") | the hero on a tall card, 20 sp on a row | Bold; Medium on a row | strong; over |
 * | 3 | the cap's sentence: the pace, ahead of the days, forgiven | [WidgetType.BODY] | Regular | strong |
 * | 4 | "of €20 · until Sunday", "Coffee · until Sunday", the cap's name | [WidgetType.BODY] | Regular; the name Medium | quiet; the name strong |
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
        WordsForm.TALL -> GlanceFit.CARD_PADDING to GlanceFit.CARD_PADDING
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
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity(WidgetIntents.quickAdd(context, state.cap.id)))
                        .semantics { contentDescription = parts.words.description(state) },
                ) {
                    when (plan.form) {
                        WordsForm.CELL -> CellContent(parts, plan)
                        WordsForm.ROW -> RowContent(parts, plan)
                        WordsForm.TALL -> TallContent(parts, plan, size)
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
    val figure = words.figure(state)
    val rowLines = words.rowLines(state)
    val paceForms = words.paceForms(state)
    val noteForms = words.noteForms(state)

    /**
     * Under a one-cell ring: the amount alone, which reads as what is left; over, the words; hidden,
     * the short word for where the cap is.
     */
    val cellForms: List<String> = if (state.over && !words.hidden) listOf(figure) else listOf(words.amount(state))

    val figureInk get() = if (state.over) palette.overInk else palette.primaryInk

    /** The last words' forms on a tall card, set by whether the name stands beside the ring. */
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
        lastWords = words.lastWords(state, withName = !GlanceFit.tallNameBeside(base))
        return WordsSpec(
            width = size.width.value,
            height = size.height.value,
            fontScale = scale,
            sentence = model.look.showSentence,
            heroEm = textEm(context, figure, TextWeight.BOLD),
            rowFigureEm = textEm(context, figure, TextWeight.MEDIUM),
            cellFigureEms = cellForms.map { textEm(context, it, TextWeight.MEDIUM) },
            rowLineEms = rowLines.map { textEm(context, it, TextWeight.REGULAR) },
            nameWordEm = state.cap.name.split(' ').maxOfOrNull { textEm(context, it, TextWeight.MEDIUM) } ?: 0f,
            paceEms = paceForms.map { textEm(context, it, TextWeight.REGULAR) },
            paceTwoLineEms = paceForms.map { twoLineEm(context, it) },
            sentenceLines = noteForms.map {
                measureWidgetLines(context, it, GlanceFit.SENTENCE_SP, inner, TextWeight.REGULAR)
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
 * One cell: the cell's circle, centred, the cap's icon inside; the figure under it where the cell
 * holds it. Over, with no room under the ring, the word takes the icon's place inside it: over is
 * said in words, never by the ring's colour alone.
 */
@Composable
private fun CellContent(parts: WordsParts, plan: WordsPlan) {
    val side = plan.circle.dp
    val overInside = parts.state.over && plan.cellFigure < 0
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = GlanceModifier.size(side), contentAlignment = Alignment.Center) {
            Ring(parts, side, withIcon = !overInside)
            if (overInside) {
                val word = parts.words.word(parts.state)
                val hole = side.value * (1f - RingPainter.STROKE_SHARE * 2) * INSIDE_FILL
                val sp = quarterPoint(
                    minOf(
                        WidgetType.LABEL,
                        hole / (textEm(parts.context, word, TextWeight.BOLD) * parts.scale),
                    ),
                )
                Text(
                    text = word,
                    style = TextStyle(color = parts.palette.overInk, fontSize = sp.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        if (plan.cellFigure >= 0) {
            Spacer(modifier = GlanceModifier.height(GlanceFit.CELL_GAP.dp))
            Text(
                text = parts.cellForms[plan.cellFigure],
                style = TextStyle(
                    color = parts.figureInk,
                    fontSize = plan.cellFigureSp.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * One row: the ring at the leading edge, what is left over the cap's name and its last day, and
 * the pace at the far edge where the row has the room.
 */
@Composable
private fun RowContent(parts: WordsParts, plan: WordsPlan) {
    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        if (plan.circle > 0f) {
            Ring(parts, plan.circle.dp)
            Spacer(modifier = GlanceModifier.width(GlanceFit.CIRCLE_GAP.dp))
        }
        Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = parts.figure,
                style = TextStyle(color = parts.figureInk, fontSize = plan.figureSp.sp, fontWeight = FontWeight.Medium),
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
                    color = parts.palette.secondaryInk,
                    fontSize = plan.paceSp.sp,
                    textAlign = TextAlign.End,
                ),
                maxLines = plan.paceLines,
                modifier = GlanceModifier.width(plan.paceWidth.dp),
            )
        }
    }
}

/**
 * Two rows and up: the ring in the top trailing corner, the cap's name beside it where it has the
 * room; from the bottom leading corner, what is left at the hero size, the cap's sentence, and
 * its last words: "of €20 · until Sunday" (or, with the name nowhere else, "Coffee · until
 * Sunday"), broken at the separator, or the day alone, where the card is narrow.
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
                        color = parts.palette.primaryInk,
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
                text = parts.figure,
                style = TextStyle(color = parts.figureInk, fontSize = plan.heroSp.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            if (plan.sentence >= 0) {
                Text(
                    text = parts.noteForms[plan.sentence],
                    style = TextStyle(color = parts.palette.primaryInk, fontSize = GlanceFit.SENTENCE_SP.sp),
                    maxLines = plan.sentenceLines,
                )
            }
            if (plan.detail >= 0) {
                Text(
                    text = parts.lastWords[plan.detail],
                    style = TextStyle(color = parts.palette.secondaryInk, fontSize = WidgetType.BODY.sp),
                    maxLines = plan.detailLines,
                )
            }
        }
    }
}

/**
 * The width of [text] over two lines, in ems: the narrower of its splits at a space, the wider of
 * each split's two halves. One line's width where it has no space.
 */
internal fun twoLineEm(context: Context, text: String): Float {
    val spaces = text.indices.filter { text[it] == ' ' }
    if (spaces.isEmpty()) return textEm(context, text, TextWeight.REGULAR)
    return spaces.minOf { at ->
        maxOf(
            textEm(context, text.substring(0, at), TextWeight.REGULAR),
            textEm(context, text.substring(at + 1), TextWeight.REGULAR),
        )
    }
}

/** How much of the ring's hole the word inside it may fill. */
private const val INSIDE_FILL = 0.8f
