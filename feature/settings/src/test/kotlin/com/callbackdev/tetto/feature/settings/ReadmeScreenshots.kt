package com.callbackdev.tetto.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.backup.DataContents
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.feature.settings.data.DataUiState
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The README's picture of Settings (docs/screenshots): the guide's card, the money with a month
 * from the 27th, the evening reminder on. Run with
 * `./gradlew :feature:settings:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("tetto.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun settings() {
        val settings = UserSettings(currency = "EUR", monthStartDay = 27, onboardingCompleted = true, reminderOn = true)
        val state =
            SettingsUiState(settings, AppCurrency("EUR", 2), DayOfWeek.MONDAY, LocalDate.of(2026, 11, 5), 6, "1.0.0")
        compose.setContent {
            TettoTheme {
                SettingsScreen(
                    state = state,
                    onBack = {},
                    actions = SettingsActions(),
                    data = DataUiState(DataContents(caps = 6, spends = 54)),
                )
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "settings.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
