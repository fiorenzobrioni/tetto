package com.callbackdev.tetto.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerTags
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.testing.assertAccessible
import com.callbackdev.tetto.core.testing.walkPage
import com.callbackdev.tetto.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The first run, drawn from its step and its picks (PLANNING.md §11 Phase 3), never from a database. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi")
class OnboardingScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private var step by mutableStateOf(OnboardingStep.WELCOME)
    private var picks by mutableStateOf(FirstRunPicks())
    private var finished = 0
    private var nothing = 0
    private var currencyPicked: String? = null

    private val euro = AppCurrency("EUR", 2)

    private fun draw(start: OnboardingStep, startPicks: FirstRunPicks = FirstRunPicks(), dark: Boolean = false) {
        step = start
        picks = startPicks
        compose.setContent {
            TettoTheme(darkTheme = dark) {
                OnboardingScreen(
                    step = step,
                    picks = picks,
                    currency = euro,
                    actions = OnboardingActions(
                        next = { OnboardingStep.entries.getOrNull(step.ordinal + 1)?.let { step = it } },
                        back = { OnboardingStep.entries.getOrNull(step.ordinal - 1)?.let { step = it } },
                        toggle = { picks = picks.toggle(it) },
                        amount = { area, amount -> picks = picks.withAmount(area, amount) },
                        kind = { area, kind -> picks = picks.withKind(area, kind) },
                        currency = { currencyPicked = it },
                        finish = { finished++ },
                        startFromNothing = { nothing++ },
                    ),
                )
            }
        }
    }

    private val twoAreas = FirstRunPicks().toggle(CapIcon.GROCERIES).toggle(CapIcon.COFFEE)

    @Test
    fun `the welcome says what Tetto is and moves on`() {
        draw(OnboardingStep.WELCOME)
        compose.onNodeWithText("How much you can still spend, and until when.").assertIsDisplayed()
        compose.onNodeWithText("Kind to the bad weeks").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-welcome")
        compose.onNodeWithTag(OnboardingTags.PRIMARY).performClick()
        assertThat(step).isEqualTo(OnboardingStep.AREAS)
    }

    @Test
    fun `the welcome says where the run stands to a screen reader, and draws no bar`() {
        draw(OnboardingStep.WELCOME)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Step 1 of 3"))
            .assertIsDisplayed()
        compose.onNodeWithContentDescription("Step 1 of 3").assertDoesNotExist()
    }

    // A phone upright as a reader holds it: 384 x 832 dp, less its status bar and gesture line,
    // which Robolectric does not draw. In Italian, the longer of the two languages.
    @Test
    @Config(qualifiers = "it-rIT-w384dp-h770dp-xxhdpi")
    fun `the welcome fits a phone without scrolling`() {
        draw(OnboardingStep.WELCOME)
        compose.waitForIdle()
        compose.writeScreenshot("onboarding-welcome-it")
        val page = compose.onNodeWithTag(OnboardingTags.PAGE).fetchSemanticsNode()
        val range = page.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        assertThat(range?.maxValue?.invoke() ?: 0f).isEqualTo(0f)
    }

    @Test
    @Config(qualifiers = "en-rGB-w384dp-h770dp-night-xxhdpi")
    fun `the welcome stands on the dark page`() {
        draw(OnboardingStep.WELCOME, dark = true)
        compose.waitForIdle()
        compose.writeScreenshot("onboarding-welcome-dark")
    }

    @Test
    fun `the areas wait for one, and pick by a touch`() {
        draw(OnboardingStep.AREAS)
        compose.onNodeWithText("What do you want to put a cap on?").assertIsDisplayed()
        compose.onNodeWithText("Bills").assertDoesNotExist()
        compose.onNodeWithTag(OnboardingTags.PRIMARY).assertIsNotEnabled()

        compose.onNodeWithTag(OnboardingTags.area(CapIcon.COFFEE)).assertIsOff().performClick()
        compose.onNodeWithTag(OnboardingTags.area(CapIcon.GROCERIES)).performClick()
        compose.onNodeWithTag(OnboardingTags.area(CapIcon.COFFEE)).assertIsOn()
        assertThat(picks.areas.map { it.area }).containsExactly(CapIcon.GROCERIES, CapIcon.COFFEE).inOrder()
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-areas")

        compose.onNodeWithTag(OnboardingTags.PRIMARY).assertIsEnabled().performClick()
        assertThat(step).isEqualTo(OnboardingStep.AMOUNTS)
    }

    @Test
    fun `start from nothing ends the run with no cap`() {
        draw(OnboardingStep.AREAS)
        compose.onNodeWithText("Start from nothing").performClick()
        assertThat(nothing).isEqualTo(1)
        assertThat(finished).isEqualTo(0)
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the areas in Italian`() {
        draw(OnboardingStep.AREAS, twoAreas)
        compose.onNodeWithText("Su cosa vuoi mettere un tetto?").assertIsDisplayed()
        compose.onNodeWithText("Parti da zero").assertIsDisplayed()
        compose.writeScreenshot("onboarding-areas-it")
    }

    @Test
    fun `the amounts wait for every amount, each with its period, a month by default`() {
        draw(OnboardingStep.AMOUNTS, twoAreas)
        compose.onNodeWithText("How much for each?").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.PRIMARY).assertIsNotEnabled()
        compose.onNodeWithText("Each cap needs an amount.").assertIsDisplayed()
        compose.onNodeWithTag(
            OnboardingTags.kind(CapIcon.GROCERIES, PeriodKind.MONTH),
        ).performScrollTo().assertIsSelected()

        compose.onNodeWithTag(OnboardingTags.amount(CapIcon.GROCERIES)).performScrollTo().performTextInput("300")
        compose.onNodeWithTag(OnboardingTags.amount(CapIcon.COFFEE)).performScrollTo().performTextInput("20.50")
        compose.onNodeWithTag(OnboardingTags.kind(CapIcon.COFFEE, PeriodKind.WEEK)).performScrollTo().performClick()
        assertThat(picks.areas).containsExactly(
            AreaPick(CapIcon.GROCERIES, Money(30_000), PeriodKind.MONTH),
            AreaPick(CapIcon.COFFEE, Money(2_050), PeriodKind.WEEK),
        ).inOrder()
        compose.onNodeWithText("Each cap needs an amount.").assertDoesNotExist()
        compose.assertAccessible()
        compose.writeScreenshot("onboarding-amounts")

        compose.onNodeWithTag(OnboardingTags.PRIMARY).assertIsEnabled().performClick()
        assertThat(finished).isEqualTo(1)
    }

    @Test
    fun `while finishing, Done waits`() {
        step = OnboardingStep.AMOUNTS
        picks = twoAreas.withAmount(CapIcon.GROCERIES, Money(1)).withAmount(CapIcon.COFFEE, Money(1))
        compose.setContent {
            TettoTheme { OnboardingScreen(step, picks, euro, OnboardingActions(), finishing = true) }
        }
        compose.onNodeWithTag(OnboardingTags.PRIMARY).assertIsNotEnabled()
    }

    @Test
    fun `the currency is the phone's region's, changed from the same page`() {
        draw(OnboardingStep.AMOUNTS, twoAreas)
        compose.onNodeWithText("Euro (EUR)").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.CURRENCY).performClick()
        compose.onNodeWithTag(CurrencyPickerTags.SEARCH).performTextInput("yen")
        compose.waitForIdle()
        compose.writeScreenshot("onboarding-currency")
        compose.onNodeWithTag(CurrencyPickerTags.currency("JPY")).performClick()
        assertThat(currencyPicked).isEqualTo("JPY")
        compose.onNodeWithTag(CurrencyPickerTags.LIST).assertDoesNotExist()
    }

    @Test
    fun `back goes to the page before, and the welcome leaves it to the system`() {
        draw(OnboardingStep.AMOUNTS, twoAreas)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertThat(step).isEqualTo(OnboardingStep.AREAS)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        assertThat(step).isEqualTo(OnboardingStep.WELCOME)
    }

    @Test
    @Config(qualifiers = "en-rGB-w411dp-h891dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, nothing is cut`() {
        draw(OnboardingStep.WELCOME)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-welcome-large-text")
        step = OnboardingStep.AREAS
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-areas-large-text")
        step = OnboardingStep.AMOUNTS
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-amounts-large-text")
    }

    @Test
    @Config(qualifiers = "en-rGB-w841dp-h701dp-xxhdpi")
    fun `on an open foldable, a centred column`() {
        draw(OnboardingStep.AREAS, twoAreas)
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-areas-foldable")
        step = OnboardingStep.AMOUNTS
        compose.walkPage(hasTestTag(OnboardingTags.PAGE), "onboarding-amounts-foldable")
    }
}
