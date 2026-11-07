package com.callbackdev.tetto.feature.caps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.components.CapRow
import com.callbackdev.tetto.core.designsystem.components.CapTile
import com.callbackdev.tetto.core.designsystem.components.EndedCapRow
import com.callbackdev.tetto.core.designsystem.components.EndedCapTile
import com.callbackdev.tetto.core.designsystem.components.FewLinesAutoSize
import com.callbackdev.tetto.core.designsystem.components.NewCapRow
import com.callbackdev.tetto.core.designsystem.components.NewCapTile
import com.callbackdev.tetto.core.designsystem.components.StatusCard
import com.callbackdev.tetto.core.designsystem.components.StatusTone
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.domain.cap.YourMonth
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.CapsLayout
import com.callbackdev.tetto.core.model.Money
import java.time.LocalDate

/**
 * The caps screen (VISION, «Caps»; PLANNING.md §11 Phase 2), drawn from its state: today, the
 * sentence before any number, a tile per cap in the reader's order (two columns on a phone, one at
 * large text sizes) or, as the reader chooses in Settings, a row per cap ([CapsLayout.LIST]),
 * "New cap" last, and "Change the order" under them from two caps up. A touch on a cap opens the
 * numpad on it, a long press its sheet. On the first day, the tip over the caps says how to read
 * one, and leads to the guide (Phase 6).
 */
@Composable
fun CapsScreen(
    state: CapsUiState,
    onAddSpend: (Long) -> Unit,
    onOpenCap: (Long) -> Unit,
    onNewCap: () -> Unit,
    onChangeOrder: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenGuide: () -> Unit = {},
    onDismissTip: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val format = rememberMoneyFormat(state.currency)
    val reduced = reducedMotion()
    Column(
        modifier
            .fillMaxSize()
            .testTag(CapsTags.PAGE)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical))
            .padding(contentPadding)
            .padding(pageGutter())
            .padding(horizontal = ScreenMargin, vertical = 24.dp),
    ) {
        // Today, and the gear at the row's end: the family's first row. The gear's 48dp touch
        // overhangs the margin, so its drawing lines up with the tiles' edge.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                headingDate(state.today),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.offset(x = 12.dp).testTag(CapsTags.SETTINGS),
            ) {
                Icon(TettoIcons.Settings, contentDescription = stringResource(R.string.open_settings))
            }
        }
        Spacer(Modifier.height(4.dp))
        val sentence = sentenceWords(state.sentence, format, state.today)
        Sentence(sentence ?: stringResource(R.string.caps_empty_title))
        if (state.caps.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.caps_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Gone with a fade and the space closing, so the tiles rise to where it was: the reader sees
        // where the tip went.
        AnimatedVisibility(
            visible = state.showsTip,
            enter = TettoMotion.enter(reduced),
            exit = TettoMotion.exit(reduced),
        ) {
            Column {
                Spacer(Modifier.height(20.dp))
                TipCard(onOpenGuide, onDismissTip)
            }
        }
        Spacer(Modifier.height(24.dp))
        when (state.layout) {
            CapsLayout.TILES -> TileGrid(state, format, onAddSpend, onOpenCap, onNewCap)
            CapsLayout.LIST -> RowList(state, format, onAddSpend, onOpenCap, onNewCap)
        }
        if (state.caps.size >= 2) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onChangeOrder) { Text(stringResource(R.string.change_order)) }
            }
        }
        val month = state.month
        if (month != null && state.caps.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            MonthCard(month, format, state.today)
        }
    }
}

/**
 * The reader's month, last on the page (owner, 6 Nov 2026): what was added to the caps in it so
 * far, what the caps are worth in a month, and what was added in the month before, to set the
 * caps against a salary. Under the caps, so the page still leads with what is left; quieter than
 * a tile, and one sentence for TalkBack. The month before is not drawn where nothing was added.
 */
@Composable
private fun MonthCard(month: YourMonth, format: MoneyFormat, today: LocalDate) {
    val range = rangeWords(month.month, today)
    val added = format.short(month.added)
    val worth = stringResource(R.string.month_worth, format.short(month.worth))
    val last = month.addedLastMonth.takeIf { it.isPositive }
        ?.let { stringResource(R.string.month_last, format.short(it)) }
    val addedWords = if (month.added.isPositive) {
        stringResource(R.string.month_added_description, added)
    } else {
        stringResource(R.string.month_added_none)
    }
    val description = listOfNotNull(stringResource(R.string.month_description, range), addedWords, worth, last)
        .joinToString(" ")
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CapsTags.MONTH)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.month_title, range),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (month.added.isPositive) {
                Row {
                    Text(
                        added,
                        style = TettoTheme.type.readingValue,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.month_added),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            } else {
                Text(stringResource(R.string.month_added_none), style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                worth,
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (last != null) {
                Text(
                    last,
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The sentence on top, the page's heading: two lines where a step down from the headline's size
 * to [SENTENCE_MIN] makes it fit, so the caps start higher; at the headline's size, on as many
 * lines as it takes, where it does not (a long cap's name, a large text size), never cut.
 */
@Composable
private fun Sentence(text: String) {
    val headline = MaterialTheme.typography.headlineSmall
    BasicText(
        text,
        style = headline.copy(
            color = LocalContentColor.current,
            // The headline's leading as a share of its size, so a smaller step keeps its rhythm.
            lineHeight = (headline.lineHeight.value / headline.fontSize.value).em,
        ),
        autoSize = FewLinesAutoSize(max = headline.fontSize, min = SENTENCE_MIN, lines = 2),
        modifier = Modifier.semantics { heading() },
    )
}

/**
 * The first day's tip (PLANNING.md §11 Phase 6): what a tile's figure and its bar say, and the way
 * to the guide for the rest. Shown until it is dismissed or followed ([CapsUiState.showsTip]); the
 * guide stays first in Settings for the day the question comes back.
 */
@Composable
private fun TipCard(onOpenGuide: () -> Unit, onDismiss: () -> Unit) {
    StatusCard(
        icon = TettoIcons.Roof,
        title = stringResource(R.string.tip_title),
        body = stringResource(R.string.tip_body),
        tone = StatusTone.NOTE,
        action = stringResource(R.string.tip_open),
        onAction = onOpenGuide,
        dismiss = stringResource(R.string.tip_dismiss),
        onDismiss = onDismiss,
        modifier = Modifier.testTag(CapsTags.TIP),
    )
}

/**
 * The tiles in rows, each row as tall as its tallest tile, so a row ends together; not a lazy
 * grid, which would leave a short tile hanging: the caps are a screenful, not a list.
 */
@Composable
private fun TileGrid(
    state: CapsUiState,
    format: MoneyFormat,
    onAddSpend: (Long) -> Unit,
    onOpenCap: (Long) -> Unit,
    onNewCap: () -> Unit,
) {
    val columns = tileColumns()
    val cells: List<CapState?> = state.shown + null
    Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
        cells.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TileGap),
            ) {
                row.forEach { cell ->
                    val cellModifier = Modifier.weight(1f).fillMaxHeight()
                    when {
                        cell == null -> NewCapTile(
                            stringResource(R.string.new_cap),
                            TettoIcons.Plus,
                            onNewCap,
                            cellModifier,
                        )

                        cell.ended -> EndedTile(cell, format, state.today, onOpenCap, cellModifier)

                        else -> Tile(cell, format, state.today, onAddSpend, onOpenCap, cellModifier)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * A row per cap across the page, closer together than the tiles (owner, 6 Nov 2026): for a reader
 * with many caps, more of them on a screen.
 */
@Composable
private fun RowList(
    state: CapsUiState,
    format: MoneyFormat,
    onAddSpend: (Long) -> Unit,
    onOpenCap: (Long) -> Unit,
    onNewCap: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
        state.shown.forEach { cap ->
            val id = cap.cap.id
            // Keyed by the cap, so a figure rolls from its own last value, never a neighbour's.
            key(id) {
                if (cap.ended) {
                    val (ended, detail) = endedLines(cap, format, state.today)
                    EndedCapRow(
                        icon = cap.cap.icon.symbol,
                        name = cap.cap.name,
                        ended = ended,
                        detail = detail,
                        bar = cap.bar,
                        over = cap.over,
                        description = endedDescription(cap, format, state.today),
                        clickLabel = stringResource(R.string.tile_long_click),
                        onClick = { onOpenCap(id) },
                        modifier = Modifier.fillMaxWidth().testTag(CapsTags.tile(id)),
                    )
                } else {
                    ListRow(cap, id, format, state.today, onAddSpend, onOpenCap)
                }
            }
        }
        NewCapRow(stringResource(R.string.new_cap), TettoIcons.Plus, onNewCap, Modifier.fillMaxWidth())
    }
}

@Composable
private fun ListRow(
    cap: CapState,
    id: Long,
    format: MoneyFormat,
    today: LocalDate,
    onAddSpend: (Long) -> Unit,
    onOpenCap: (Long) -> Unit,
) {
    CapRow(
        icon = cap.cap.icon.symbol,
        name = cap.cap.name,
        figure = figureWords(cap, format),
        figureValue = cap.left.minor,
        detail = detailWords(cap, format, today),
        bar = cap.bar,
        mark = cap.todayMark,
        over = cap.over,
        ahead = cap.aheadOfDays,
        aheadWords = aheadWords(cap),
        description = tileDescription(cap, format, today),
        clickLabel = stringResource(R.string.tile_click),
        longClickLabel = stringResource(R.string.tile_long_click),
        onClick = { onAddSpend(id) },
        onLongClick = { onOpenCap(id) },
        modifier = Modifier.fillMaxWidth().testTag(CapsTags.tile(id)),
    )
}

@Composable
private fun Tile(
    state: CapState,
    format: MoneyFormat,
    today: LocalDate,
    onAddSpend: (Long) -> Unit,
    onOpenCap: (Long) -> Unit,
    modifier: Modifier,
) {
    val id = state.cap.id
    val (of, until) = tileLines(state, format, today)
    CapTile(
        icon = state.cap.icon.symbol,
        name = state.cap.name,
        figure = figureWords(state, format),
        figureValue = state.left.minor,
        detail = of,
        dateLine = until,
        bar = state.bar,
        mark = state.todayMark,
        over = state.over,
        ahead = state.aheadOfDays,
        aheadWords = aheadWords(state),
        description = tileDescription(state, format, today),
        clickLabel = stringResource(R.string.tile_click),
        longClickLabel = stringResource(R.string.tile_long_click),
        onClick = { onAddSpend(id) },
        onLongClick = { onOpenCap(id) },
        pace = paceWords(state, format),
        reserve = reserveWords(state, format),
        modifier = modifier.testTag(CapsTags.tile(id)),
    )
}

/**
 * An ended one-off's tile (owner, 6 Nov 2026), muted, last: when it ended and what its dates spent.
 * A touch opens its sheet, where it can run again with new dates: no numpad, its dates are over.
 */
@Composable
private fun EndedTile(
    state: CapState,
    format: MoneyFormat,
    today: LocalDate,
    onOpenCap: (Long) -> Unit,
    modifier: Modifier,
) {
    val id = state.cap.id
    val (ended, detail) = endedLines(state, format, today)
    EndedCapTile(
        icon = state.cap.icon.symbol,
        name = state.cap.name,
        ended = ended,
        detail = detail,
        bar = state.bar,
        over = state.over,
        description = endedDescription(state, format, today),
        clickLabel = stringResource(R.string.tile_long_click),
        onClick = { onOpenCap(id) },
        modifier = modifier.testTag(CapsTags.tile(id)),
    )
}

/**
 * Two columns on a phone, one at large text sizes (VISION, «Caps»): from one and a half times the
 * text, or where the page, measured in the reader's text, is narrow.
 */
@Composable
private fun tileColumns(): Int {
    val fontScale = LocalDensity.current.fontScale
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return if (fontScale >= LARGE_TEXT || width.value / fontScale < NARROW_DP) 1 else 2
}

private val TileGap = 12.dp
private val RowGap = 8.dp

/** The smallest the sentence steps down to for two lines: still the page's largest words. */
private val SENTENCE_MIN = 20.sp
private const val LARGE_TEXT = 1.5f
private const val NARROW_DP = 300f

/** What the tests find the page and its tiles by. */
object CapsTags {
    const val PAGE = "caps_page"
    const val SETTINGS = "caps_settings"
    const val TIP = "caps_tip"
    const val MONTH = "caps_month"
    const val NUMPAD = "numpad"
    const val AMOUNT = "numpad_amount"
    const val PREVIEW = "numpad_preview"

    /** The cap's habits above the numpad's keys. */
    const val AMOUNTS = "numpad_amounts"

    /** The amount and its preview together, as TalkBack hears them. */
    const val READOUT = "numpad_readout"
    const val CONFIRM = "numpad_confirm"
    const val SHEET = "cap_sheet"
    const val EDITOR = "cap_editor"
    const val EDITOR_NAME = "cap_editor_name"
    const val EDITOR_AMOUNT = "cap_editor_amount"
    const val EDITOR_SAVE = "cap_editor_save"
    const val ARRANGE = "arrange"
    const val ARRANGE_LIST = "arrange_list"
    const val QUICK_ADDED = "quick_added"
    const val QUICK_UNDO = "quick_undo"
    const val QUICK_GONE = "quick_gone"

    fun tile(capId: Long) = "cap_tile_$capId"

    fun spend(spendId: Long) = "spend_$spendId"

    fun amount(amount: Money) = "numpad_amount_${amount.minor}"

    /** A cap's sheet's figures: the cap, its reserve, what was spent and what is left. */
    const val RECEIPT = "cap_receipt"

    /** An ended one-off's «Again, with new dates». */
    const val AGAIN = "cap_again"

    /** The editor's dates, a one-off's. */
    const val EDITOR_DATES = "cap_editor_dates"
}
