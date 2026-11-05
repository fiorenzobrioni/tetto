package com.callbackdev.tetto.shell

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The caps screen before any cap (PLANNING.md §11 Phase 0), in both languages and both themes. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class CapsEmptyPageTest {
    @get:Rule val compose = createComposeRule()

    private fun draw(dark: Boolean = false) {
        compose.setContent { TettoTheme(darkTheme = dark) { TettoRoot() } }
    }

    @Test
    fun `says there is no cap yet`() {
        draw()
        compose.onNodeWithText("No caps yet").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("caps-empty")
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        compose.onNodeWithText("No caps yet").assertIsDisplayed()
        compose.writeScreenshot("caps-empty-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.onNodeWithText("Ancora nessun tetto").assertIsDisplayed()
        compose.writeScreenshot("caps-empty-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(ShellTags.PAGE), "caps-empty-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        draw()
        compose.walkPage(hasTestTag(ShellTags.PAGE), "caps-empty-foldable")
    }
}
