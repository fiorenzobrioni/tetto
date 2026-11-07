package com.callbackdev.tetto.shell

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The first run's evening reminder (owner, 7 Nov 2026): offered, its notification shown as an example. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class ReminderOfferPageTest {
    @get:Rule val compose = createComposeRule()

    private var reminded = 0
    private var left = 0

    private fun draw(dark: Boolean = false) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                ReminderOfferPage(
                    time = UserSettings.DEFAULT_REMINDER_TIME,
                    onRemind = { reminded++ },
                    onLeave = { left++ },
                )
            }
        }
    }

    @Test
    fun `the reminder offered, its notification as an example, and the two ways on`() {
        draw()
        compose.onNodeWithText("A word in the evening").assertExists()
        compose.onNodeWithTag(ReminderOfferTags.EXAMPLE)
            .assertContentDescriptionEquals(
                "An example of the evening’s notification, at 20:30: Anything to add today?",
            )
        compose.onNodeWithText("Remind me at 20:30").performClick()
        compose.onNodeWithTag(ReminderOfferTags.LATER).performClick()
        assertThat(reminded to left).isEqualTo(1 to 1)
        compose.assertAccessible()
        compose.writeScreenshot("reminder-offer")
    }

    @Test
    fun `dark`() {
        draw(dark = true)
        compose.writeScreenshot("reminder-offer-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw()
        compose.onNodeWithText("Un promemoria la sera").assertExists()
        compose.onNodeWithText("Ricordamelo alle 20:30").assertExists()
        compose.writeScreenshot("reminder-offer-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone`() {
        draw()
        compose.walkPage(hasTestTag(ReminderOfferTags.SCROLL), "reminder-offer-large-text")
    }
}
