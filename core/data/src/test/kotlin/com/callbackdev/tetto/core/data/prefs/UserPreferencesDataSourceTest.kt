package com.callbackdev.tetto.core.data.prefs

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.tetto.core.data.TestDataStore
import com.callbackdev.tetto.core.model.AppFont
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.ThemeMode
import com.callbackdev.tetto.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.DayOfWeek

/** The settings file (PLANNING.md §5): defaults unwritten, unreadable values read as defaults. */
class UserPreferencesDataSourceTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var store: TestDataStore
    private lateinit var source: UserPreferencesDataSource

    @Before
    fun setUp() {
        store = TestDataStore(folder.root)
        source = UserPreferencesDataSource(store.dataStore)
    }

    @After
    fun tearDown() {
        store.scope.cancel()
    }

    @Test
    fun `nothing stored reads as the defaults`() = runTest {
        assertThat(source.current()).isEqualTo(UserSettings())
    }

    @Test
    fun `a choice survives, and a default leaves no key`() = runTest {
        source.update { it.copy(currency = "CHF", monthStartDay = 27, weekStart = DayOfWeek.SUNDAY) }
        assertThat(source.current()).isEqualTo(UserSettings("CHF", 27, DayOfWeek.SUNDAY))

        source.update { UserSettings() }
        assertThat(store.dataStore.data.first().asMap()).isEmpty()
    }

    @Test
    fun `a value that cannot be read back reads as its default`() = runTest {
        store.dataStore.edit {
            it[stringPreferencesKey("currency")] = "ZZZ"
            it[intPreferencesKey("month_start_day")] = 40
            it[stringPreferencesKey("week_start")] = "SOMEDAY"
        }
        assertThat(source.current()).isEqualTo(UserSettings())
    }

    @Test
    fun `the appearance and the first run's flag survive, and their defaults leave no key`() = runTest {
        val chosen = UserSettings(
            theme = ThemeMode.DARK,
            palette = AppPalette.PAPER,
            font = AppFont.INTER,
            dynamicColor = true,
            onboardingCompleted = true,
        )
        source.update { chosen }
        assertThat(source.current()).isEqualTo(chosen)
        assertThat(store.dataStore.data.first().asMap().keys.map { it.name })
            .containsExactly("theme", "palette", "font", "dynamic_color", "onboarding_completed")

        source.update { it.copy(theme = ThemeMode.SYSTEM, palette = AppPalette.VIVID, font = AppFont.GOOGLE_SANS) }
        assertThat(store.dataStore.data.first().asMap().keys.map { it.name })
            .containsExactly("dynamic_color", "onboarding_completed")
    }

    @Test
    fun `an appearance from a newer build reads as the default`() = runTest {
        store.dataStore.edit {
            it[stringPreferencesKey("theme")] = "SEPIA"
            it[stringPreferencesKey("palette")] = "NEON"
            it[stringPreferencesKey("font")] = "COMIC"
        }
        assertThat(source.current()).isEqualTo(UserSettings())
    }

    @Test
    fun `an out of range month start is held in range when written`() = runTest {
        assertThat(source.update { it.copy(monthStartDay = 0) }.monthStartDay).isEqualTo(1)
        assertThat(source.update { it.copy(monthStartDay = 32) }.monthStartDay).isEqualTo(31)
    }
}
