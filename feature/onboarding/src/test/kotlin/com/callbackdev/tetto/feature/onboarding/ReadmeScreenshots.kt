package com.callbackdev.tetto.feature.onboarding

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's picture of the first run (docs/screenshots): the areas, four of them picked. Run
 * with `./gradlew :feature:onboarding:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
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
    fun onboarding() {
        val picks = listOf(CapIcon.GROCERIES, CapIcon.EATING_OUT, CapIcon.COFFEE, CapIcon.TRANSPORT)
            .fold(FirstRunPicks()) { picks, area -> picks.toggle(area) }
        compose.setContent {
            TettoTheme {
                OnboardingScreen(OnboardingStep.AREAS, picks, AppCurrency("EUR", 2), OnboardingActions())
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "onboarding.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
