package com.callbackdev.tetto.feature.caps

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.Renewal
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

/** The caps screen from its states (PLANNING.md §11 Phase 2), never from a database. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class CapsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val added = mutableListOf<Long>()
    private val opened = mutableListOf<Long>()
    private var newCap = 0
    private var order = 0
    private var settings = 0
    private var guide = 0
    private var tipDismissed = 0

    private fun draw(state: CapsUiState, dark: Boolean = false) {
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                CapsScreen(
                    state = state,
                    onAddSpend = { added += it },
                    onOpenCap = { opened += it },
                    onNewCap = { newCap++ },
                    onChangeOrder = { order++ },
                    onOpenSettings = { settings++ },
                    onOpenGuide = { guide++ },
                    onDismissTip = { tipDismissed++ },
                )
            }
        }
    }

    private fun sentenceHeight(part: String): Float {
        val node = compose.onNodeWithText(part, substring = true).fetchSemanticsNode()
        return node.boundsInRoot.height / node.layoutInfo.density.density
    }

    private fun description(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()

    @Test
    fun `with no cap, says so, and offers the first one`() {
        draw(CapsSample.ui(emptyList()))
        compose.onNodeWithText("No caps yet").assertIsDisplayed()
        compose.onNodeWithText("New cap").performClick()
        assertThat(newCap).isEqualTo(1)
        compose.onNodeWithText("Change the order").assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("caps-empty")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `with no cap, in Italian`() {
        draw(CapsSample.ui(emptyList()))
        compose.onNodeWithText("Ancora nessun tetto").assertIsDisplayed()
        compose.onNodeWithText("Nuovo tetto").assertIsDisplayed()
        compose.writeScreenshot("caps-empty-it")
    }

    @Test
    fun `all under, the sentence names the closest, before any number`() {
        draw(CapsSample.ui(CapsSample.underCaps))
        compose.onNodeWithText(
            "All under your caps. Restaurants is the closest: €12 left until 30 Nov.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Thursday 5 November", substring = true).assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("caps")
    }

    @Test
    fun `with nothing spent on any cap, the sentence names none`() {
        val fuel = CapsSample.state(8, "Fuel", CapIcon.FUEL, euros(200), PeriodKind.MONTH, Money.ZERO)
        val coffee = CapsSample.state(9, "Coffee", CapIcon.COFFEE, euros(100), PeriodKind.MONTH, Money.ZERO)
        draw(CapsSample.ui(listOf(fuel, coffee)))
        compose.onNodeWithText("All under your caps.").assertIsDisplayed()
    }

    @Test
    fun `a tile reads as one sentence, and its touches are named`() {
        draw(CapsSample.ui(CapsSample.underCaps))
        assertThat(description(CapsTags.tile(1)))
            .isEqualTo("Coffee, €16 left of €20, until Sunday, about €4 a day, with €4 in reserve")
        assertThat(description(CapsTags.tile(5))).isEqualTo("Lunch, €7.50 left of €12, today")
        val tile = compose.onNodeWithTag(CapsTags.tile(1))
        tile.assert(SemanticsMatcher("named touch") { it.config[SemanticsActions.OnClick].label == "Add a spend" })
        tile.assert(
            SemanticsMatcher("named long press") {
                it.config[SemanticsActions.OnLongClick].label ==
                    "Open the cap"
            },
        )
        tile.performClick()
        tile.performSemanticsAction(SemanticsActions.OnLongClick)
        assertThat(added).containsExactly(1L)
        assertThat(opened).containsExactly(1L)
    }

    @Test
    fun `on the first day, a tip over the tiles says how to read one, and leads to the guide`() {
        draw(CapsSample.ui(CapsSample.underCaps).copy(tipSeen = false))
        compose.onNodeWithText("How to read a cap").assertIsDisplayed()
        compose.onNodeWithText("The thin line across the bar is today", substring = true).assertIsDisplayed()
        // Over the tiles: the first tile starts below the tip.
        val tip = compose.onNodeWithTag(CapsTags.TIP).fetchSemanticsNode().boundsInRoot
        val tile = compose.onNodeWithTag(CapsTags.tile(1)).fetchSemanticsNode().boundsInRoot
        assertThat(tile.top).isGreaterThan(tip.bottom)
        compose.assertAccessible()
        compose.writeScreenshot("caps-tip")

        compose.onNodeWithText("Open the guide").performClick()
        assertThat(guide).isEqualTo(1)
        compose.onNodeWithText("Got it").performClick()
        assertThat(tipDismissed).isEqualTo(1)
    }

    @Test
    fun `once seen, the tip is gone for good, and an empty page never shows it`() {
        var state by mutableStateOf(CapsSample.ui(CapsSample.underCaps).copy(tipSeen = false))
        compose.setContent {
            TettoTheme {
                CapsScreen(
                    state = state,
                    onAddSpend = {},
                    onOpenCap = {},
                    onNewCap = {},
                    onChangeOrder = {},
                    onOpenSettings = {},
                    onDismissTip = { state = state.copy(tipSeen = true) },
                )
            }
        }
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithTag(CapsTags.TIP).assertDoesNotExist()

        state = CapsSample.ui(emptyList()).copy(tipSeen = false)
        compose.onNodeWithText("No caps yet").assertIsDisplayed()
        compose.onNodeWithTag(CapsTags.TIP).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the tip, in Italian`() {
        draw(CapsSample.ui(CapsSample.underCaps).copy(tipSeen = false))
        compose.onNodeWithText("Come leggere un tetto").assertIsDisplayed()
        compose.onNodeWithText("Apri la guida").assertIsDisplayed()
        compose.onNodeWithText("Ho capito").assertIsDisplayed()
        compose.writeScreenshot("caps-tip-it")
    }

    @Test
    @Config(qualifiers = "en-rGB-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `the tip at twice the text size on a small phone`() {
        draw(CapsSample.ui(CapsSample.underCaps).copy(tipSeen = false))
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-tip-large-text", maxScreens = 20)
    }

    @Test
    fun `from two caps, the order can be changed`() {
        draw(CapsSample.ui(listOf(CapsSample.coffee, CapsSample.groceries)))
        compose.onNodeWithText("Change the order").performClick()
        assertThat(order).isEqualTo(1)
    }

    @Test
    fun `the gear, beside today, opens Settings`() {
        draw(CapsSample.ui(emptyList()))
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()
        assertThat(settings).isEqualTo(1)
    }

    @Test
    fun `over is said in words, with when the cap starts again`() {
        draw(CapsSample.ui(listOf(CapsSample.coffee, CapsSample.shopping, CapsSample.groceries)))
        compose.onNodeWithText("Shopping is €30 over. It starts again on 1 Dec.").assertIsDisplayed()
        assertThat(description(CapsTags.tile(4))).isEqualTo("Shopping, €30 over its cap of €100, starts again on 1 Dec")
        compose.writeScreenshot("caps-over")
    }

    @Test
    fun `every cap over`() {
        val over = CapsSample.state(6, "Fun", CapIcon.FUN, euros(50), PeriodKind.WEEK, euros(55))
        draw(CapsSample.ui(listOf(CapsSample.shopping, over)))
        compose.onNodeWithText("Every cap is over. Shopping starts again on 1 Dec.").assertIsDisplayed()
    }

    @Test
    fun `a cap renewed today says so, when none is over`() {
        val renewed = CapsSample.state(
            7,
            "Books",
            CapIcon.BOOKS,
            euros(30),
            PeriodKind.MONTH,
            Money.ZERO,
        ).let { it.copy(renewedToday = true) }
        draw(CapsSample.ui(listOf(CapsSample.coffee, renewed)))
        compose.onNodeWithText("Books starts again today: €30 until 30 Nov.").assertIsDisplayed()
    }

    @Test
    fun `the day a cap renews, it says how the last period ended, forgiveness first`() {
        fun renewed(renewal: Renewal) = CapsSample.ui(
            listOf(
                CapsSample.coffee,
                CapsSample.state(7, "Books", CapIcon.BOOKS, euros(30), PeriodKind.MONTH, Money.ZERO)
                    .copy(renewedToday = true, renewal = renewal),
            ),
        )
        val start = "Books starts again today: €30 until 30 Nov"
        val said = listOf(
            Renewal(Renewal.Kind.FORGIVEN, euros(12)) to "$start. It was €12 over: forgiven, a clean start.",
            Renewal(Renewal.Kind.COVERED, euros(5)) to "$start. The reserve covered €5 over.",
            Renewal(Renewal.Kind.SAVED, euros(4)) to "$start, with €4 more in reserve.",
            Renewal(Renewal.Kind.RESERVE_FULL, euros(30)) to "$start, with its reserve full.",
            Renewal(Renewal.Kind.EVEN, euros(30)) to "$start.",
        )
        var shown by mutableStateOf(renewed(said.first().first))
        compose.setContent {
            TettoTheme {
                CapsScreen(
                    state = shown,
                    onAddSpend = {},
                    onOpenCap = {},
                    onNewCap = {},
                    onChangeOrder = {},
                    onOpenSettings = {},
                    onOpenGuide = {},
                    onDismissTip = {},
                )
            }
        }
        for ((renewal, words) in said) {
            shown = renewed(renewal)
            compose.waitForIdle()
            // The sentence keeps a date's words together with a no-break space: read it plain.
            val text = compose.onNodeWithText("Books starts again today", substring = true)
                .fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
            assertThat(text.replace('\u00A0', ' ')).isEqualTo(words)
            if (renewal.kind == Renewal.Kind.FORGIVEN) compose.writeScreenshot("caps-renewed-forgiven")
        }
    }

    @Test
    fun `dark`() {
        draw(
            CapsSample.ui(listOf(CapsSample.coffee, CapsSample.shopping, CapsSample.groceries, CapsSample.lunch)),
            dark = true,
        )
        compose.assertAccessible()
        compose.writeScreenshot("caps-dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        draw(CapsSample.ui(CapsSample.underCaps))
        // Italian writes the euro after the figure, past a no-break space.
        compose.onNodeWithText("Tutti sotto il tetto. Il più vicino è Restaurants: ancora 12 € fino a lunedì 30 nov.")
            .assertIsDisplayed()
        assertThat(description(CapsTags.tile(1))).isEqualTo(
            "Coffee, ancora 16 € su 20 €, fino a domenica, circa 4 € al giorno, con 4 € di riserva",
        )
        compose.writeScreenshot("caps-it")
    }

    @Test
    @Config(qualifiers = "it-rIT-w393dp-h851dp-xxhdpi")
    fun `the sentence steps down to two lines where that is enough`() {
        val fuel = CapsSample.state(8, "Benzina", CapIcon.FUEL, euros(250), PeriodKind.MONTH, euros(50))
        val coffee = CapsSample.state(9, "Caffè e Bar", CapIcon.COFFEE, euros(100), PeriodKind.MONTH, euros(10))
        draw(CapsSample.ui(listOf(fuel, coffee)))
        // Three lines at the headline's 24 sp; two at 20 sp, its smallest step, 26.7 dp each.
        assertThat(sentenceHeight("Il più vicino è Benzina")).isLessThan(60f)
    }

    @Test
    @Config(qualifiers = "it-rIT-w393dp-h851dp-xxhdpi")
    fun `a sentence two lines cannot hold keeps the headline's size`() {
        draw(CapsSample.ui(CapsSample.underCaps))
        // "Restaurants" would need 18 sp for two lines: three lines, then, at 24 sp, never shrunk for nothing.
        assertThat(sentenceHeight("Il più vicino è Restaurants")).isWithin(1f).of(96f)
    }

    @Test
    fun `your month, last on the page, in one sentence for TalkBack`() {
        draw(CapsSample.ui(CapsSample.underCaps, month = CapsSample.month()))
        compose.onNodeWithTag(CapsTags.PAGE).performScrollToNode(hasTestTag(CapsTags.MONTH))
        compose.onNodeWithTag(CapsTags.MONTH).assertIsDisplayed()
        val words = description(CapsTags.MONTH)
        assertThat(words).startsWith("Your month, 1")
        assertThat(words).endsWith(
            "Nov. €842 added to your caps so far. Your caps are worth about €1,250 a month. Last month: €1,130.",
        )
        compose.assertAccessible()
        compose.writeScreenshot("caps-month")
    }

    @Test
    @Config(qualifiers = "it-rIT-w393dp-h851dp-xxhdpi")
    fun `your month, in Italian, with nothing added yet and no month before`() {
        draw(CapsSample.ui(CapsSample.underCaps, month = CapsSample.month(Money.ZERO, Money.ZERO)))
        compose.onNodeWithTag(CapsTags.PAGE).performScrollToNode(hasTestTag(CapsTags.MONTH))
        val words = description(CapsTags.MONTH)
        assertThat(words).contains("Ancora nessuna spesa aggiunta ai tetti in questo mese.")
        assertThat(words).contains("I tuoi tetti valgono circa 1.250")
        assertThat(words).doesNotContain("mese scorso")
        compose.writeScreenshot("caps-month-it")
    }

    @Test
    fun `your month under the list too, the same card`() {
        draw(CapsSample.ui(CapsSample.underCaps, layout = CapsLayout.LIST, month = CapsSample.month()))
        compose.onNodeWithTag(CapsTags.PAGE).performScrollToNode(hasTestTag(CapsTags.MONTH))
        compose.onNodeWithTag(CapsTags.MONTH).assertIsDisplayed()
        val last = compose.onNodeWithTag(CapsTags.tile(5)).fetchSemanticsNode().boundsInRoot
        assertThat(compose.onNodeWithTag(CapsTags.MONTH).fetchSemanticsNode().boundsInRoot.top)
            .isGreaterThan(last.bottom)
        compose.writeScreenshot("caps-list-month")
    }

    @Test
    fun `no month drawn without caps`() {
        draw(CapsSample.ui(emptyList(), month = CapsSample.month()))
        compose.onNodeWithTag(CapsTags.MONTH).assertDoesNotExist()
    }

    @Test
    fun `as a list, a row per cap, shorter than a tile, with the same words and touches`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping, layout = CapsLayout.LIST))
        val first = compose.onNodeWithTag(CapsTags.tile(1)).fetchSemanticsNode()
        val second = compose.onNodeWithTag(CapsTags.tile(2)).fetchSemanticsNode().boundsInRoot
        val density = first.layoutInfo.density.density
        assertThat(second.left).isEqualTo(first.boundsInRoot.left)
        assertThat(second.top).isGreaterThan(first.boundsInRoot.bottom)
        assertThat(first.boundsInRoot.height / density).isLessThan(100f)
        assertThat(description(CapsTags.tile(1)))
            .isEqualTo("Coffee, €16 left of €20, until Sunday, about €4 a day, with €4 in reserve")
        compose.onNodeWithTag(CapsTags.tile(1)).performClick()
        compose.onNodeWithTag(CapsTags.tile(1)).performSemanticsAction(SemanticsActions.OnLongClick)
        assertThat(added).containsExactly(1L)
        assertThat(opened).containsExactly(1L)
        compose.onNodeWithText("New cap").performClick()
        assertThat(newCap).isEqualTo(1)
        compose.assertAccessible()
        compose.writeScreenshot("caps-list")
    }

    @Test
    @Config(qualifiers = "it-rIT-w393dp-h851dp-xxhdpi")
    fun `as a list, in Italian`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping, layout = CapsLayout.LIST))
        compose.onNodeWithText("Nuovo tetto").assertIsDisplayed()
        compose.writeScreenshot("caps-list-it")
    }

    @Test
    fun `as a list, dark`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping, layout = CapsLayout.LIST), dark = true)
        compose.assertAccessible()
        compose.writeScreenshot("caps-list-dark")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `as a list at twice the text size, nothing is cut`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping, layout = CapsLayout.LIST))
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-list-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `as a list on an open foldable, a centred column`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping, layout = CapsLayout.LIST))
        val page = compose.onNodeWithTag(CapsTags.PAGE).fetchSemanticsNode().boundsInRoot
        val first = compose.onNodeWithTag(CapsTags.tile(1)).fetchSemanticsNode().boundsInRoot
        assertThat((page.right - first.right)).isWithin(4f).of(first.left - page.left)
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-list-foldable")
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, the tiles stand in one column and nothing is cut`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping))
        val first = compose.onNodeWithTag(CapsTags.tile(1)).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag(CapsTags.tile(2)).fetchSemanticsNode().boundsInRoot
        assertThat(second.left).isEqualTo(first.left)
        assertThat(second.top).isGreaterThan(first.bottom)
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable, a centred column in two`() {
        draw(CapsSample.ui(CapsSample.underCaps + CapsSample.shopping))
        val page = compose.onNodeWithTag(CapsTags.PAGE).fetchSemanticsNode().boundsInRoot
        val first = compose.onNodeWithTag(CapsTags.tile(1)).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag(CapsTags.tile(2)).fetchSemanticsNode().boundsInRoot
        assertThat(second.top).isEqualTo(first.top)
        // The column is the family's page width, centred: as far from the right as from the left.
        val density = compose.onNodeWithTag(CapsTags.PAGE).fetchSemanticsNode().layoutInfo.density.density
        assertThat(first.left / density).isGreaterThan(100.dp.value)
        assertThat((page.right - second.right) / density).isWithin(2f).of(first.left / density)
        compose.walkPage(hasTestTag(CapsTags.PAGE), "caps-foldable")
    }
}
