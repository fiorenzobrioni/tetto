package com.callbackdev.tetto.feature.guide

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek

/**
 * The README's picture of the guide (docs/screenshots), in euros, weeks from Monday. Run with
 * `./gradlew :feature:guide:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("tetto.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    /** The reserve's story: the chapter a screen alone could not tell. */
    @Test
    fun guide() {
        compose.setContent {
            TettoTheme { GuideScreen(GuideUiState(AppCurrency("EUR", 2), DayOfWeek.MONDAY), onBack = {}) }
        }
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.STORY))
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "guide.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
