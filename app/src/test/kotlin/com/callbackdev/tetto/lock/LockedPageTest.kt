package com.callbackdev.tetto.lock

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What stands in Tetto's place until the phone's lock says yes (owner, 6 Nov 2026). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class LockedPageTest {
    @get:Rule val compose = createComposeRule()

    private var asked = 0

    private fun draw(dark: Boolean = false) {
        compose.setContent { TettoTheme(darkTheme = dark) { LockedPage(onUnlock = { asked++ }) } }
    }

    @Test
    fun `nothing of the reader's, the lock, and a button that asks again`() {
        draw()
        compose.onNodeWithText("Tetto is locked").assertExists()
        compose.onNodeWithText("Your fingerprint, your face", substring = true).assertExists()
        compose.onNodeWithTag(LockTags.UNLOCK).performClick()
        assertThat(asked).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("locked")
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        compose.writeScreenshot("locked-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.onNodeWithText("Tetto è bloccato").assertExists()
        compose.onNodeWithText("Sblocca").assertExists()
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        draw()
        compose.walkPage(hasTestTag(LockTags.PAGE), "locked-large-text")
    }
}
