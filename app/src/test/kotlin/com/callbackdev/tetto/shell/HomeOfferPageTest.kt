package com.callbackdev.tetto.shell

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.data.widget.HomeCard
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
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

/** The first run's last page (PLANNING.md §8): the reader's caps as a card, and the request. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class HomeOfferPageTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 5)
    private val euro = AppCurrency("EUR", 2)
    private val pinned = mutableListOf<HomeCard>()
    private var left = 0

    private val state = HomeOfferState(
        caps = listOf(
            Triple("Coffee and bars", CapIcon.COFFEE, PeriodKind.WEEK),
            Triple("Groceries", CapIcon.GROCERIES, PeriodKind.MONTH),
            Triple("Eating out", CapIcon.EATING_OUT, PeriodKind.MONTH),
        ).mapIndexed { index, (name, icon, kind) ->
            val rule = PeriodRule(kind, DayOfWeek.MONDAY, 1)
            CapStates.of(
                Cap(
                    index + 1L,
                    name,
                    icon,
                    Money(10_000L * (index + 1)),
                    kind,
                    Money.ZERO,
                    CapChanges.openStartForNew(rule, today),
                    index,
                ),
                Money.ZERO,
                rule,
                today,
                euro,
            )
        },
        settings = UserSettings(onboardingCompleted = true),
        currency = euro,
        today = today,
    )

    private fun draw(placed: Boolean) {
        compose.setContent {
            TettoTheme {
                HomeOfferPage(state = state, placed = placed, onPin = { pinned += it }, onLeave = { left++ })
            }
        }
    }

    @Test
    fun `both cards as they will stand, at a glance first, each with its request`() {
        draw(placed = false)
        compose.onNodeWithText("Your caps on the home screen").assertExists()
        val glance = compose.onNodeWithTag(HomeOfferTags.choice(HomeCard.AT_A_GLANCE)).getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag(HomeOfferTags.choice(HomeCard.LIST)).getUnclippedBoundsInRoot()
        assertThat(glance.top).isLessThan(list.top)
        compose.onNodeWithTag(HomeOfferTags.preview(HomeCard.AT_A_GLANCE)).assertExists()
        compose.onNodeWithTag(HomeOfferTags.preview(HomeCard.LIST)).assertExists()
        compose.onNodeWithContentDescription("Add At a glance to the home screen").performClick()
        assertThat(pinned).containsExactly(HomeCard.AT_A_GLANCE)
        compose.assertAccessible()
        compose.writeScreenshot("home-offer")
    }

    @Test
    fun `the list's request, and done without a card`() {
        draw(placed = false)
        compose.onNodeWithTag(HomeOfferTags.pin(HomeCard.LIST)).performScrollTo().performClick()
        assertThat(pinned).containsExactly(HomeCard.LIST)
        compose.onNodeWithText("Done").performClick()
        assertThat(left).isEqualTo(1)
    }

    @Test
    fun `once the card is there, it says so and the wide button leaves`() {
        draw(placed = true)
        compose.onNodeWithTag(HomeOfferTags.PLACED).assertExists()
        compose.onNodeWithText("Done").performClick()
        assertThat(left).isEqualTo(1)
        assertThat(pinned).isEmpty()
    }

    @Test
    fun `before the reminder's page, it says where the run stands and continues`() {
        compose.setContent {
            TettoTheme {
                HomeOfferPage(
                    state = state,
                    placed = true,
                    onPin = { pinned += it },
                    onLeave = { left++ },
                    where = "Step 4 of 5",
                    last = false,
                )
            }
        }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 4 of 5"))
            .assertExists()
        compose.onNodeWithText("Continue").performClick()
        assertThat(left).isEqualTo(1)
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, nothing is cut`() {
        draw(placed = false)
        compose.walkPage(hasTestTag(HomeOfferTags.PAGE), "home-offer-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable, a centred column`() {
        draw(placed = false)
        compose.writeScreenshot("home-offer-foldable")
        compose.assertAccessible()
    }
}
