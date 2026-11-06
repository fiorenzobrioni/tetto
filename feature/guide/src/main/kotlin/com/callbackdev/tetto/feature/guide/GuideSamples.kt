package com.callbackdev.tetto.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.CapBar
import com.callbackdev.tetto.core.designsystem.components.CapTile
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.domain.cap.CapChanges
import com.callbackdev.tetto.core.domain.cap.CapStates
import com.callbackdev.tetto.core.domain.cap.Rollover
import com.callbackdev.tetto.core.domain.cap.SpendPreview
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.Cap
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.PeriodKind
import com.callbackdev.tetto.core.model.PeriodRule
import com.callbackdev.tetto.core.model.Share
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

/**
 * The guide's examples, made by the engine itself from sample figures, so an example can never say
 * what the app would not: two weekly caps on the fourth day of the reader's week, and four weeks of
 * one of them closed by [Rollover]. Whole units of the reader's currency (a yen reader reads ¥20,
 * as the appearance's preview writes it), the days named by the reader's week.
 */
internal class GuideSample(private val currency: AppCurrency, weekStart: DayOfWeek) {
    private val unit = currency.unit.minor
    private val rule = PeriodRule(PeriodKind.WEEK, weekStart, 1)

    /** A week of the reader's, any one: only its days' names are ever said. */
    private val weekFirst: LocalDate = REFERENCE.with(TemporalAdjusters.previousOrSame(weekStart))

    /** The fourth day of the week: a little past its middle, the mark at four sevenths. */
    val today: LocalDate = weekFirst.plusDays(3)

    /** Under its cap, with a reserve: €16 left of €20 and €4 in reserve, about €4 a day. */
    val coffee: CapState = state(CapIcon.COFFEE, cap = 20, reserve = 4, spent = 8)

    /** Ahead of the days: €31 of €40 by the fourth day, its bar past the mark. */
    val eatingOut: CapState = state(CapIcon.EATING_OUT, cap = 40, reserve = 0, spent = 31)

    /** The four weeks' cap. */
    val storyCap: Money = units(STORY_CAP)

    /**
     * Four weeks of the coffee cap: €14 (six to the reserve), €23 (past the cap, three from the
     * reserve), €30 (the reserve's three used, seven forgiven), and the fourth starting whole.
     */
    val story: List<StoryWeek> = buildList {
        var reserve = Money.ZERO
        STORY_SPENT.forEachIndexed { index, spentUnits ->
            val spent = units(spentUnits)
            val outcome = Rollover.close(storyCap, reserve, spent)
            add(StoryWeek(index + 1, storyCap, spent, reserve, outcome.reserveAfter, outcome.forgiven, closed = true))
            reserve = outcome.reserveAfter
        }
        add(StoryWeek(STORY_SPENT.size + 1, storyCap, Money.ZERO, reserve, reserve, Money.ZERO, closed = false))
    }

    /** What the numpad's example has typed on the coffee cap. */
    val typed: Money = units(TYPED)

    val preview: SpendPreview = SpendPreview.of(coffee.left, typed)

    private fun units(count: Long) = Money(count * unit)

    private fun state(icon: CapIcon, cap: Long, reserve: Long, spent: Long): CapState = CapStates.of(
        cap = Cap(
            id = icon.ordinal.toLong(),
            name = "",
            icon = icon,
            amount = units(cap),
            kind = PeriodKind.WEEK,
            reserve = units(reserve),
            openStart = CapChanges.openStartForNew(rule, today),
            position = 0,
        ),
        spent = units(spent),
        rule = rule,
        today = today,
        currency = currency,
    )

    private companion object {
        val REFERENCE: LocalDate = LocalDate.of(2026, 11, 5)
        const val STORY_CAP = 20L
        val STORY_SPENT = listOf(14L, 23L, 30L)
        const val TYPED = 5L
    }
}

/** One week of the reserve's story: what it spent of the cap and its reserve, and how it closed. */
internal data class StoryWeek(
    val number: Int,
    val cap: Money,
    val spent: Money,
    val reserveBefore: Money,
    val reserveAfter: Money,
    val forgiven: Money,
    val closed: Boolean,
) {
    /** What the week could spend: its cap and the reserve it started with. */
    val base: Money get() = cap + reserveBefore
}

/** A weekday as a sentence names one: "Thursday", "giovedì". */
@Composable
@ReadOnlyComposable
internal fun weekdayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])

/**
 * Two of the caps screen's tiles, as the screen lays them: side by side on a phone, one over the
 * other at large text sizes. Pictures of tiles ([CapTile]'s `enabled = false`): nothing to touch,
 * and TalkBack reads each as the screen's sentence for it, said to be an example.
 */
@Composable
internal fun TilesSample(sample: GuideSample, format: MoneyFormat) {
    val lastDay = weekdayName(sample.coffee.lastDay.dayOfWeek)
    val tiles = listOf(
        sample.coffee to stringResource(R.string.guide_sample_coffee),
        sample.eatingOut to stringResource(R.string.guide_sample_eating_out),
    )
    if (LocalDensity.current.fontScale >= LARGE_TEXT) {
        Column(Modifier.testTag(GuideTags.TILES), verticalArrangement = Arrangement.spacedBy(TileGap)) {
            tiles.forEach { (state, name) -> SampleTile(state, name, lastDay, format, Modifier.fillMaxWidth()) }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).testTag(GuideTags.TILES),
            horizontalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            tiles.forEach { (state, name) ->
                SampleTile(state, name, lastDay, format, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun SampleTile(state: CapState, name: String, lastDay: String, format: MoneyFormat, modifier: Modifier) {
    val pace = state.pace?.let { stringResource(R.string.guide_sample_pace, format.short(it)) }
    val reserve = state.cap.reserve.takeIf { it.isPositive }
    val description = listOfNotNull(
        stringResource(
            R.string.guide_sample_tile_description,
            name,
            format.short(state.left),
            format.short(state.cap.amount),
            lastDay,
        ),
        stringResource(R.string.guide_sample_ahead).takeIf { state.aheadOfDays },
        pace,
        reserve?.let { stringResource(R.string.guide_sample_with_reserve, format.short(it)) },
    ).joinToString(", ")
    CapTile(
        icon = state.cap.icon.symbol,
        name = name,
        figure = format.short(state.left),
        figureValue = state.left.minor,
        detail = stringResource(R.string.guide_sample_detail, format.short(state.cap.amount), lastDay),
        bar = state.bar,
        mark = state.todayMark,
        over = state.over,
        ahead = state.aheadOfDays,
        description = description,
        clickLabel = "",
        longClickLabel = "",
        onClick = {},
        onLongClick = {},
        modifier = modifier,
        pace = pace,
        reserve = reserve?.let { stringResource(R.string.guide_sample_reserve, format.short(it)) },
        enabled = false,
    )
}

/**
 * The reserve's story, four weeks of one cap: each week's bar as the tile draws it (of the cap and
 * the reserve it started with; full and hatched past them), what it spent of the cap, and how it
 * closed, in the words of a cap's sheet. The fourth week is the one under way, today on its first
 * day. Each week is one sentence for TalkBack.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StorySample(sample: GuideSample, format: MoneyFormat) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().testTag(GuideTags.STORY),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            sample.story.forEach { week ->
                val label = stringResource(R.string.guide_story_week, week.number)
                val spent = stringResource(R.string.guide_story_spent, format.short(week.spent), format.short(week.cap))
                val outcome = outcomeWords(week, format)
                val description = stringResource(R.string.guide_story_description, label, spent, outcome)
                Column(
                    Modifier.clearAndSetSemantics { contentDescription = description },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        itemVerticalAlignment = Alignment.Bottom,
                    ) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Text(
                            spent,
                            style = MaterialTheme.typography.bodyMedium.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CapBar(
                        bar = Share(minOf(week.spent, week.base).minor, week.base.minor),
                        // Today's line on the week under way only: the closed weeks are whole.
                        mark = if (week.closed) null else Share(1, DAYS_IN_WEEK),
                        over = week.spent > week.base,
                        ahead = false,
                    )
                    Text(
                        outcome,
                        style = MaterialTheme.typography.labelLarge.tabular(),
                        color = if (week.reserveAfter > week.reserveBefore) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun outcomeWords(week: StoryWeek, format: MoneyFormat): String = when {
    !week.closed -> stringResource(R.string.guide_story_whole, format.short(week.base))

    week.forgiven.isPositive && week.reserveBefore.isPositive ->
        stringResource(R.string.guide_story_forgiven, format.short(week.reserveBefore), format.short(week.forgiven))

    week.forgiven.isPositive -> stringResource(R.string.guide_story_forgiven_alone, format.short(week.forgiven))

    week.reserveAfter > week.reserveBefore ->
        stringResource(R.string.guide_story_to_reserve, format.short(week.reserveAfter - week.reserveBefore))

    else -> stringResource(R.string.guide_story_from_reserve, format.short(week.reserveBefore - week.reserveAfter))
}

/**
 * The numpad's top, as the sheet draws it: the cap's badge, its name and what it has left; the
 * amount typed at the hero's weight, with its caret; under it, the preview. A picture: TalkBack
 * reads it as one sentence, said to be an example.
 */
@Composable
internal fun NumpadSample(sample: GuideSample, format: MoneyFormat) {
    val name = stringResource(R.string.guide_sample_coffee)
    val left = stringResource(R.string.guide_sample_left, format.short(sample.coffee.left))
    val preview = stringResource(R.string.guide_sample_left, format.short(sample.preview.amount))
    val typed = format.full(sample.typed)
    val description = stringResource(
        R.string.guide_numpad_description,
        typed,
        name,
        format.short(sample.coffee.left),
        preview,
    )
    val hero = TettoTheme.type.heroNumber
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(GuideTags.NUMPAD)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                CapBadge(sample.coffee.cap.icon.symbol)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        left,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    typed,
                    style = hero.copy(
                        fontSize = SampleHero,
                        lineHeight = SampleHeroLine,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = SampleHero, stepSize = 2.sp),
                    modifier = Modifier.weight(1f, fill = false),
                )
                // The caret as tall as the figure's capitals, in the reader's text size.
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .width(3.dp)
                        .height(with(LocalDensity.current) { CaretHeight.toDp() })
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
                )
            }
            Text(
                preview,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val TileGap = 12.dp
private const val LARGE_TEXT = 1.5f
private const val DAYS_IN_WEEK = 7L
private val SampleHero = 48.sp
private val SampleHeroLine = 52.sp
private val CaretHeight = 36.sp
