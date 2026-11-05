package com.callbackdev.tetto.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerTags
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.ThemeMode
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
import java.time.DayOfWeek
import java.time.LocalDate

/** Settings, drawn from its state (PLANNING.md §11 Phase 3), never from a database. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private var currency: String? = null
    private var monthStart: Int? = null
    private var weekStart: DayOfWeek? = DayOfWeek.WEDNESDAY
    private var weekStartSet = false
    private var theme: ThemeMode? = null
    private var dynamic: Boolean? = null

    private val euro = AppCurrency("EUR", 2)

    private fun state(
        settings: UserSettings = UserSettings(currency = "EUR", onboardingCompleted = true),
        capCount: Int = 3,
    ) = SettingsUiState(settings, euro, DayOfWeek.MONDAY, LocalDate.of(2026, 11, 5), capCount, "0.1.0")

    private fun draw(state: SettingsUiState?, dark: Boolean = false) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                SettingsScreen(
                    state = state,
                    onBack = {},
                    actions = SettingsActions(
                        setCurrency = { currency = it },
                        setMonthStartDay = { monthStart = it },
                        setWeekStart = {
                            weekStart = it
                            weekStartSet = true
                        },
                        setTheme = { theme = it },
                        setDynamicColor = { dynamic = it },
                    ),
                )
            }
        }
    }

    @Test
    fun `the money first, each setting saying what it is now`() {
        draw(state())
        compose.onNodeWithText("Money").assertIsDisplayed()
        compose.onNodeWithText("Euro (EUR)").assertIsDisplayed()
        compose.onNodeWithText("Day 1 · this month:", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Monday, as the phone").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings")
    }

    @Test
    fun `before the settings are read, no list`() {
        draw(null)
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.LIST).assertDoesNotExist()
    }

    @Test
    fun `the month start, from a month's days`() {
        draw(state(UserSettings(currency = "EUR", monthStartDay = 27)))
        compose.onNodeWithText("Day 27 · this month:", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.MONTH_START).performClick()
        compose.onNodeWithText("A month too short for it starts on its last day", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-month-start")
        compose.onNodeWithTag(SettingsTags.day(15)).performClick()
        assertThat(monthStart).isEqualTo(15)
    }

    @Test
    fun `the week start, the reader's or the phone's`() {
        draw(state(UserSettings(currency = "EUR", weekStart = DayOfWeek.SUNDAY)))
        compose.onNodeWithText("Sunday").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.WEEK_START).performClick()
        compose.onNodeWithText("Monday, as the phone").performClick()
        assertThat(weekStartSet).isTrue()
        assertThat(weekStart).isNull()
    }

    @Test
    fun `a new currency, with caps to relabel, is confirmed first and says how amounts round`() {
        draw(state(capCount = 3))
        compose.onNodeWithTag(SettingsTags.CURRENCY).performClick()
        compose.onNodeWithText("it never converts them", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(CurrencyPickerTags.SEARCH).performTextInput("yen")
        compose.onNodeWithTag(CurrencyPickerTags.currency("JPY")).performClick()

        compose.onNodeWithText("Count in Japanese Yen?").assertIsDisplayed()
        compose.onNodeWithText("€12.55 becomes JP¥13", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-currency-confirm")
        assertThat(currency).isNull()
        compose.onNodeWithTag(SettingsTags.CURRENCY_CONFIRM).performClick()
        assertThat(currency).isEqualTo("JPY")
    }

    @Test
    fun `a new currency with as many decimals says nothing of rounding`() {
        draw(state(capCount = 1))
        compose.onNodeWithTag(SettingsTags.CURRENCY).performClick()
        compose.onNodeWithTag(CurrencyPickerTags.SEARCH).performTextInput("CHF")
        compose.onNodeWithTag(CurrencyPickerTags.currency("CHF")).performClick()
        compose.onNodeWithText("becomes", substring = true).assertDoesNotExist()
        compose.onNodeWithTag(SettingsTags.CURRENCY_CONFIRM).performClick()
        assertThat(currency).isEqualTo("CHF")
    }

    @Test
    fun `with no cap, a new currency needs no confirmation, and the same one changes nothing`() {
        draw(state(capCount = 0))
        compose.onNodeWithTag(SettingsTags.CURRENCY).performClick()
        compose.onNodeWithTag(CurrencyPickerTags.currency("EUR")).performClick()
        assertThat(currency).isNull()
        compose.onNodeWithTag(SettingsTags.CURRENCY).performClick()
        compose.onNodeWithTag(CurrencyPickerTags.SEARCH).performTextInput("dollar")
        compose.onNodeWithTag(CurrencyPickerTags.currency("USD")).performClick()
        assertThat(currency).isEqualTo("USD")
    }

    @Test
    fun `the appearance, a preview above the choices`() {
        draw(state())
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.PREVIEW))
        compose.onNodeWithTag(SettingsTags.PREVIEW).assertIsDisplayed()
        compose.writeScreenshot("settings-appearance")
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.THEME))
        compose.onNodeWithTag(SettingsTags.THEME).performClick()
        compose.onNodeWithText("Dark").performClick()
        assertThat(theme).isEqualTo(ThemeMode.DARK)
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.DYNAMIC_COLOR))
        compose.onNodeWithTag(SettingsTags.DYNAMIC_COLOR).performClick()
        assertThat(dynamic).isTrue()
    }

    @Test
    fun `with the wallpaper's colours, the palette waits`() {
        draw(state(UserSettings(currency = "EUR", dynamicColor = true, palette = AppPalette.PAPER)))
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Palette"))
        compose.onNodeWithText("Palette").assertIsNotEnabled()
    }

    @Test
    fun `the privacy statement says Tetto sends nothing, and what Android's backup does`() {
        draw(state())
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Tetto sends nothing", substring = true))
        compose.onNodeWithText("Android’s own backup", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("0.1.0"))
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Material Symbols", substring = true))
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw(state(UserSettings(currency = "EUR", monthStartDay = 27)))
        compose.onNodeWithText("Denaro").assertIsDisplayed()
        compose.onNodeWithText("Lunedì, come il telefono").assertIsDisplayed()
        compose.onNodeWithText("Giorno 27 · questo mese:", substring = true).assertIsDisplayed()
        compose.writeScreenshot("settings-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-night-xxhdpi")
    fun `on the dark page`() {
        draw(state(), dark = true)
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, nothing is cut`() {
        draw(state())
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings-large-text", maxScreens = 30)
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable, a centred column`() {
        draw(state())
        compose.walkPage(hasTestTag(SettingsTags.LIST), "settings-foldable")
    }
}
