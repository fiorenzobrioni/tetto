package com.callbackdev.tetto.feature.caps

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.domain.money.MoneyInput
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of the caps screen, the numpad and a cap's sheet (docs/screenshots), drawn
 * from [CapsSample]'s Thursday, in euros. Run with
 * `./gradlew :feature:caps:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 *
 * The two sheets are drawn over the page as Material's modal sheet draws them (the scrim, the
 * rounded ground, the handle): its own window would leave the page out of the picture.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("tetto.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private val fuel = CapsSample.state(6, "Fuel", CapIcon.FUEL, euros(150), PeriodKind.MONTH, euros(52))

    /** Five caps of different periods, all under: the sentence names the closest. */
    private val caps = CapsSample.underCaps + fuel

    @Test
    fun caps() {
        show { Page(CapsSample.ui(caps)) }
        save("caps")
    }

    /** In the dark, one cap over: said in words on its tile and in the sentence. */
    @Test
    fun capsDark() {
        show(dark = true) {
            Page(CapsSample.ui(listOf(CapsSample.coffee, CapsSample.shopping) + CapsSample.underCaps.drop(1)))
        }
        save("caps-dark")
    }

    /** The numpad over the page, "3.20" typed key by key, its preview saying what it leaves. */
    @Test
    fun numpad() {
        val typed = listOf(
            MoneyInput.Key.Digit(3),
            MoneyInput.Key.Separator,
            MoneyInput.Key.Digit(2),
            MoneyInput.Key.Digit(0),
        )
            .fold(MoneyInput.empty(CapsSample.EUR)) { input, key -> input.press(key) }
        show {
            Box {
                Page(CapsSample.ui(caps))
                SheetOver {
                    NumpadContent(
                        state = CapsSample.coffee,
                        format = rememberMoneyFormat(CapsSample.EUR),
                        input = typed,
                        correcting = null,
                        onKey = {},
                        onConfirm = {},
                        onOpenCap = {},
                    )
                }
            }
        }
        save("numpad")
    }

    /** A cap's sheet: this period, its spends, the last periods' lines that explain the reserve. */
    @Test
    fun capSheet() {
        show {
            Box {
                Page(CapsSample.ui(caps))
                SheetOver {
                    CapSheetContent(
                        state = CapsSample.coffee,
                        data = CapSheetData(CapsSample.coffee.cap.id, CapsSample.coffeeSpends, CapsSample.coffeeLines),
                        format = rememberMoneyFormat(CapsSample.EUR),
                        today = CapsSample.today,
                        actions = CapSheetActions({}, {}, {}, {}, {}),
                    )
                }
            }
        }
        save("cap-sheet")
    }

    @Composable
    private fun Page(state: CapsUiState) {
        Surface(Modifier.fillMaxSize()) {
            CapsScreen(state, onAddSpend = {}, onOpenCap = {}, onNewCap = {}, onChangeOrder = {}, onOpenSettings = {})
        }
    }

    /** Material's modal sheet, drawn in place: the scrim, the ground with its top corners, the handle. */
    @Composable
    private fun SheetOver(content: @Composable () -> Unit) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(vertical = 22.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HANDLE_ALPHA),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                    content()
                }
            }
        }
    }

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent { TettoTheme(darkTheme = dark) { content() } }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val SCRIM_ALPHA = 0.32f
        const val HANDLE_ALPHA = 0.4f
    }
}
