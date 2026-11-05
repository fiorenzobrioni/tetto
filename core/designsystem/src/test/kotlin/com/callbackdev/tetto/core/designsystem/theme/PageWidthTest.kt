package com.callbackdev.tetto.core.designsystem.theme

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The page's column: a phone untouched, a wide window centred, the insets kept clear. */
class PageWidthTest {
    @Test
    fun `a phone upright keeps its whole width`() {
        assertThat(PageGutter.of(411.dp, 0.dp, 0.dp)).isEqualTo(PageGutter.None)
        assertThat(PageGutter.of(PageMaxWidth, 0.dp, 0.dp)).isEqualTo(PageGutter.None)
    }

    @Test
    fun `a foldable open centres the column`() {
        assertThat(PageGutter.of(840.dp, 0.dp, 0.dp)).isEqualTo(PageGutter(100.dp, 100.dp))
    }

    @Test
    fun `a cutout or a side bar is kept clear on its own side`() {
        assertThat(PageGutter.of(411.dp, 24.dp, 0.dp)).isEqualTo(PageGutter(24.dp, 0.dp))
        assertThat(PageGutter.of(891.dp, 48.dp, 3.dp)).isEqualTo(PageGutter(148.dp, 103.dp))
    }
}
