package com.callbackdev.tetto.core.domain.widget

import kotlin.math.floor

/*
 * The widgets' arithmetic (PLANNING.md §6, §7; `docs/adr/0003-widgets.md`): given the size the
 * launcher really granted, in dp, the reader's text size, and what only the widget's process can
 * measure (how wide a figure is in ems of the launcher's face, how many lines a sentence takes),
 * which form a card takes and what it holds. Pure, so a test pins every form at the family's
 * reference grants: one row ≈ 85 dp tall, two ≈ 189, three ≈ 293; one cell ≈ 85 dp wide, two ≈
 * 159, three ≈ 250, four ≈ 340; and the owner's two-by-two, 188 × 218.
 *
 * «In words» is the family's ring card: its circle is **the family's circle** ([cellCircle],
 * [rowCircle], [tallCircle]), copied constant for constant, sized by the card and never by the
 * day's words, so it stands in line with the sisters' ring cards of the same size (owner,
 * 5 Nov 2026). A change to those three functions or their constants is the family's, decided
 * with the owner, never Tetto's alone.
 */

/**
 * The forms of «In words» (PLANNING.md §7). [PANEL], two rows high and three cells wide or more
 * (owner, 14 Nov 2026): the one-row card on top, its circle the tall card's, and under it the cap's
 * period day by day or the other caps the reader picked.
 */
enum class WordsForm { CELL, ROW, TALL, PANEL }

/**
 * What «In words» is asked to hold, measured by the caller in the launcher's face.
 *
 * @property sentence the reader's «The sentence» switch: off, a tall card says the amount and its
 *   cap alone, and its circle takes the band the sentence leaves.
 * @property heroEm what is left ("€12 left", "€30 over") on one line, in ems of the bold face.
 * @property rowFigureEm the same words in ems of the medium face, the row's first line.
 * @property cellFigureEms what a one-cell card may say under its ring, fullest first ("€30 over",
 *   then "over"), in ems of the medium face; empty for none.
 * @property rowLineEms the row's second line, fullest first ("Coffee · until Monday", "until Monday",
 *   "Coffee"), in ems of the regular face.
 * @property nameWordEm the widest word of the cap's name, in ems of the medium face: a name beside
 *   a tall circle steps down before a word of it is broken.
 * @property paceEms the pace for a row's far edge, fullest first ("about €4 a day", "€4 a day"),
 *   one line each, in ems of the regular face; empty where there is none.
 * @property paceTwoLineEms the same over two lines: the wider of the two, in ems.
 * @property sentenceLines the lines each form of the cap's sentence takes across a tall card's
 *   inner width at [GlanceFit.SENTENCE_SP], fullest first; empty where the cap has none to say.
 * @property detailLines the lines each form of a tall card's last words takes across its inner
 *   width at [WidgetType.BODY], fullest first: on one line ("of €20 · until Sunday"), broken at
 *   its separator ("of €20" over "until Sunday"), the day alone ("until Sunday"); with the name
 *   nowhere else ([GlanceFit.tallNameBeside] false), the name in the cap's place. A form that cannot
 *   take its lines (the one-line form over two, a broken form's line over two) is [Int.MAX_VALUE].
 * @property stripDays the boxes of the cap's period a wide card may draw under it ([PeriodStrip]);
 *   0 for none.
 * @property belowCaps the other caps the reader picked for under it, as many as there are; 0 for
 *   none. With either, a card wide and tall enough is a [WordsForm.PANEL].
 */
data class WordsSpec(
    val width: Float,
    val height: Float,
    val fontScale: Float,
    val sentence: Boolean,
    val heroEm: Float,
    val rowFigureEm: Float,
    val cellFigureEms: List<Float>,
    val rowLineEms: List<Float>,
    val nameWordEm: Float = 0f,
    val paceEms: List<Float> = emptyList(),
    val paceTwoLineEms: List<Float> = paceEms,
    val sentenceLines: List<Int> = emptyList(),
    val detailLines: List<Int> = listOf(1),
    val stripDays: Int = 0,
    val belowCaps: Int = 0,
)

/**
 * The card's plan. The circle's side in dp (0: none). On one cell, which figure under it
 * ([cellFigure] into [WordsSpec.cellFigureEms], -1: none). On a row, the figure's size and the
 * second line's; the pace at the far edge ([pace] into [WordsSpec.paceEms], -1: none) over
 * [paceLines] lines, [paceWidth] wide. On a tall card, the hero's size; the cap's name beside the
 * circle over at most [nameLines] lines, or, [nameBeside] false, in the detail line with the cap's
 * last day; which sentence ([sentence] into [WordsSpec.sentenceLines], -1: none) over
 * [sentenceLines] lines; and which form of the last words ([detail] into [WordsSpec.detailLines],
 * -1: none) over [detailLines] lines.
 */
data class WordsPlan(
    val form: WordsForm,
    val circle: Float,
    val snug: Boolean,
    val sidePadding: Float,
    val cellFigure: Int = -1,
    val cellFigureSp: Float = GlanceFit.CELL_FIGURE_SP,
    val figureSp: Float = GlanceFit.ROW_FIGURE_SP,
    val lineSp: Float = GlanceFit.ROW_LINE_SP,
    val line: Int = 0,
    val pace: Int = -1,
    val paceSp: Float = GlanceFit.PACE_SP,
    val paceLines: Int = 0,
    val paceWidth: Float = 0f,
    val heroSp: Float = WidgetType.HERO,
    val nameBeside: Boolean = true,
    val nameSp: Float = WidgetType.BODY,
    val nameLines: Int = 0,
    val sentence: Int = -1,
    val sentenceLines: Int = 0,
    val detail: Int = 0,
    val detailLines: Int = 1,
    val header: Float = 0f,
    val strip: Float = 0f,
    val columns: Int = 0,
    val shown: Int = 0,
    val more: Int = 0,
)

/** The three forms of «At a glance» (PLANNING.md §7). */
enum class GlanceForm { CELL, ROW, GRID }

/**
 * What «At a glance» is asked to hold.
 *
 * @property caps how many caps the card shows (the reader's choice, in their order).
 * @property cellFigureEm the first cap's figure ("€12", "€30") on one line, in ems of the bold face:
 *   a one-cell card's hero.
 */
data class GlanceSpec(
    val width: Float,
    val height: Float,
    val fontScale: Float,
    val caps: Int,
    val cellFigureEm: Float = 0f,
)

/**
 * The card's plan. One cell: the figure's size, and whether the icon, the word ("left", "over")
 * and the bar are drawn. A row: [columns] slots, of which [shown] are caps and the last says
 * [more] when they do not all fit, whether a slot draws its cap's name ([names]). A grid: [rows]
 * by [columns], [shown] caps and [more] counted, each item [inline] (icon, name and figure on one
 * line) or stacked (the name over the figure).
 */
data class GlancePlan(
    val form: GlanceForm,
    val snug: Boolean,
    val shown: Int,
    val more: Int,
    val columns: Int = 1,
    val rows: Int = 1,
    val names: Boolean = true,
    val inline: Boolean = false,
    val cellFigureSp: Float = 0f,
    val cellIcon: Boolean = false,
    val cellWord: Boolean = false,
    val cellBar: Boolean = false,
)

object GlanceFit {
    // --- The family's card ------------------------------------------------------------------------

    /** The words' inset, the snug inset of a one-row card, and a one-cell card's. */
    const val CARD_PADDING = 14f
    const val CARD_PADDING_SNUG = 6f
    const val CELL_PADDING = 8f

    /** Two rows, on every grid measured; and under this a card is one cell wide. */
    const val TALL_MIN_HEIGHT = 150f
    const val CELL_MAX_WIDTH = 120f

    // --- The family's circle ----------------------------------------------------------------------

    /*
     * Every card of the family that draws a circle draws it at the size these three give for the
     * card's size, whatever the day's words say, so two cards of one size side by side on the home
     * screen show one circle, and a card's own does not grow when its words are short. It grows
     * with the card, and with the reader's text size it yields where the words need the room.
     */

    /** One cell wide: the cell's shorter side, less [CELL_CIRCLE_INSET] on each side. */
    fun cellCircle(width: Float, height: Float): Float =
        (minOf(width, height) - CELL_CIRCLE_INSET * 2).coerceIn(CELL_CIRCLE_MIN, CELL_CIRCLE_MAX)

    /**
     * One row: the height inside the snug insets, but never so wide that the words beside it keep
     * less than [ROW_CIRCLE_WORDS]; between [ROW_CIRCLE_MIN] and [ROW_CIRCLE_MAX].
     */
    fun rowCircle(width: Float, height: Float): Float = minOf(
        height - CARD_PADDING_SNUG * 2,
        width - CARD_PADDING * 2 - CIRCLE_GAP - ROW_CIRCLE_WORDS,
    ).coerceIn(ROW_CIRCLE_MIN, ROW_CIRCLE_MAX)

    /**
     * Two rows and up: the height inside the card left over a budgeted band of words, never
     * measured ([TALL_WORDS_BAND] under [WIDE_BAND_MIN_WIDTH], [WIDE_WORDS_BAND] from it;
     * [BARE_WORDS_BAND] where the reader hides the [sentence]), at [LINE_BOX_EM] a line and the
     * reader's text size; between [TALL_CIRCLE_MIN] and the ceiling ([tallCeiling]) or the card's
     * inner width, rounded down to a quarter.
     */
    fun tallCircle(width: Float, height: Float, fontScale: Float, sentence: Boolean = true): Float {
        val innerHeight = height - CARD_PADDING * 2
        val innerWidth = width - CARD_PADDING * 2
        val band = when {
            !sentence -> BARE_WORDS_BAND
            width < WIDE_BAND_MIN_WIDTH -> TALL_WORDS_BAND
            else -> WIDE_WORDS_BAND
        }
        val most = minOf(tallCeiling(innerHeight, innerWidth), innerWidth)
        return quarterPoint((innerHeight - band * LINE_BOX_EM * fontScale).coerceIn(TALL_CIRCLE_MIN, most))
    }

    /**
     * The most a tall circle grows to: [TALL_CIRCLE_SHARE] of the card's inner height and of its
     * inner width, so a large card's circle grows with it and its words keep their room; never
     * less than [TALL_CIRCLE_CEILING].
     */
    fun tallCeiling(innerHeight: Float, innerWidth: Float): Float =
        maxOf(TALL_CIRCLE_CEILING, TALL_CIRCLE_SHARE * minOf(innerHeight, innerWidth))

    const val CELL_CIRCLE_MIN = 36f
    const val CELL_CIRCLE_MAX = 104f
    const val CELL_CIRCLE_INSET = 8f
    const val ROW_CIRCLE_MIN = 44f
    const val ROW_CIRCLE_MAX = 56f

    /** The air between a circle and the words beside it. */
    const val CIRCLE_GAP = 12f

    /** The width the row's circle leaves beside it on every card of the family. */
    const val ROW_CIRCLE_WORDS = 84f
    const val TALL_CIRCLE_MIN = ROW_CIRCLE_MIN
    const val TALL_CIRCLE_CEILING = 104f
    const val TALL_CIRCLE_SHARE = 0.45f

    /**
     * The words' band under a tall circle, in sp of [LINE_BOX_EM] lines: a figure's line and three
     * lines of text under three cells wide (108.24 dp at the default text size), a figure's line
     * and two from three cells (87.12 dp), where a sentence that takes two lines on two cells takes
     * one; a figure's line and one (66 dp) where the reader hides the sentence.
     */
    const val TALL_WORDS_BAND = WidgetType.HERO + 3 * WidgetType.BODY
    const val WIDE_WORDS_BAND = WidgetType.HERO + 2 * WidgetType.BODY
    const val BARE_WORDS_BAND = WidgetType.HERO + WidgetType.BODY
    const val WIDE_BAND_MIN_WIDTH = 230f

    // --- «In words» -------------------------------------------------------------------------------

    fun wordsForm(width: Float, height: Float): WordsForm = when {
        width < CELL_MAX_WIDTH -> WordsForm.CELL
        height < TALL_MIN_HEIGHT -> WordsForm.ROW
        else -> WordsForm.TALL
    }

    fun wordsPlan(spec: WordsSpec): WordsPlan = when (wordsForm(spec.width, spec.height)) {
        WordsForm.CELL -> wordsCell(spec)
        WordsForm.ROW -> wordsRow(spec)
        WordsForm.TALL, WordsForm.PANEL -> wordsPanel(spec) ?: wordsTall(spec)
    }

    /**
     * One cell: the cell's circle, centred, and inside it the fullest figure that fits the hole
     * ([insideSp]), as every ring card of the family draws one cell (owner, 14 Nov 2026: it was the
     * cap's icon inside and the figure under the ring, where the height held it). Where no figure
     * fits the hole, the icon stays.
     */
    private fun wordsCell(spec: WordsSpec): WordsPlan {
        val snug = spec.height < TALL_MIN_HEIGHT
        val circle = cellCircle(spec.width, spec.height)
        val figure = spec.cellFigureEms.indexOfFirst { insideSp(circle, it, spec.fontScale) != null }
        return WordsPlan(
            form = WordsForm.CELL,
            circle = circle,
            snug = snug,
            sidePadding = CELL_PADDING,
            cellFigure = figure,
            cellFigureSp = if (figure >= 0) insideSp(circle, spec.cellFigureEms[figure], spec.fontScale) ?: 0f else 0f,
        )
    }

    /**
     * A figure set inside a ring of side [circle]: as large as fits [INSIDE_FILL] of its hole, never
     * over [CELL_INSIDE_MAX_SP]; null under [INSIDE_MIN_SP], which no one reads.
     */
    fun insideSp(circle: Float, em: Float, fontScale: Float): Float? {
        val hole = circle * (1f - RING_STROKE_SHARE * 2)
        val sp = quarterPoint(minOf(CELL_INSIDE_MAX_SP, hole * INSIDE_FILL / (em * fontScale.coerceAtLeast(0.1f))))
        return sp.takeIf { it >= INSIDE_MIN_SP }
    }

    /** The ring's stroke, as a share of its side (the painter's): the hole is what it leaves. */
    const val RING_STROKE_SHARE = 0.11f
    const val INSIDE_FILL = 0.72f
    const val INSIDE_MIN_SP = 10f
    const val CELL_INSIDE_MAX_SP = 28f

    /**
     * One row: the row's circle at the leading edge, where the words beside it keep
     * [ROW_WORDS_MIN]; what is left over the cap's name and its last day (or the day alone, or the
     * name alone, the fullest that stays whole), a size smaller on a narrow row; the pace at the far
     * edge where those words stay whole beside it, over two lines when one would be wider than
     * [PACE_ONE_LINE]. The words step down before they are ever cut.
     */
    private fun wordsRow(spec: WordsSpec, circleOf: Float? = null): WordsPlan {
        val scale = spec.fontScale
        val inner = spec.height - CARD_PADDING_SNUG * 2
        val width = spec.width - CARD_PADDING * 2
        val circle = circleOf ?: if (width - CIRCLE_GAP - ROW_CIRCLE_MIN >=
            ROW_WORDS_MIN
        ) {
            rowCircle(spec.width, spec.height)
        } else {
            0f
        }
        val words = width - (if (circle > 0f) circle + CIRCLE_GAP else 0f)
        val narrow = words < ROW_WORDS_COMFORT
        val figureSizes = if (narrow) NARROW_FIGURE_SIZES else FIGURE_SIZES
        val lineSizes = if (narrow) NARROW_LINE_SIZES else LINE_SIZES
        var figureSp = figureSizes.firstOrNull { widthOf(spec.rowFigureEm, it, scale) <= words }
            ?: quarterPoint(spThatFits(words, spec.rowFigureEm, scale))
        // The fullest second line that fits whole at one of the sizes; else the last, at the smallest.
        val fitting = spec.rowLineEms.indices.mapNotNull { index ->
            lineSizes.firstOrNull { widthOf(spec.rowLineEms[index], it, scale) <= words }?.let { index to it }
        }
        fun lineHeightsFit(fig: Float, sp: Float) = lineHeight(fig, scale) + lineHeight(sp, scale) <= inner

        // Both lines in the row's height, the second line yielding first: what is left is the card.
        fun sized(start: Float): Pair<Float, Float> {
            var sp = start
            var fig = figureSp
            while (!lineHeightsFit(fig, sp) && sp > ROW_LINE_MIN_SP) sp -= 1f
            if (!lineHeightsFit(fig, sp)) {
                fig =
                    minOf(fig, quarterPoint(sizeForLine(inner - lineHeight(sp, scale), scale)))
            }
            return fig to sp
        }

        // The pace only beside words that stay whole: a day cut off for it would say less.
        data class PacePick(val index: Int, val lines: Int, val width: Float)

        fun paceBeside(need: Float): PacePick? {
            val paceRoom = words - need - PACE_GAP
            if (paceRoom < PACE_COLUMN_MIN) return null
            spec.paceEms.indices.forEach { i ->
                val one = widthOf(spec.paceEms[i], PACE_SP, scale)
                if (one <= minOf(paceRoom, PACE_ONE_LINE) && lineHeight(PACE_SP, scale) <= inner) {
                    return PacePick(i, 1, one)
                }
                val two = widthOf(spec.paceTwoLineEms[i], PACE_SP, scale)
                if (two <= paceRoom && linesHeight(PACE_SP, 2, scale) <= inner) return PacePick(i, 2, two)
            }
            return null
        }

        // The fullest line that leaves the pace its column, where the cap has a pace (since the line
        // opens with its word, 14 Nov 2026, the fullest one can take the whole row); else the
        // fullest line alone.
        val choices = fitting.ifEmpty { listOf(spec.rowLineEms.lastIndex.coerceAtLeast(0) to lineSizes.last()) }
        val picked = choices.firstNotNullOfOrNull { (index, sp) ->
            val (fig, lsp) = sized(sp)
            val need =
                maxOf(
                    widthOf(spec.rowFigureEm, fig, scale),
                    widthOf(
                        spec.rowLineEms.getOrElse(index) {
                            0f
                        },
                        lsp,
                        scale,
                    ),
                )
            paceBeside(need)?.let { Triple(index, fig to lsp, it) }
        }
        val line = picked?.first ?: choices.first().first
        val (sizedFigure, lineSp) = picked?.second ?: sized(choices.first().second)
        figureSp = sizedFigure
        val pace = picked?.third?.index ?: -1
        val paceLines = picked?.third?.lines ?: 0
        val paceWidth = picked?.third?.width ?: 0f
        return WordsPlan(
            form = WordsForm.ROW,
            circle = circle,
            snug = true,
            sidePadding = CARD_PADDING,
            figureSp = figureSp,
            lineSp = lineSp,
            line = line,
            pace = pace,
            paceLines = paceLines,
            paceWidth = paceWidth,
        )
    }

    /**
     * Two rows and up: the tall circle ([tallCircle]) in the top trailing corner, the cap's name
     * beside it where [NAME_BESIDE_MIN] are left ([tallNameBeside]), else in the last words with the
     * cap's last day ("Coffee · until Monday"), so the circle never yields to a name; from the
     * bottom leading corner, the hero (what is left, at [WidgetType.HERO], smaller only where the
     * width cannot hold it), the cap's sentence whole or not at all (up to [SENTENCE_MAX_LINES]
     * under [WIDE_BAND_MIN_WIDTH], one from it), and the last words, in the fullest form the card
     * has the lines for. The words are counted as the band counts them ([bandLine]), so at the
     * default text size the family's circle stands with the band it budgets exactly; the circle
     * yields only where the hero and one line would not fit under it (a large text size), never
     * under [TALL_CIRCLE_MIN]. The last words come before the sentence. What is left is air.
     */
    private fun wordsTall(spec: WordsSpec): WordsPlan {
        val scale = spec.fontScale
        val inner = spec.height - CARD_PADDING * 2
        val heroSp = quarterPoint(minOf(WidgetType.HERO, spThatFits(spec.width - CARD_PADDING * 2, spec.heroEm, scale)))
        val most = tallCircle(spec.width, spec.height, scale, spec.sentence)
        val heroLine = bandLine(heroSp, scale)
        val bodyLine = bandLine(WidgetType.BODY, scale)
        // The circle keeps the hero and one line of last words under it; else it yields, never
        // under its floor; and where even that cannot be, the last words go before the circle does.
        val withDetail = inner - heroLine - bodyLine >= TALL_CIRCLE_MIN
        val room = inner - heroLine
        val circle = when {
            // The family's circle where the words leave it, to a hair: the band is counted in
            // floats, and a rounding a millionth under it must not cost the circle a quarter.
            withDetail -> if (room - bodyLine >= most - ROUNDING) most else quarterPoint(room - bodyLine)

            room >= TALL_CIRCLE_MIN -> TALL_CIRCLE_MIN

            else -> 0f
        }
        val nameBeside = circle > 0f && tallNameBeside(spec)
        // A name beside the circle steps down before a word of it would break; its lines, what the
        // circle's height holds.
        val beside = spec.width - CARD_PADDING * 2 - circle - CIRCLE_GAP
        val nameSp = NAME_SIZES.firstOrNull { widthOf(spec.nameWordEm, it, scale) <= beside } ?: NAME_SIZES.last()
        val nameLines = if (nameBeside) {
            floor(circle / lineHeight(nameSp, scale)).toInt().coerceIn(1, NAME_MAX_LINES)
        } else {
            0
        }
        var left = room - circle
        // The last words: the fullest form whose lines the card has, up to [DETAIL_MAX_LINES]; the
        // day alone, at the least, cut to one line only where nothing else can stand.
        val allowed = if (withDetail) floor(left / bodyLine).toInt().coerceIn(1, DETAIL_MAX_LINES) else 0
        val detail = if (allowed > 0) {
            spec.detailLines.indexOfFirst { it <= allowed }.takeIf { it >= 0 } ?: spec.detailLines.lastIndex
        } else {
            -1
        }
        val detailLines = if (detail >= 0) minOf(spec.detailLines[detail], allowed) else 0
        left -= bodyLine * detailLines
        // The sentence whole or not at all: a sentence cut off mid-phrase says less than none.
        val maxLines = if (spec.width < WIDE_BAND_MIN_WIDTH) SENTENCE_MAX_LINES else 1
        val sentence = if (spec.sentence) {
            spec.sentenceLines.indexOfFirst { lines ->
                lines in 1..maxLines &&
                    bandLine(SENTENCE_SP, scale) * lines <= left
            }
        } else {
            -1
        }
        return WordsPlan(
            form = WordsForm.TALL,
            circle = circle,
            snug = false,
            sidePadding = CARD_PADDING,
            heroSp = heroSp,
            nameBeside = nameBeside,
            nameSp = nameSp,
            nameLines = nameLines,
            sentence = sentence,
            sentenceLines = if (sentence >= 0) spec.sentenceLines[sentence] else 0,
            detail = detail,
            detailLines = detailLines,
        )
    }

    /**
     * Two rows high and from [WIDE_BAND_MIN_WIDTH] wide, with something to stand under the cap
     * (owner, 14 Nov 2026): the one-row card on top at the height its tallest column needs (the
     * tall card's circle, [tallCircle], or the hero over its line), and under it, after
     * [PANEL_GAP], the strip: the cap's period ([WordsSpec.stripDays] boxes over their labels), or
     * the other caps side by side, as many as [panelColumns] holds, the last counting the rest. None
     * where the strip would be shorter than it needs: the card keeps its tall form.
     */
    private fun wordsPanel(spec: WordsSpec): WordsPlan? {
        if (spec.width < WIDE_BAND_MIN_WIDTH || spec.height < TALL_MIN_HEIGHT) return null
        if (spec.stripDays <= 0 && spec.belowCaps <= 0) return null
        val scale = spec.fontScale
        val innerHeight = spec.height - CARD_PADDING * 2
        val circle = tallCircle(spec.width, spec.height, scale, spec.sentence)
        val words = bandLine(WidgetType.HERO, scale) + bandLine(ROW_LINE_SP, scale)
        val header = maxOf(circle, words)
        val strip = innerHeight - header - PANEL_GAP
        val need = if (spec.belowCaps > 0) slotHeight(scale) else stripDaysMin(scale)
        if (strip < need) return null
        val row = wordsRow(spec.copy(height = header + CARD_PADDING_SNUG * 2), circleOf = circle)
        val columns = panelColumns(spec.width)
        val shown = if (spec.belowCaps > columns) columns - 1 else spec.belowCaps
        return row.copy(
            form = WordsForm.PANEL,
            snug = false,
            header = header,
            strip = strip,
            columns = if (spec.belowCaps > 0) columns else 0,
            shown = shown.coerceAtLeast(0),
            more = (spec.belowCaps - shown).coerceAtLeast(0),
        )
    }

    /**
     * How many caps a wide card holds side by side under its own: each at least [PANEL_SLOT_MIN],
     * never more than [GROUP_MAX] (a container's children, with the gaps between them).
     */
    fun panelColumns(width: Float): Int =
        ((width - CARD_PADDING * 2 + SLOT_GAP) / (PANEL_SLOT_MIN + SLOT_GAP)).toInt().coerceIn(1, GROUP_MAX)

    /** A cap under the panel's own, as «At a glance»'s row draws it: figure, name, the bar. */
    fun slotHeight(fontScale: Float): Float = lineHeight(SLOT_FIGURE_SP, fontScale) +
        lineHeight(SLOT_NAME_SP, fontScale) + BAR_GAP + BAR_HEIGHT + MARK_OVERHANG * 2

    /** The period's boxes at their floor, over a line of labels. */
    fun stripDaysMin(fontScale: Float): Float = STRIP_BOXES_MIN + stripLabelHeight(fontScale)

    /** The labels under the period's boxes, and the air over them. */
    fun stripLabelHeight(fontScale: Float): Float = lineHeight(STRIP_LABEL_SP, fontScale) + STRIP_LABEL_GAP

    const val PANEL_GAP = 12f
    const val PANEL_SLOT_MIN = 64f
    const val STRIP_BOXES_MIN = 28f

    /**
     * The period's boxes stand no taller than this: past it a week reads as seven slabs, and the
     * air goes between the header and the strip instead, as under a ring card's bars.
     */
    const val STRIP_BOXES_MAX = 32f
    const val STRIP_LABEL_SP = WidgetType.CAPTION
    const val STRIP_LABEL_GAP = 2f
    const val STRIP_GAP = 3f

    /**
     * Whether a tall card's name stands beside its circle: where the circle the card's size gives
     * leaves [NAME_BESIDE_MIN] beside it. The caller asks it before measuring the last words, which
     * carry the name where it is not beside the circle.
     */
    fun tallNameBeside(spec: WordsSpec): Boolean = spec.width - CARD_PADDING * 2 - tallCircle(
        spec.width,
        spec.height,
        spec.fontScale,
        spec.sentence,
    ) - CIRCLE_GAP >=
        NAME_BESIDE_MIN

    /**
     * A line as the family's band counts it under a tall circle: [LINE_BOX_EM] at the reader's text
     * size, nothing more. A `TextView`'s second line takes less than its first (no font padding
     * between two lines), which is the slack a launcher's rounding takes.
     */
    fun bandLine(sizeSp: Float, fontScale: Float): Float = sizeSp * LINE_BOX_EM * fontScale

    /** A one-cell card's figure under the ring, and the air over it. */
    const val CELL_FIGURE_SP = WidgetType.LABEL
    const val CELL_GAP = 4f

    /**
     * The row's figure: the family's hero, Bold, the amount alone (owner, 14 Nov 2026: it was
     * "€12 left" at 20 sp Medium; the word went to the line under it), stepping down the scale on a
     * row too narrow for it, and past the last step to the size it fits.
     */
    const val ROW_FIGURE_SP = WidgetType.HERO
    private val FIGURE_SIZES = listOf(ROW_FIGURE_SP, WidgetType.DISPLAY, 26f, WidgetType.TITLE)
    private val NARROW_FIGURE_SIZES = listOf(28f, 24f, WidgetType.TITLE, 20f)

    /** The row's second line: the scale's body at ease, 14 to 12 on a narrow row. */
    const val ROW_LINE_SP = WidgetType.BODY
    private val LINE_SIZES = listOf(ROW_LINE_SP, 14f)
    private val NARROW_LINE_SIZES = listOf(14f, WidgetType.LABEL, 12f)
    private const val ROW_LINE_MIN_SP = 11f

    /** The narrowest the words beside a row's circle may be, and where they are set a size smaller. */
    const val ROW_WORDS_MIN = 72f
    const val ROW_WORDS_COMFORT = 150f

    /** The pace's column on a row: its size, the air before it, its floor. */
    const val PACE_SP = WidgetType.BODY
    const val PACE_GAP = 12f
    const val PACE_COLUMN_MIN = 56f
    const val PACE_ONE_LINE = 100f

    /** The tall card's lines: the cap's sentence, its last words, the name beside the circle. */
    const val SENTENCE_SP = WidgetType.BODY
    const val SENTENCE_MAX_LINES = 2
    const val DETAIL_MAX_LINES = 2
    private const val ROUNDING = 0.01f
    const val NAME_BESIDE_MIN = 56f
    const val NAME_MAX_LINES = 3
    private val NAME_SIZES = listOf(WidgetType.BODY, 14f, WidgetType.LABEL)

    // --- «At a glance» ----------------------------------------------------------------------------

    fun glanceForm(width: Float, height: Float): GlanceForm = when {
        width < CELL_MAX_WIDTH -> GlanceForm.CELL
        height < TALL_MIN_HEIGHT -> GlanceForm.ROW
        else -> GlanceForm.GRID
    }

    fun glancePlan(spec: GlanceSpec): GlancePlan = when (glanceForm(spec.width, spec.height)) {
        GlanceForm.CELL -> glanceCell(spec)
        GlanceForm.ROW -> glanceRow(spec)
        GlanceForm.GRID -> glanceGrid(spec)
    }

    /**
     * One cell, one cap, its figure the hero: as large as the cell's width holds, never over
     * [CELL_HERO_MAX]; then, where the height leaves them, the word ("left", "over"), the icon on
     * top, and the bar at the bottom, in that order. "Over" is never left to the bar's colour:
     * without room for the word, the figure steps down until it has it.
     */
    private fun glanceCell(spec: GlanceSpec): GlancePlan {
        val scale = spec.fontScale
        val snug = spec.height < TALL_MIN_HEIGHT
        val inner = spec.height - (if (snug) CARD_PADDING_SNUG else CARD_PADDING) * 2
        val width = spec.width - CELL_PADDING * 2
        val word = lineHeight(CELL_WORD_SP, scale)
        var figure = quarterPoint(minOf(CELL_HERO_MAX, spThatFits(width, spec.cellFigureEm, scale)))
        if (lineHeight(figure, scale) + word > inner) figure = quarterPoint(sizeForLine(inner - word, scale))
        var left = inner - lineHeight(figure, scale) - word
        val icon = left >= CELL_ICON + CELL_ICON_GAP
        if (icon) left -= CELL_ICON + CELL_ICON_GAP
        val bar = left >= BAR_HEIGHT + BAR_GAP
        return GlancePlan(
            form = GlanceForm.CELL,
            snug = snug,
            shown = minOf(1, spec.caps),
            more = 0,
            cellFigureSp = figure,
            cellIcon = icon,
            cellWord = true,
            cellBar = bar,
        )
    }

    /**
     * One row: the caps side by side, each slot at least [SLOT_MIN] wide, at most [GROUP_MAX] (a
     * Glance container drops its eleventh child silently, and a slot is two); when they do not all
     * fit, the last slot counts the rest. Each slot its icon and figure, its name where the height
     * holds it, and its bar.
     */
    private fun glanceRow(spec: GlanceSpec): GlancePlan {
        val scale = spec.fontScale
        val inner = spec.height - CARD_PADDING_SNUG * 2
        val width = spec.width - CARD_PADDING * 2
        val slots = floor((width + SLOT_GAP) / (SLOT_MIN + SLOT_GAP)).toInt().coerceIn(1, GROUP_MAX)
        val columns = minOf(slots, spec.caps.coerceAtLeast(1))
        val shown = if (spec.caps > slots) slots - 1 else spec.caps
        val base = lineHeight(SLOT_FIGURE_SP, scale) + BAR_GAP + BAR_HEIGHT
        val names = base + lineHeight(SLOT_NAME_SP, scale) <= inner
        return GlancePlan(
            form = GlanceForm.ROW,
            snug = true,
            shown = shown,
            more = spec.caps - shown,
            columns = columns,
            rows = 1,
            names = names,
        )
    }

    /**
     * Two rows and up: the caps in rows, one column under [TWO_COLUMNS_MIN] of inner width, two
     * from it, three from [THREE_COLUMNS_MIN]; each item its icon, name, figure and bar, on one
     * line where a column is [INLINE_MIN] wide, else the name over the figure. As many rows as the
     * height holds, [GROUP_MAX] at most; when the caps do not all fit, the last item counts the rest.
     */
    private fun glanceGrid(spec: GlanceSpec): GlancePlan {
        val scale = spec.fontScale
        val inner = spec.height - CARD_PADDING * 2
        val width = spec.width - CARD_PADDING * 2
        val columns = when {
            width >= THREE_COLUMNS_MIN -> 3
            width >= TWO_COLUMNS_MIN -> 2
            else -> 1
        }.coerceAtMost(spec.caps.coerceAtLeast(1))
        val column = (width - GRID_COLUMN_GAP * (columns - 1)) / columns
        val inline = column >= INLINE_MIN
        val item = itemHeight(inline, scale)
        val rows = floor((inner + GRID_ROW_GAP) / (item + GRID_ROW_GAP)).toInt().coerceIn(1, GROUP_MAX)
        val cells = rows * columns
        val shown = if (spec.caps > cells) cells - 1 else spec.caps
        val used = minOf(rows, (spec.caps + columns - 1) / columns).coerceAtLeast(1)
        return GlancePlan(
            form = GlanceForm.GRID,
            snug = false,
            shown = shown,
            more = spec.caps - shown,
            columns = columns,
            rows = used,
            names = true,
            inline = inline,
        )
    }

    /**
     * A slot's or an item's figure beside its icon: the [preferred] size where the figure fits
     * [width] beside the icon, else smaller down to [SLOT_FIGURE_MIN_SP]; past that, without the
     * icon, as large as fits. The size, and whether the icon is drawn.
     */
    fun figureBesideIcon(
        width: Float,
        em: Float,
        fontScale: Float,
        preferred: Float,
        icon: Float,
    ): Pair<Float, Boolean> {
        val beside = width - icon - ICON_GAP
        val sp = minOf(preferred, quarterPoint(spThatFits(beside, em, fontScale)))
        if (sp >= SLOT_FIGURE_MIN_SP) return sp to true
        return minOf(preferred, quarterPoint(spThatFits(width, em, fontScale))) to false
    }

    const val ICON_GAP = 6f
    const val SLOT_FIGURE_MIN_SP = 12f

    /**
     * The size of a grid item's name on the line it shares with its figure: the label's, or, where
     * the figure's words leave the name less than its width ("sotto di 6 €" beside "Svago" cut it
     * to "Sva…"), the scale's next step down, the caption's. A name a little smaller is still a
     * name; one that fits at neither size is cut at the smaller, where more of it shows.
     *
     * @param column the item's width; [nameEm] and [figureEm], the widths of its name and of its
     *   figure's words, in ems.
     */
    fun itemNameSp(column: Float, nameEm: Float, figureEm: Float, fontScale: Float): Float {
        val beside = column - ITEM_ICON - ICON_GAP - ITEM_FIGURE_GAP - widthOf(figureEm, ITEM_FIGURE_SP, fontScale)
        return if (widthOf(nameEm, ITEM_NAME_SP, fontScale) <= beside) ITEM_NAME_SP else ITEM_NAME_SMALL_SP
    }

    /** One grid item's height: its line or lines of words, the bar and the air between. */
    fun itemHeight(inline: Boolean, fontScale: Float): Float = if (inline) {
        maxOf(lineHeight(ITEM_FIGURE_SP, fontScale), ITEM_ICON) + BAR_GAP + BAR_HEIGHT
    } else {
        maxOf(lineHeight(ITEM_NAME_SP, fontScale), ITEM_ICON) + lineHeight(ITEM_FIGURE_SP, fontScale) +
            BAR_GAP + BAR_HEIGHT
    }

    /** A Glance container's children: at most ten, and a slot or an item never needs more than two. */
    const val GROUP_MAX = 5

    /** The bar under a figure: slim, with air over it; the mark for today overhangs it. */
    const val BAR_HEIGHT = 6f
    const val BAR_GAP = 6f
    const val MARK_OVERHANG = 2f

    /** One cell's hero, its word, its icon. */
    const val CELL_HERO_MAX = 28f
    const val CELL_WORD_SP = WidgetType.LABEL
    const val CELL_ICON = 20f
    const val CELL_ICON_GAP = 4f

    /** A row's slots: two on two cells, three on three, four on four. */
    const val SLOT_MIN = 60f
    const val SLOT_GAP = 10f
    const val SLOT_ICON = 18f
    const val SLOT_FIGURE_SP = WidgetType.BODY
    const val SLOT_NAME_SP = WidgetType.LABEL

    /** The grid's columns and items. */
    const val TWO_COLUMNS_MIN = 260f
    const val THREE_COLUMNS_MIN = 440f
    const val INLINE_MIN = 120f
    const val GRID_COLUMN_GAP = 16f
    const val GRID_ROW_GAP = 10f
    const val ITEM_ICON = 18f
    const val ITEM_FIGURE_SP = WidgetType.BODY
    const val ITEM_NAME_SP = WidgetType.LABEL

    /** A name's size where its figure's words leave it too little room at the label's. */
    const val ITEM_NAME_SMALL_SP = WidgetType.CAPTION

    /** The air between an item's name and the figure on its line. */
    const val ITEM_FIGURE_GAP = 8f
}
