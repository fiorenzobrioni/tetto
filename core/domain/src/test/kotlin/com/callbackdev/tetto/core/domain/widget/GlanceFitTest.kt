package com.callbackdev.tetto.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The widgets' arithmetic at the family's reference grants (PLANNING.md §7): one cell 85 dp; rows
 * 85, 189 and 293 dp tall; two, three and four cells 159, 250 and 340 dp wide; and the owner's
 * two-by-two, 188 × 218 dp.
 */
class GlanceFitTest {
    // --- The family's circle: the numbers both sisters' tests pin ----------------------------------

    @Test
    fun `the circle at every reference grant, with the sentence`() {
        val circles = mapOf(
            "1x1" to circle(85f, 85f),
            "1x2" to circle(85f, 189f),
            "1x3" to circle(85f, 293f),
            "2x1" to circle(159f, 85f),
            "3x1" to circle(250f, 85f),
            "4x1" to circle(340f, 85f),
            "2x2" to circle(159f, 189f),
            "3x2" to circle(250f, 189f),
            "4x2" to circle(340f, 189f),
            "2x3" to circle(159f, 293f),
            "3x3" to circle(250f, 293f),
            "4x3" to circle(340f, 293f),
        )
        assertThat(circles).containsExactly(
            "1x1", 69f, "1x2", 69f, "1x3", 69f,
            "2x1", 44f, "3x1", 56f, "4x1", 56f,
            "2x2", 52.75f, "3x2", 73.75f, "4x2", 73.75f,
            "2x3", 104f, "3x3", 104f, "4x3", 119.25f,
        )
    }

    @Test
    fun `the circle at every reference grant, without the sentence`() {
        assertThat(circle(85f, 85f, sentence = false)).isEqualTo(69f)
        assertThat(circle(159f, 85f, sentence = false)).isEqualTo(44f)
        assertThat(circle(340f, 85f, sentence = false)).isEqualTo(56f)
        assertThat(circle(159f, 189f, sentence = false)).isEqualTo(95f)
        assertThat(circle(250f, 189f, sentence = false)).isEqualTo(95f)
        assertThat(circle(340f, 189f, sentence = false)).isEqualTo(95f)
        assertThat(circle(159f, 293f, sentence = false)).isEqualTo(104f)
        assertThat(circle(250f, 293f, sentence = false)).isEqualTo(104f)
        assertThat(circle(340f, 293f, sentence = false)).isEqualTo(119.25f)
    }

    @Test
    fun `the owner's two-by-two`() {
        assertThat(circle(188f, 218f)).isEqualTo(81.75f)
        assertThat(circle(188f, 218f, sentence = false)).isEqualTo(104f)
    }

    @Test
    fun `at a larger text size the two-by-two's band grows and its circle stands at the floor`() {
        assertThat(GlanceFit.tallCircle(159f, 189f, 1.15f)).isEqualTo(44f)
        assertThat(GlanceFit.tallCircle(159f, 189f, 1.3f)).isEqualTo(44f)
    }

    @Test
    fun `the circle is the card's, whatever the words say`() {
        val short = GlanceFit.wordsPlan(words(250f, 189f, sentenceLines = listOf(1)))
        val none = GlanceFit.wordsPlan(words(250f, 189f, sentenceLines = emptyList()))
        assertThat(short.circle).isEqualTo(73.75f)
        assertThat(none.circle).isEqualTo(73.75f)
    }

    @Test
    fun `the band is the family's`() {
        assertThat(GlanceFit.TALL_WORDS_BAND * LINE_BOX_EM).isWithin(0.001f).of(108.24f)
        assertThat(GlanceFit.WIDE_WORDS_BAND * LINE_BOX_EM).isWithin(0.001f).of(87.12f)
        assertThat(GlanceFit.BARE_WORDS_BAND * LINE_BOX_EM).isWithin(0.001f).of(66f)
    }

    // --- «At a glance» ---------------------------------------------------------------------------------

    @Test
    fun `the forms by size`() {
        assertThat(GlanceFit.wordsForm(85f, 85f)).isEqualTo(WordsForm.CELL)
        assertThat(GlanceFit.wordsForm(85f, 293f)).isEqualTo(WordsForm.CELL)
        assertThat(GlanceFit.wordsForm(159f, 85f)).isEqualTo(WordsForm.ROW)
        assertThat(GlanceFit.wordsForm(340f, 85f)).isEqualTo(WordsForm.ROW)
        assertThat(GlanceFit.wordsForm(159f, 189f)).isEqualTo(WordsForm.TALL)
    }

    @Test
    fun `one cell says the figure under the ring where it fits, and never shrinks the ring for it`() {
        val square = GlanceFit.wordsPlan(words(85f, 85f))
        assertThat(square.circle).isEqualTo(69f)
        assertThat(square.cellFigure).isEqualTo(-1)
        val tall = GlanceFit.wordsPlan(words(85f, 189f))
        assertThat(tall.circle).isEqualTo(69f)
        assertThat(tall.cellFigure).isEqualTo(0)
    }

    @Test
    fun `one cell takes the shorter figure when the fuller is too wide`() {
        val plan = GlanceFit.wordsPlan(words(85f, 189f, cellFigureEms = listOf(9f, 2f)))
        assertThat(plan.cellFigure).isEqualTo(1)
    }

    @Test
    fun `a row's circle and its words at ease`() {
        val plan = GlanceFit.wordsPlan(words(340f, 85f))
        assertThat(plan.form).isEqualTo(WordsForm.ROW)
        assertThat(plan.circle).isEqualTo(56f)
        assertThat(plan.figureSp).isEqualTo(20f)
        assertThat(plan.lineSp).isEqualTo(16f)
        assertThat(plan.pace).isEqualTo(0)
    }

    @Test
    fun `a narrow row sets its words a size smaller and has no pace`() {
        val plan = GlanceFit.wordsPlan(words(159f, 85f))
        assertThat(plan.circle).isEqualTo(44f)
        assertThat(plan.figureSp).isEqualTo(16f)
        assertThat(plan.lineSp).isAtMost(14f)
        assertThat(plan.pace).isEqualTo(-1)
    }

    @Test
    fun `a row's second line is the fullest that stays whole`() {
        val wide = GlanceFit.wordsPlan(words(340f, 85f))
        assertThat(wide.line).isEqualTo(0)
        // Two cells: the day alone where it stays whole, else the name.
        assertThat(GlanceFit.wordsPlan(words(159f, 85f)).line).isEqualTo(1)
        val narrow = GlanceFit.wordsPlan(words(159f, 85f, rowLineEms = listOf(9f, 7f, 3f)))
        assertThat(narrow.line).isEqualTo(2)
        assertThat(widthOf(3f, narrow.lineSp, 1f)).isAtMost(159f - 28f - 44f - 12f)
    }

    @Test
    fun `the pace never cuts the words beside it`() {
        val plan = GlanceFit.wordsPlan(words(340f, 85f, rowLineEms = listOf(13f)))
        assertThat(plan.pace).isEqualTo(-1)
    }

    @Test
    fun `a name beside the circle steps down before a word of it breaks`() {
        val plan = GlanceFit.wordsPlan(words(159f, 189f, nameWordEm = 4.4f))
        assertThat(plan.nameBeside).isTrue()
        assertThat(plan.nameSp).isLessThan(WidgetType.BODY)
        assertThat(widthOf(4.4f, plan.nameSp, 1f)).isAtMost(159f - 28f - plan.circle - GlanceFit.CIRCLE_GAP)
    }

    @Test
    fun `a row's words fit its height at every text size`() {
        listOf(1f, 1.15f, 1.3f).forEach { scale ->
            listOf(159f, 250f, 340f).forEach { width ->
                val plan = GlanceFit.wordsPlan(words(width, 85f, scale = scale))
                val words = lineHeight(plan.figureSp, scale) + lineHeight(plan.lineSp, scale)
                assertThat(words).isAtMost(85f - GlanceFit.CARD_PADDING_SNUG * 2)
            }
        }
    }

    @Test
    fun `a tall card's name stands beside the circle where it has the room, else with its last day`() {
        assertThat(GlanceFit.wordsPlan(words(159f, 189f)).nameBeside).isTrue()
        assertThat(GlanceFit.wordsPlan(words(159f, 293f)).nameBeside).isFalse()
        assertThat(GlanceFit.wordsPlan(words(250f, 293f)).nameBeside).isTrue()
    }

    @Test
    fun `a tall card's sentence is whole or not at all`() {
        val two = GlanceFit.wordsPlan(words(159f, 189f, sentenceLines = listOf(2, 1)))
        assertThat(two.sentence).isEqualTo(0)
        assertThat(two.sentenceLines).isEqualTo(2)
        // From three cells wide, one line: the fuller sentence goes for the shorter.
        val wide = GlanceFit.wordsPlan(words(250f, 189f, sentenceLines = listOf(2, 1)))
        assertThat(wide.sentence).isEqualTo(1)
        assertThat(wide.sentenceLines).isEqualTo(1)
        val none = GlanceFit.wordsPlan(words(250f, 189f, sentenceLines = listOf(3)))
        assertThat(none.sentence).isEqualTo(-1)
    }

    @Test
    fun `a tall card's last words take the fullest form it has the lines for, before the sentence`() {
        // Two cells wide: one line of three words is too wide, the two parts take two lines.
        val narrow = GlanceFit.wordsPlan(
            words(159f, 189f, sentenceLines = listOf(1), detailLines = listOf(Int.MAX_VALUE, 2, 1)),
        )
        assertThat(narrow.detail).isEqualTo(1)
        assertThat(narrow.detailLines).isEqualTo(2)
        assertThat(narrow.sentence).isEqualTo(0)
        // Without the sentence the circle takes the band, and leaves one line: the day alone.
        val bare = GlanceFit.wordsPlan(
            words(159f, 189f, sentence = false, detailLines = listOf(Int.MAX_VALUE, 2, 1)),
        )
        assertThat(bare.circle).isEqualTo(95f)
        assertThat(bare.detail).isEqualTo(2)
        assertThat(bare.detailLines).isEqualTo(1)
        // Wide enough, one line.
        val wide = GlanceFit.wordsPlan(words(340f, 189f, detailLines = listOf(1, 2, 1)))
        assertThat(wide.detail).isEqualTo(0)
    }

    @Test
    fun `without the sentence switch no sentence is drawn and the circle grows`() {
        val plan = GlanceFit.wordsPlan(words(159f, 189f, sentence = false, sentenceLines = listOf(1)))
        assertThat(plan.sentence).isEqualTo(-1)
        assertThat(plan.circle).isEqualTo(95f)
    }

    @Test
    fun `a tall card's words and circle fit its height at every reference grant and text size`() {
        listOf(1f, 1.15f, 1.3f).forEach { scale ->
            listOf(159f, 188f, 250f, 340f).forEach { width ->
                listOf(189f, 218f, 293f).forEach { height ->
                    listOf(true, false).forEach { sentence ->
                        val plan = GlanceFit.wordsPlan(
                            words(width, height, scale = scale, sentence = sentence, sentenceLines = listOf(2, 1)),
                        )
                        val words = GlanceFit.bandLine(plan.heroSp, scale) +
                            GlanceFit.bandLine(WidgetType.BODY, scale) * plan.detailLines +
                            GlanceFit.bandLine(GlanceFit.SENTENCE_SP, scale) * plan.sentenceLines
                        assertThat(words + plan.circle).isAtMost(height - GlanceFit.CARD_PADDING * 2)
                        assertThat(plan.circle).isAtLeast(GlanceFit.TALL_CIRCLE_MIN)
                    }
                }
            }
        }
    }

    @Test
    fun `a tall card's hero steps down for a long figure, never cut`() {
        val plan = GlanceFit.wordsPlan(words(159f, 189f, heroEm = 7f))
        assertThat(plan.heroSp).isLessThan(WidgetType.HERO)
        assertThat(widthOf(7f, plan.heroSp, 1f)).isAtMost(159f - GlanceFit.CARD_PADDING * 2)
    }

    // --- «At a glance» with the other caps (owner, 14 Nov 2026) ----------------------------------------

    @Test
    fun `with the other caps the ring keeps the family's circle at every tall grant`() {
        listOf(159f to 189f, 188f to 218f, 250f to 189f, 340f to 189f, 159f to 293f, 250f to 293f, 340f to 293f)
            .forEach { (width, height) ->
                val plan = GlanceFit.wordsPlan(others(width, height))
                assertThat(plan.others).isNotNull()
                assertThat(plan.circle).isEqualTo(circle(width, height))
            }
    }

    @Test
    fun `without the sentence, the row takes the room it leaves, not the ring`() {
        val plan = GlanceFit.wordsPlan(others(340f, 189f, sentence = false))
        assertThat(plan.circle).isEqualTo(73.75f)
        assertThat(plan.sentence).isEqualTo(-1)
    }

    @Test
    fun `the words go under the ring where it leaves them the room, beside it where not`() {
        assertThat(GlanceFit.wordsPlan(others(340f, 189f)).layout).isEqualTo(WordsLayout.BESIDE)
        assertThat(GlanceFit.wordsPlan(others(250f, 189f)).layout).isEqualTo(WordsLayout.BESIDE)
        assertThat(GlanceFit.wordsPlan(others(159f, 189f)).layout).isEqualTo(WordsLayout.UNDER)
        assertThat(GlanceFit.wordsPlan(others(250f, 293f)).layout).isEqualTo(WordsLayout.UNDER)
        assertThat(GlanceFit.wordsPlan(others(340f, 293f)).layout).isEqualTo(WordsLayout.UNDER)
        // Beside the ring, the name is in the last words, and the hero is never under its floor.
        val beside = GlanceFit.wordsPlan(others(340f, 189f))
        assertThat(beside.nameBeside).isFalse()
        assertThat(beside.heroSp).isAtLeast(GlanceFit.BESIDE_HERO_MIN)
        assertThat(beside.detail).isAtLeast(0)
    }

    @Test
    fun `the row holds as many slots as the width, the last counting the rest`() {
        val two = row(others(159f, 189f, count = 4))
        assertThat(listOf(two.slots, two.shown, two.more)).containsExactly(2, 1, 3).inOrder()
        val four = row(others(340f, 189f, count = 4))
        assertThat(listOf(four.slots, four.shown, four.more)).containsExactly(4, 4, 0).inOrder()
        val few = row(others(340f, 189f, count = 1))
        assertThat(listOf(few.slots, few.shown, few.more)).containsExactly(4, 1, 0).inOrder()
        val many = row(others(900f, 189f, count = 40))
        assertThat(many.shown + if (many.more > 0) 1 else 0).isAtMost(GlanceFit.GROUP_MAX)
    }

    @Test
    fun `the card keeps its last day before the row keeps its names`() {
        // Two by two: under the ring, the hero and a line of the day only without the names' line.
        val plan = GlanceFit.wordsPlan(others(159f, 189f, detailLines = listOf(Int.MAX_VALUE, 2, 1)))
        assertThat(plan.detail).isEqualTo(2)
        assertThat(plan.others?.names).isFalse()
        // Four by two keeps both.
        val wide = GlanceFit.wordsPlan(others(340f, 189f))
        assertThat(wide.others?.names).isTrue()
        assertThat(wide.detail).isAtLeast(0)
    }

    @Test
    fun `a day that would be cut is not drawn, and does not count as one`() {
        val plan = GlanceFit.wordsPlan(others(159f, 189f, detailLines = listOf(Int.MAX_VALUE, Int.MAX_VALUE, 3)))
        assertThat(plan.detail).isEqualTo(-1)
        assertThat(plan.nameBeside).isTrue()
    }

    @Test
    fun `the words, the gap and the row fill the card's height and never more`() {
        listOf(1f, 1.15f, 1.3f).forEach { scale ->
            listOf(159f, 188f, 250f, 340f).forEach { width ->
                listOf(189f, 218f, 293f, 397f).forEach { height ->
                    listOf(true, false).forEach { sentence ->
                        val plan = GlanceFit.wordsPlan(others(width, height, scale = scale, sentence = sentence))
                        val row = plan.others ?: return@forEach
                        val inner = height - GlanceFit.CARD_PADDING * 2
                        assertThat(plan.zone + GlanceFit.OTHERS_GAP + row.height).isWithin(0.01f).of(inner)
                        assertThat(plan.circle).isAtMost(plan.zone)
                        val body = GlanceFit.bandLine(WidgetType.BODY, scale)
                        val words = GlanceFit.bandLine(plan.heroSp, scale) + body * plan.detailLines +
                            body * plan.sentenceLines
                        when (plan.layout) {
                            WordsLayout.UNDER -> assertThat(words + plan.circle).isAtMost(plan.zone + 0.01f)
                            WordsLayout.BESIDE -> assertThat(words).isAtMost(plan.zone + 0.01f)
                            WordsLayout.ALONE -> error("a row with no layout")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `a slot without its name keeps its icon, its figure yielding beside it`() {
        val sp = GlanceFit.figureKeepingIcon(60f, 2.9f, 1f, GlanceFit.SLOT_FIGURE_SP, GlanceFit.SLOT_ICON)
        assertThat(sp).isLessThan(GlanceFit.SLOT_FIGURE_MIN_SP)
        assertThat(widthOf(2.9f, sp, 1f)).isAtMost(60f - GlanceFit.SLOT_ICON - GlanceFit.ICON_GAP)
        assertThat(GlanceFit.figureKeepingIcon(80f, 2f, 1f, GlanceFit.SLOT_FIGURE_SP, GlanceFit.SLOT_ICON))
            .isEqualTo(GlanceFit.SLOT_FIGURE_SP)
    }

    @Test
    fun `where nothing stands with the row, the card is its cap's alone`() {
        val plan = GlanceFit.wordsPlan(others(159f, 150f, scale = 1.3f))
        assertThat(plan.others).isNull()
        assertThat(plan.layout).isEqualTo(WordsLayout.ALONE)
        // One slot wide, the row would only count them.
        assertThat(GlanceFit.wordsPlan(others(140f, 293f, count = 3)).others).isNull()
        assertThat(GlanceFit.wordsPlan(others(140f, 293f, count = 1)).others).isNotNull()
        // And none asked for, none drawn.
        assertThat(GlanceFit.wordsPlan(words(340f, 189f)).others).isNull()
    }

    // --- «List» -----------------------------------------------------------------------------

    @Test
    fun `glance forms by size`() {
        assertThat(GlanceFit.glanceForm(85f, 85f)).isEqualTo(GlanceForm.CELL)
        assertThat(GlanceFit.glanceForm(250f, 85f)).isEqualTo(GlanceForm.ROW)
        assertThat(GlanceFit.glanceForm(340f, 189f)).isEqualTo(GlanceForm.GRID)
    }

    @Test
    fun `one cell holds its figure and says the word`() {
        val plan = GlanceFit.glancePlan(GlanceSpec(85f, 85f, 1f, caps = 3, cellFigureEm = 2.2f))
        assertThat(plan.shown).isEqualTo(1)
        assertThat(plan.cellWord).isTrue()
        assertThat(widthOf(2.2f, plan.cellFigureSp, 1f)).isAtMost(85f - GlanceFit.CELL_PADDING * 2)
        val inner = 85f - GlanceFit.CARD_PADDING_SNUG * 2
        var used = lineHeight(plan.cellFigureSp, 1f) + lineHeight(GlanceFit.CELL_WORD_SP, 1f)
        if (plan.cellIcon) used += GlanceFit.CELL_ICON + GlanceFit.CELL_ICON_GAP
        if (plan.cellBar) used += GlanceFit.BAR_HEIGHT + GlanceFit.BAR_GAP
        assertThat(used).isAtMost(inner)
    }

    @Test
    fun `a row holds as many slots as its width, the last counting the rest`() {
        val four = GlanceFit.glancePlan(GlanceSpec(340f, 85f, 1f, caps = 4))
        assertThat(four.columns).isEqualTo(4)
        assertThat(four.shown).isEqualTo(4)
        assertThat(four.more).isEqualTo(0)
        val seven = GlanceFit.glancePlan(GlanceSpec(340f, 85f, 1f, caps = 7))
        assertThat(seven.columns).isEqualTo(4)
        assertThat(seven.shown).isEqualTo(3)
        assertThat(seven.more).isEqualTo(4)
        val two = GlanceFit.glancePlan(GlanceSpec(159f, 85f, 1f, caps = 2))
        assertThat(two.columns).isEqualTo(2)
        assertThat(two.more).isEqualTo(0)
    }

    @Test
    fun `a grid's rows fit its height, the rest counted`() {
        listOf(1f, 1.15f, 1.3f).forEach { scale ->
            listOf(159f, 188f, 250f, 340f).forEach { width ->
                listOf(189f, 218f, 293f).forEach { height ->
                    val plan = GlanceFit.glancePlan(GlanceSpec(width, height, scale, caps = 12))
                    assertThat(plan.shown + plan.more).isEqualTo(12)
                    assertThat(plan.shown + 1).isEqualTo(plan.rows * plan.columns)
                    val item = GlanceFit.itemHeight(plan.inline, scale)
                    val used = plan.rows * item + (plan.rows - 1) * GlanceFit.GRID_ROW_GAP
                    assertThat(used).isAtMost(height - GlanceFit.CARD_PADDING * 2)
                }
            }
        }
    }

    @Test
    fun `an item's name steps down a size before its figure's words cut it`() {
        // 4×2 at the reference grant, two columns: "Svago" beside "6 € oltre".
        val column = (340f - GlanceFit.CARD_PADDING * 2 - GlanceFit.GRID_COLUMN_GAP) / 2
        // Beside a figure alone, the name keeps the label's size.
        assertThat(GlanceFit.itemNameSp(column, nameEm = 2.9f, figureEm = 2.2f, fontScale = 1f))
            .isEqualTo(GlanceFit.ITEM_NAME_SP)
        // Beside the words of a cap over, it has less than its width: the caption's size, whole.
        val small = GlanceFit.itemNameSp(column, nameEm = 2.9f, figureEm = 4.6f, fontScale = 1f)
        assertThat(small).isEqualTo(GlanceFit.ITEM_NAME_SMALL_SP)
        val beside = column - GlanceFit.ITEM_ICON - GlanceFit.ICON_GAP - GlanceFit.ITEM_FIGURE_GAP -
            widthOf(4.6f, GlanceFit.ITEM_FIGURE_SP, 1f)
        assertThat(widthOf(2.9f, small, 1f)).isAtMost(beside)
        assertThat(widthOf(2.9f, GlanceFit.ITEM_NAME_SP, 1f)).isGreaterThan(beside)
    }

    @Test
    fun `a grid with few caps uses the rows it needs`() {
        val plan = GlanceFit.glancePlan(GlanceSpec(340f, 293f, 1f, caps = 3))
        assertThat(plan.columns).isEqualTo(2)
        assertThat(plan.rows).isEqualTo(2)
        assertThat(plan.shown).isEqualTo(3)
        assertThat(plan.more).isEqualTo(0)
    }

    @Test
    fun `no container holds more than five groups`() {
        val plan = GlanceFit.glancePlan(GlanceSpec(600f, 900f, 1f, caps = 40))
        assertThat(plan.rows).isAtMost(GlanceFit.GROUP_MAX)
        assertThat(plan.columns).isAtMost(GlanceFit.GROUP_MAX)
        val row = GlanceFit.glancePlan(GlanceSpec(900f, 85f, 1f, caps = 40))
        assertThat(row.columns).isAtMost(GlanceFit.GROUP_MAX)
    }

    private fun circle(width: Float, height: Float, sentence: Boolean = true): Float =
        GlanceFit.wordsPlan(words(width, height, sentence = sentence)).circle

    private fun words(
        width: Float,
        height: Float,
        scale: Float = 1f,
        sentence: Boolean = true,
        heroEm: Float = 3.6f,
        cellFigureEms: List<Float> = listOf(2.2f),
        sentenceLines: List<Int> = listOf(2, 1),
        detailLines: List<Int> = listOf(1),
        rowLineEms: List<Float> = listOf(9f, 4.5f, 3f),
        nameWordEm: Float = 3f,
    ) = WordsSpec(
        width = width,
        height = height,
        fontScale = scale,
        sentence = sentence,
        heroEm = heroEm,
        rowFigureEm = 3.4f,
        cellFigureEms = cellFigureEms,
        rowLineEms = rowLineEms,
        nameWordEm = nameWordEm,
        paceEms = listOf(5.5f, 4f),
        paceTwoLineEms = listOf(3.2f, 2.4f),
        sentenceLines = sentenceLines,
        detailLines = detailLines,
    )

    private fun row(spec: WordsSpec): OthersPlan = checkNotNull(GlanceFit.wordsPlan(spec).others)

    /** A tall card with [count] other caps, its words measured beside the ring as wide as it is. */
    private fun others(
        width: Float,
        height: Float,
        count: Int = 4,
        scale: Float = 1f,
        sentence: Boolean = true,
        detailLines: List<Int> = listOf(1, 2, 1),
    ) = words(width, height, scale = scale, sentence = sentence, detailLines = detailLines).copy(
        others = count,
        othersDetailLines = detailLines,
        besideWidth = GlanceFit.othersBeside(width, height, scale),
        besideDetailLines = listOf(1, 2, 1),
        besideSentenceLines = listOf(2, 1),
    )
}
