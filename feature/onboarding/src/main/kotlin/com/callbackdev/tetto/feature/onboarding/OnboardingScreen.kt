package com.callbackdev.tetto.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.CurrencyPickerDialog
import com.callbackdev.tetto.core.designsystem.components.MoneyField
import com.callbackdev.tetto.core.designsystem.components.ValueRow
import com.callbackdev.tetto.core.designsystem.components.currencyNamed
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.label
import com.callbackdev.tetto.core.designsystem.icons.labelRes
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapDraft
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.designsystem.R as DesignR

/**
 * The first run's pages (VISION, «First run»; PLANNING.md §11 Phase 3): what Tetto is, the areas
 * the reader puts a cap on, and their amounts. No permission is asked.
 */
enum class OnboardingStep {
    WELCOME,
    AREAS,
    AMOUNTS,
}

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val started by viewModel.currency.collectAsStateWithLifecycle()
    val finishing by viewModel.isFinishing.collectAsStateWithLifecycle()
    // Until the settings are read the run has no currency to offer: nothing rather than a guess.
    val startCurrency = started ?: return
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var picks by rememberSaveable(stateSaver = PicksSaver) { mutableStateOf(FirstRunPicks()) }
    val currency = picks.currency?.let(AppCurrency::ofOrNull) ?: startCurrency
    val resources = LocalResources.current
    OnboardingScreen(
        step = step,
        picks = picks,
        currency = currency,
        finishing = finishing,
        actions = OnboardingActions(
            next = { OnboardingStep.entries.getOrNull(step.ordinal + 1)?.let { step = it } },
            back = { OnboardingStep.entries.getOrNull(step.ordinal - 1)?.let { step = it } },
            toggle = { picks = picks.toggle(it) },
            amount = { area, amount -> picks = picks.withAmount(area, amount) },
            kind = { area, kind -> picks = picks.withKind(area, kind) },
            currency = { code -> AppCurrency.ofOrNull(code)?.let { picks = picks.withCurrency(currency, it) } },
            finish = {
                // Each cap named after its area, in the reader's language; the name is theirs to change.
                val drafts = picks.areas.map {
                    CapDraft(resources.getString(it.area.labelRes), it.amount, it.kind, it.area)
                }
                viewModel.finish(currency.code, drafts)
            },
            startFromNothing = { viewModel.finish(currency.code, emptyList()) },
        ),
    )
}

/** What the pages can ask for, as functions: the screen is a plain composable a test can draw. */
class OnboardingActions(
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val toggle: (CapIcon) -> Unit = {},
    val amount: (CapIcon, Money) -> Unit = { _, _ -> },
    val kind: (CapIcon, PeriodKind) -> Unit = { _, _ -> },
    val currency: (String) -> Unit = {},
    val finish: () -> Unit = {},
    val startFromNothing: () -> Unit = {},
)

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    picks: FirstRunPicks,
    currency: AppCurrency,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
    finishing: Boolean = false,
) {
    val reduced = reducedMotion()
    val gutter = pageGutter()
    var choosingCurrency by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = step != OnboardingStep.WELCOME, onBack = actions.back)
    Surface(modifier = modifier.fillMaxSize().testTag(OnboardingTags.ROOT)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (reduced) {
                        fadeIn(TettoMotion.fade()) togetherWith fadeOut(TettoMotion.fade())
                    } else {
                        val spec = tween<IntOffset>(PAGE_MILLIS, easing = FastOutSlowInEasing)
                        val shift = if (forward) SLIDE_DIVISOR else -SLIDE_DIVISOR
                        val enter = fadeIn(tween(PAGE_MILLIS)) + slideInHorizontally(spec) { it / shift }
                        val exit = fadeOut(tween(PAGE_MILLIS)) + slideOutHorizontally(spec) { -it / shift }
                        enter togetherWith exit
                    }
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { page ->
                // The page's words, centred in the room above the buttons; a page taller than the
                // room scrolls from its top.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val room = maxHeight
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = room)
                            .padding(gutter)
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .testTag(OnboardingTags.PAGE),
                    ) {
                        when (page) {
                            OnboardingStep.WELCOME -> WelcomePage()
                            OnboardingStep.AREAS -> AreasPage(picks, actions.toggle)
                            OnboardingStep.AMOUNTS -> AmountsPage(picks, currency, actions) { choosingCurrency = true }
                        }
                    }
                }
            }
            Box(Modifier.padding(gutter)) { BottomBar(step, picks, finishing, actions) }
        }
    }
    if (choosingCurrency) {
        CurrencyPickerDialog(
            selected = currency.code,
            onPick = { code ->
                choosingCurrency = false
                actions.currency(code)
            },
            onDismiss = { choosingCurrency = false },
        )
    }
}

private const val PAGE_MILLIS = 300
private const val SLIDE_DIVISOR = 6

/**
 * The buttons under every page (the family's frame): one wide button that moves on, in the same
 * place on every page, and the quiet way past under it. On the areas' page the wide button waits
 * for an area, and "Start from nothing" ends the run with no cap; on the amounts' page it waits
 * for every amount, and says so. Disabled, never relabelled. The way back is the system's.
 */
@Composable
private fun BottomBar(step: OnboardingStep, picks: FirstRunPicks, finishing: Boolean, actions: OnboardingActions) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val label = when (step) {
            OnboardingStep.WELCOME -> R.string.onboarding_start
            OnboardingStep.AREAS -> R.string.onboarding_next
            OnboardingStep.AMOUNTS -> R.string.onboarding_done
        }
        val enabled = when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.AREAS -> picks.areas.isNotEmpty()
            OnboardingStep.AMOUNTS -> picks.complete && !finishing
        }
        Button(
            onClick = if (step == OnboardingStep.AMOUNTS) actions.finish else actions.next,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(OnboardingTags.PRIMARY),
        ) {
            Text(stringResource(label))
        }
        if (step == OnboardingStep.AMOUNTS && !picks.complete) {
            Text(
                text = stringResource(R.string.onboarding_amounts_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (step == OnboardingStep.AREAS) {
            TextButton(
                onClick = actions.startFromNothing,
                enabled = !finishing,
                modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTags.NOTHING),
            ) {
                Text(stringResource(R.string.onboarding_nothing))
            }
        }
    }
}

/** A page's heading, which a screen reader announces with where the run stands. */
@Composable
private fun Modifier.stepHeading(step: OnboardingStep): Modifier {
    val where = stringResource(R.string.onboarding_progress, step.ordinal + 1, OnboardingStep.entries.size)
    return semantics {
        heading()
        stateDescription = where
    }
}

@Composable
private fun PageTitle(text: String, step: OnboardingStep) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(step),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun PageBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * The name and its sentence, then the three things Tetto promises, in words only: no picture (the
 * reader has just touched the icon to get here) and no badges round the promises (the family's
 * onboarding frame).
 */
@Composable
private fun WelcomePage() {
    Text(
        text = stringResource(R.string.onboarding_app_name),
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(OnboardingStep.WELCOME),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_tagline),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(36.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Promise(R.string.onboarding_left_title, R.string.onboarding_left_body)
        Promise(R.string.onboarding_kind_title, R.string.onboarding_kind_body)
        Promise(R.string.onboarding_private_title, R.string.onboarding_private_body)
    }
}

@Composable
private fun Promise(title: Int, body: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "What do you want to put a cap on?": the twelve areas, the reader's own picked. */
@Composable
private fun AreasPage(picks: FirstRunPicks, toggle: (CapIcon) -> Unit) {
    PageTitle(stringResource(R.string.onboarding_areas_title), OnboardingStep.AREAS)
    PageBody(stringResource(R.string.onboarding_areas_body))
    Spacer(Modifier.height(24.dp))
    // Three to a row on a phone, four on an open foldable, two at large text sizes: each area
    // keeps the room its two lines of name need.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val columns = (maxWidth / (AREA_WIDTH * fontScale)).toInt().coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        Column(verticalArrangement = Arrangement.spacedBy(AREA_GAP)) {
            FirstRunAreas.chunked(columns).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(AREA_GAP),
                ) {
                    row.forEach { area ->
                        AreaChoice(area, picks.isPicked(area), { toggle(area) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private val AREA_WIDTH = 104.dp
private val AREA_GAP = 10.dp
private const val MIN_COLUMNS = 2
private const val MAX_COLUMNS = 4

/**
 * One area, a choice among the others: its icon and its name; picked, it takes the accent's ground,
 * a firmer edge and a tick, so the choice is a shape as well as a colour. TalkBack says it as a
 * checkbox.
 */
@Composable
private fun AreaChoice(area: CapIcon, picked: Boolean, onToggle: () -> Unit, modifier: Modifier) {
    Surface(
        shape = GroupShape,
        color = if (picked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (picked) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = modifier
            .clip(GroupShape)
            .toggleable(value = picked, role = Role.Checkbox, onValueChange = { onToggle() })
            .testTag(OnboardingTags.area(area)),
    ) {
        Box {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 16.dp),
            ) {
                val colors = MaterialTheme.colorScheme
                val (badge, ink) = if (picked) {
                    colors.primary to colors.onPrimary
                } else {
                    colors.primaryContainer to colors.onPrimaryContainer
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(badge),
                ) {
                    Icon(area.symbol, contentDescription = null, tint = ink, modifier = Modifier.size(24.dp))
                }
                Text(area.label(), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
            if (picked) {
                Icon(
                    TettoIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(18.dp),
                )
            }
        }
    }
}

/**
 * The amounts: the currency first (the phone's region's, changeable here), then one group per area
 * picked, each its amount and its period, a month by default.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountsPage(
    picks: FirstRunPicks,
    currency: AppCurrency,
    actions: OnboardingActions,
    onChangeCurrency: () -> Unit,
) {
    val format = rememberMoneyFormat(currency)
    PageTitle(stringResource(R.string.onboarding_amounts_title), OnboardingStep.AMOUNTS)
    PageBody(stringResource(R.string.onboarding_amounts_body))
    Spacer(Modifier.height(24.dp))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ValueRow(
            label = stringResource(DesignR.string.currency_title),
            value = currencyNamed(currency.code),
            onClick = onChangeCurrency,
            modifier = Modifier.testTag(OnboardingTags.CURRENCY),
        )
    }
    picks.areas.forEach { pick ->
        key(pick.area) {
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = GroupShape,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CapBadge(pick.area.symbol)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            pick.area.label(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    MoneyField(
                        amount = pick.amount,
                        format = format,
                        label = stringResource(R.string.onboarding_amount),
                        onAmount = { actions.amount(pick.area, it) },
                        modifier = Modifier.fillMaxWidth().testTag(OnboardingTags.amount(pick.area)),
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PeriodKind.entries.forEach { kind ->
                            FilterChip(
                                selected = pick.kind == kind,
                                onClick = { actions.kind(pick.area, kind) },
                                label = { Text(kind.label()) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag(OnboardingTags.kind(pick.area, kind)),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The picks survive a rotation and the process's death: the currency, then each area's three fields. */
private val PicksSaver = listSaver<FirstRunPicks, String>(
    save = { picks ->
        listOf(picks.currency.orEmpty()) +
            picks.areas.flatMap { listOf(it.area.key, it.amount.minor.toString(), it.kind.name) }
    },
    restore = { saved ->
        FirstRunPicks(
            areas = saved.drop(1).chunked(AREA_FIELDS).map { (key, minor, kind) ->
                AreaPick(CapIcon.fromKey(key), Money(minor.toLong()), PeriodKind.valueOf(kind))
            },
            currency = saved.first().ifEmpty { null },
        )
    },
)

private const val AREA_FIELDS = 3

/** Hooks for the UI tests. */
object OnboardingTags {
    const val ROOT = "onboarding_root"
    const val PAGE = "onboarding_page"
    const val PRIMARY = "onboarding_primary"
    const val NOTHING = "onboarding_nothing"
    const val CURRENCY = "onboarding_currency"

    fun area(area: CapIcon) = "onboarding_area_${area.key}"

    fun amount(area: CapIcon) = "onboarding_amount_${area.key}"

    fun kind(area: CapIcon, kind: PeriodKind) = "onboarding_kind_${area.key}_${kind.name.lowercase()}"
}
