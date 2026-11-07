package com.callbackdev.tetto.feature.caps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.DateRange
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.callbackdev.tetto.feature.caps.CapsSample.euros
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * The one-off cap on the screens (owner, 6 Nov 2026; PLANNING.md §11 Phase 6b): its tile «until»
 * its last day, an ended one last and muted, its sheet's two actions, the sentence the day after,
 * the editor's «Once» with its dates, and the numpad over the home screen on an ended one.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class OneOffScreensTest {
    @get:Rule val compose = createComposeRule()

    private val added = mutableListOf<Long>()
    private val opened = mutableListOf<Long>()

    private fun drawCaps(caps: List<CapState>, layout: CapsLayout = CapsLayout.TILES) {
        compose.setContent {
            TettoTheme {
                CapsScreen(
                    state = CapsSample.ui(caps, layout = layout),
                    onAddSpend = { added += it },
                    onOpenCap = { opened += it },
                    onNewCap = {},
                    onChangeOrder = {},
                    onOpenSettings = {},
                )
            }
        }
    }

    private fun description(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    @Test
    fun `an open one-off reads as any cap, until its last day`() {
        drawCaps(listOf(CapsSample.coffee, CapsSample.lisbon))
        // A date is held together by a no-break space: "13 Nov" never breaks between its parts.
        assertThat(description(CapsTags.tile(7)))
            .isEqualTo("Lisbon trip, €560 left of €800, until 13\u00A0Nov, about €62 a day")
        compose.onNodeWithTag(CapsTags.tile(7)).performClick()
        assertThat(added).containsExactly(7L)
    }

    @Test
    fun `an ended one-off stands last and muted, the sentence says how it ended, a touch opens its sheet`() {
        drawCaps(listOf(CapsSample.paris(), CapsSample.coffee, CapsSample.lisbon, CapsSample.groceries))
        compose.onNodeWithText("Paris weekend ended: €280 of €300.").assertExists()
        assertThat(description(CapsTags.tile(8))).isEqualTo("Paris weekend, ended on 4\u00A0Nov, €280 spent of €300")
        compose.onNodeWithTag(CapsTags.tile(8)).performScrollTo().performClick()
        assertThat(opened).containsExactly(8L)
        assertThat(added).isEmpty()
        compose.assertAccessible()
        compose.writeScreenshot("caps-one-off")
    }

    @Test
    fun `an ended one-off in the list`() {
        drawCaps(listOf(CapsSample.paris(), CapsSample.coffee, CapsSample.lisbon), CapsLayout.LIST)
        compose.onNodeWithTag(CapsTags.tile(8)).performScrollTo().performClick()
        assertThat(opened).containsExactly(8L)
        compose.assertAccessible()
        compose.writeScreenshot("caps-one-off-list")
    }

    @Test
    fun `ended over, over is said in words`() {
        val over = CapsSample.paris(CapsSample.parisLine.copy(spent = euros(350), forgiven = euros(50)))
        drawCaps(listOf(over, CapsSample.coffee))
        compose.onNodeWithText("Paris weekend ended: €350 of €300. It was €50 over: forgiven.").assertExists()
        assertThat(description(CapsTags.tile(8))).endsWith("€350 spent of €300, €50 over, forgiven")
    }

    @Test
    fun `an ended one-off's sheet offers it again with new dates, or to delete it`() {
        var again = 0
        var deleted = 0
        compose.setContent {
            TettoTheme {
                CapSheetContent(
                    state = CapsSample.paris(),
                    data = CapSheetData(8, emptyList(), listOf(CapsSample.parisLine)),
                    format = rememberMoneyFormat(CapsSample.EUR),
                    today = CapsSample.today,
                    actions = CapSheetActions(
                        onCorrect = {},
                        onDelete = {},
                        onClearReserve = {},
                        onEdit = {},
                        onDeleteCap = { deleted++ },
                        onAgain = { again++ },
                    ),
                )
            }
        }
        compose.onNodeWithTag(CapsTags.RECEIPT).fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            .single().let { assertThat(it).isEqualTo("€300 cap, less €280 spent: €20 to spare") }
        compose.onNodeWithText("Add a refund").assertDoesNotExist()
        compose.onNodeWithText("Edit").assertDoesNotExist()
        compose.onNodeWithTag(CapsTags.AGAIN).performScrollTo().performClick()
        compose.onNodeWithText("Delete the cap").performScrollTo().performClick()
        assertThat(again to deleted).isEqualTo(1 to 1)
        compose.assertAccessible()
        compose.writeScreenshot("cap-sheet-ended")
    }

    @Test
    fun `the editor's Once asks for its dates, and saves only with them`() {
        var draft by mutableStateOf(CapDraft("Lisbon trip", euros(800), PeriodKind.MONTH, CapIcon.TRAVEL))
        compose.setContent {
            TettoTheme {
                CapEditorScreen(
                    editing = null,
                    draft = draft,
                    currency = CapsSample.EUR,
                    onChange = { draft = it },
                    onSave = {},
                    onBack = {},
                    today = CapsSample.today,
                )
            }
        }
        compose.onNodeWithText("Once").performClick()
        // The picker opens at once; dismissed, the row asks for the dates and Save waits.
        compose.onNodeWithText("The cap’s dates").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Choose the first and the last day").assertExists()
        compose.onNodeWithTag(CapsTags.EDITOR_SAVE).performScrollTo().assertIsNotEnabled()
        draft = draft.copy(dates = DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 21)))
        compose.onNodeWithText("10–20 Nov · 11 days", substring = true, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag(CapsTags.EDITOR_SAVE).performScrollTo().assertIsEnabled()
        compose.assertAccessible()
        compose.writeScreenshot("editor-once")
    }

    @Test
    fun `a cap with a reserve turned into a one-off says the reserve is let go`() {
        compose.setContent {
            TettoTheme {
                CapEditorScreen(
                    editing = PeriodKind.WEEK,
                    draft = CapDraft(
                        "Coffee",
                        euros(20),
                        PeriodKind.ONE_OFF,
                        CapIcon.COFFEE,
                        DateRange(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12)),
                    ),
                    currency = CapsSample.EUR,
                    onChange = {},
                    onSave = {},
                    onBack = {},
                    today = CapsSample.today,
                    reserve = euros(4),
                )
            }
        }
        compose.onNodeWithText("Its €4 in reserve is let go: a one-off cap keeps none.").assertExists()
    }

    @Test
    fun `again with new dates, the editor says so and opens the picker`() {
        compose.setContent {
            TettoTheme {
                CapEditorScreen(
                    editing = PeriodKind.ONE_OFF,
                    draft = CapDraft("Paris weekend", euros(300), PeriodKind.ONE_OFF, CapIcon.TRAVEL),
                    currency = CapsSample.EUR,
                    onChange = {},
                    onSave = {},
                    onBack = {},
                    today = CapsSample.today,
                    again = true,
                )
            }
        }
        compose.onNodeWithText("The cap’s dates").assertExists()
        compose.writeScreenshot("editor-once-picker")
    }

    @Test
    fun `the numpad over the home screen on an ended one-off says so, and opens the app`() {
        var openedApp = 0
        compose.setContent {
            TettoTheme {
                QuickAddContent(
                    state = QuickAddUiState(CapsSample.today, CapsSample.EUR, CapsSample.paris()),
                    phase = QuickAddPhase.Typing(),
                    onAdd = {},
                    onUndo = {},
                    onClose = {},
                    onOpenApp = { openedApp++ },
                )
            }
        }
        compose.onNodeWithText("Paris weekend has ended").assertExists()
        compose.onNodeWithText("Open Tetto").performClick()
        assertThat(openedApp).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone`() {
        drawCaps(listOf(CapsSample.paris(), CapsSample.coffee, CapsSample.lisbon))
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-one-off-large-text", maxScreens = 20)
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `as a list at twice the text size on a small phone`() {
        val over = CapsSample.paris(CapsSample.parisLine.copy(spent = euros(350), forgiven = euros(50)))
        drawCaps(listOf(over, CapsSample.coffee, CapsSample.lisbon), CapsLayout.LIST)
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-one-off-list-large-text", maxScreens = 20)
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable`() {
        drawCaps(listOf(CapsSample.paris(), CapsSample.coffee, CapsSample.lisbon))
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-one-off-foldable")
    }
}
