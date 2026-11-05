package com.callbackdev.tetto.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Every fixed size a card prints in is a step of the family's scale; a ladder starts on one. */
class WidgetTypeTest {
    @Test
    fun `the scale is the family's`() {
        assertThat(WidgetType.steps).containsExactly(11f, 13f, 16f, 22f, 30f, 34f)
    }

    @Test
    fun `every fixed size is a step`() {
        listOf(
            GlanceFit.CELL_FIGURE_SP,
            GlanceFit.ROW_LINE_SP,
            GlanceFit.PACE_SP,
            GlanceFit.SENTENCE_SP,
            GlanceFit.CELL_WORD_SP,
            GlanceFit.SLOT_FIGURE_SP,
            GlanceFit.SLOT_NAME_SP,
            GlanceFit.ITEM_FIGURE_SP,
            GlanceFit.ITEM_NAME_SP,
        ).forEach { assertThat(WidgetType.steps).contains(it) }
    }

    @Test
    fun `the row's figure starts where the family's row title does`() {
        assertThat(GlanceFit.ROW_FIGURE_SP).isEqualTo(20f)
    }
}
