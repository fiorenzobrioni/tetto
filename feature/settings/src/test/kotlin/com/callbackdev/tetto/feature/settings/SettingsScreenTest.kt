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
import com.callbackdev.tetto.core.data.backup.DataContents
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerTags
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.domain.backup.Backup
import com.callbackdev.tetto.core.domain.backup.BackupCap
import com.callbackdev.tetto.core.domain.backup.BackupSpend
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.reminder.ReminderBlock
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.callbackdev.tetto.feature.settings.data.DataActions
import com.callbackdev.tetto.feature.settings.data.DataFailure
import com.callbackdev.tetto.feature.settings.data.DataOutcome
import com.callbackdev.tetto.feature.settings.data.DataTags
import com.callbackdev.tetto.feature.settings.data.DataTask
import com.callbackdev.tetto.feature.settings.data.DataUiState
import com.callbackdev.tetto.feature.settings.data.RestorePreview
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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
    private var reminder: Boolean? = null
    private var reminderTime: LocalTime? = null
    private var fixed = false
    private var restored = false
    private var restoreCancelled = false
    private var erased = false
    private var dismissed = false

    private val euro = AppCurrency("EUR", 2)

    private fun state(
        settings: UserSettings = UserSettings(currency = "EUR", onboardingCompleted = true),
        capCount: Int = 3,
    ) = SettingsUiState(settings, euro, DayOfWeek.MONDAY, LocalDate.of(2026, 11, 5), capCount, "0.1.0")

    private fun draw(
        state: SettingsUiState?,
        dark: Boolean = false,
        reach: ReminderReach = ReminderReach.Clear,
        data: DataUiState = DataUiState(DataContents(caps = 4, spends = 37)),
    ) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                SettingsScreen(
                    state = state,
                    onBack = {},
                    reach = reach,
                    data = data,
                    dataActions = DataActions(
                        confirmRestore = { restored = true },
                        cancelRestore = { restoreCancelled = true },
                        erase = { erased = true },
                        dismissOutcome = { dismissed = true },
                    ),
                    actions = SettingsActions(
                        setCurrency = { currency = it },
                        setMonthStartDay = { monthStart = it },
                        setWeekStart = {
                            weekStart = it
                            weekStartSet = true
                        },
                        setTheme = { theme = it },
                        setDynamicColor = { dynamic = it },
                        setReminder = { reminder = it },
                        setReminderTime = { reminderTime = it },
                        fixNotifications = { fixed = true },
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

    // The evening reminder (PLANNING.md §11 Phase 4).

    private fun scrollTo(tag: String) {
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun `the reminder says what it asks and when, and turns on from its row`() {
        draw(state())
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("Evening reminder").assertIsDisplayed()
        compose.onNodeWithText("“Anything to add today?” around 20:30, only on a day with no spend added")
            .assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.REMINDER_BLOCKED).assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("settings-reminder")
        compose.onNodeWithTag(SettingsTags.REMINDER).performClick()
        assertThat(reminder).isTrue()
    }

    @Test
    fun `on, before Android has asked, a card asks for the permission`() {
        draw(
            state(UserSettings(currency = "EUR", reminderOn = true)),
            reach = ReminderReach(ReminderBlock.APP, askable = true),
        )
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("Tetto may not notify you yet").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-reminder-ask")
        compose.onNodeWithText("Allow").performClick()
        assertThat(fixed).isTrue()
    }

    @Test
    fun `refused for good, or its channel off, the card opens the phone's settings`() {
        draw(
            state(UserSettings(currency = "EUR", reminderOn = true)),
            reach = ReminderReach(ReminderBlock.APP, askable = false),
        )
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("Notifications are off for Tetto").assertIsDisplayed()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
        compose.writeScreenshot("settings-reminder-blocked")
    }

    @Test
    fun `the reminder's channel off is said as such`() {
        draw(
            state(UserSettings(currency = "EUR", reminderOn = true)),
            reach = ReminderReach(ReminderBlock.CHANNEL, askable = false),
        )
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("The reminder’s notifications are off").assertIsDisplayed()
    }

    @Test
    fun `off, a block says nothing, since the switch promises nothing`() {
        draw(state(), reach = ReminderReach(ReminderBlock.APP, askable = false))
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithTag(SettingsTags.REMINDER_BLOCKED).assertDoesNotExist()
    }

    @Test
    fun `the time, on the clock dial, saved as it was left`() {
        draw(state(UserSettings(currency = "EUR", reminderOn = true, reminderTime = LocalTime.of(21, 15))))
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("21:15").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.REMINDER_TIME).performClick()
        compose.onNodeWithText("Android may bring it a few minutes later", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-reminder-time")
        compose.onNodeWithTag(SettingsTags.REMINDER_TIME_SAVE).performClick()
        // The same time is no change to write.
        assertThat(reminderTime).isNull()
        compose.onNodeWithTag(SettingsTags.REMINDER_TIME_SAVE).assertDoesNotExist()
    }

    @Test
    fun `the reminder's permission is askable until a refusal Android will not repeat`() {
        assertThat(
            ReminderReach.of(ReminderBlock.APP, granted = false, askedBefore = false, showRationale = false).askable,
        )
            .isTrue()
        assertThat(
            ReminderReach.of(ReminderBlock.APP, granted = false, askedBefore = true, showRationale = true).askable,
        )
            .isTrue()
        assertThat(
            ReminderReach.of(ReminderBlock.APP, granted = false, askedBefore = true, showRationale = false).askable,
        )
            .isFalse()
        // Given, but the app's notifications turned off in the system: only its settings can help.
        assertThat(
            ReminderReach.of(ReminderBlock.APP, granted = true, askedBefore = true, showRationale = false).askable,
        )
            .isFalse()
    }

    // Your data (PLANNING.md §11 Phase 4).

    @Test
    fun `your data, each row saying what it carries`() {
        draw(state())
        scrollTo(DataTags.ERASE)
        compose.onNodeWithText("Your data").assertIsDisplayed()
        compose.onNodeWithText("4 caps and 37 spends, with your settings, in a file you choose").assertIsDisplayed()
        compose.onNodeWithText("37 spends, one row each, as a CSV file").assertIsDisplayed()
        compose.onNodeWithText("Erase everything").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-data")
    }

    @Test
    fun `with no spends, no table to offer`() {
        draw(state(), data = DataUiState(DataContents(caps = 0, spends = 0)))
        scrollTo(DataTags.SPENDS)
        compose.onNodeWithTag(DataTags.SPENDS).assertIsNotEnabled()
        compose.onNodeWithText("No spends yet").assertIsDisplayed()
        compose.onNodeWithText("No caps yet: the file would hold your settings alone").assertIsDisplayed()
    }

    @Test
    fun `while a file is written, the rows wait`() {
        draw(state(), data = DataUiState(DataContents(4, 37), task = DataTask.WRITING_BACKUP))
        scrollTo(DataTags.ERASE)
        compose.onNodeWithText("Writing the file…").assertIsDisplayed()
        compose.onNodeWithTag(DataTags.RESTORE).assertIsNotEnabled()
        compose.onNodeWithTag(DataTags.ERASE).assertIsNotEnabled()
    }

    @Test
    fun `a backup saved says where, and that the file is not encrypted`() {
        val saved = DataOutcome.BackupWritten("tetto-backup-2026-11-05.json", caps = 4, spends = 37)
        draw(state(), data = DataUiState(DataContents(4, 37), outcome = saved))
        scrollTo(DataTags.OUTCOME)
        compose.onNodeWithText("Backup saved").assertIsDisplayed()
        compose.onNodeWithText("in tetto-backup-2026-11-05.json. The file is not encrypted", substring = true)
            .assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-data-saved")
        compose.onNodeWithText("OK").performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun `a damaged file is said so, and that nothing changed`() {
        draw(state(), data = DataUiState(DataContents(4, 37), outcome = DataOutcome.Failed(DataFailure.DAMAGED)))
        scrollTo(DataTags.OUTCOME)
        compose.onNodeWithText("The backup is damaged").assertIsDisplayed()
        compose.onNodeWithText("Nothing changed.", substring = true).assertIsDisplayed()
    }

    @Test
    fun `before a restore, what the file holds and what it replaces`() {
        val preview = RestorePreview(sampleBackup(), "tetto-backup-2026-11-05.json")
        draw(state(), data = DataUiState(DataContents(caps = 3, spends = 20), preview = preview))
        compose.onNodeWithText("Restore this backup?").assertIsDisplayed()
        compose.onNodeWithText("by Tetto 0.1.0", substring = true).assertIsDisplayed()
        // The locale's own list: British English puts no comma before its "and".
        compose.onNodeWithText("5 caps: Coffee, Groceries, Shopping and 2 more").assertIsDisplayed()
        compose.onNodeWithText("6 spends").assertIsDisplayed()
        compose.onNodeWithText("Amounts in Euro").assertIsDisplayed()
        compose.onNodeWithText("It replaces the 3 caps on this phone", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-data-restore")
        assertThat(restored).isFalse()
        compose.onNodeWithTag(DataTags.CONFIRM_RESTORE).performClick()
        assertThat(restored).isTrue()
    }

    @Test
    fun `on a phone with no caps, the restore says the settings come too`() {
        val preview = RestorePreview(sampleBackup(), null)
        draw(state(capCount = 0), data = DataUiState(DataContents(caps = 0, spends = 0), preview = preview))
        compose.onNodeWithText("Your settings become the backup’s too.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertThat(restoreCancelled).isTrue()
    }

    @Test
    fun `erasing everything is said in full, and done only on its button`() {
        draw(state())
        scrollTo(DataTags.ERASE)
        compose.onNodeWithTag(DataTags.ERASE).performClick()
        compose.onNodeWithText("Erase everything?").assertIsDisplayed()
        compose.onNodeWithText("Your 4 caps, their spends and their reserves", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("settings-data-erase")
        assertThat(erased).isFalse()
        compose.onNodeWithTag(DataTags.CONFIRM_ERASE).performClick()
        assertThat(erased).isTrue()
        compose.onNodeWithText("Erase everything?").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the reminder and your data in Italian`() {
        draw(
            state(UserSettings(currency = "EUR", reminderOn = true)),
            reach = ReminderReach(ReminderBlock.APP, askable = true),
        )
        scrollTo(SettingsTags.REMINDER_TIME)
        compose.onNodeWithText("Promemoria serale").assertIsDisplayed()
        compose.onNodeWithText("Consenti").assertIsDisplayed()
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("I tuoi dati"))
        compose.onNodeWithText("I tuoi dati").assertIsDisplayed()
        scrollTo(DataTags.ERASE)
        compose.onNodeWithText(
            "4 tetti e 37 spese, con le tue impostazioni, in un file che scegli tu",
        ).assertIsDisplayed()
        compose.writeScreenshot("settings-data-it")
    }

    /** Five caps a reader in Rome backed up, six spends among them. */
    private fun sampleBackup(): Backup {
        val written = LocalDateTime.of(2026, 11, 5, 21, 14).atZone(ZoneId.of("Europe/Rome")).toInstant()
        fun cap(name: String, icon: CapIcon, spends: Int) = BackupCap(
            name = name,
            icon = icon,
            amount = Money(20_000),
            kind = PeriodKind.MONTH,
            reserve = Money.ZERO,
            openStart = LocalDate.of(2026, 11, 1),
            createdAt = Instant.EPOCH,
            spends = List(spends) { BackupSpend(Money(500), written, LocalDate.of(2026, 11, 1)) },
            closed = emptyList(),
        )
        return Backup(
            exportedAt = written,
            zone = "Europe/Rome",
            appVersion = "0.1.0",
            currency = euro,
            settings = UserSettings(currency = "EUR"),
            caps = listOf(
                cap("Coffee", CapIcon.COFFEE, 2),
                cap("Groceries", CapIcon.GROCERIES, 2),
                cap("Shopping", CapIcon.SHOPPING, 1),
                cap("Travel", CapIcon.TRAVEL, 1),
                cap("Gifts", CapIcon.GIFTS, 0),
            ),
        )
    }
}
