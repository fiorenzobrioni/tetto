package com.callbackdev.tetto.feature.caps

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.components.CapTile
import com.callbackdev.tetto.core.designsystem.components.NewCapTile
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.model.CapState
import java.time.LocalDate

/**
 * The caps screen (VISION, «Caps»; PLANNING.md §11 Phase 2), drawn from its state: today, the
 * sentence before any number, a tile per cap in the reader's order (two columns on a phone, one at
 * large text sizes), "New cap" last, and "Change the order" under them from two caps up. A touch
 * on a tile opens the numpad on it, a long press its sheet.
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
    contentPadding: PaddingValues = PaddingValues(),
) {
    val format = rememberMoneyFormat(state.currency)
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
        Text(
            sentence ?: stringResource(R.string.caps_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        if (state.caps.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.caps_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        TileGrid(state, format, onAddSpend, onOpenCap, onNewCap)
        if (state.caps.size >= 2) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onChangeOrder) { Text(stringResource(R.string.change_order)) }
            }
        }
    }
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
    val cells: List<CapState?> = state.caps + null
    Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
        cells.chunked(columns).forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(TileGap),
            ) {
                row.forEach { cell ->
                    val cellModifier = Modifier.weight(1f).fillMaxHeight()
                    if (cell == null) {
                        NewCapTile(stringResource(R.string.new_cap), TettoIcons.Plus, onNewCap, cellModifier)
                    } else {
                        Tile(cell, format, state.today, onAddSpend, onOpenCap, cellModifier)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
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
    CapTile(
        icon = state.cap.icon.symbol,
        name = state.cap.name,
        left = leftWords(state, format),
        leftValue = state.left.minor,
        detail = detailWords(state, format, today),
        bar = state.bar,
        mark = state.todayMark,
        over = state.over,
        ahead = state.aheadOfDays,
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
private const val LARGE_TEXT = 1.5f
private const val NARROW_DP = 300f

/** What the tests find the page and its tiles by. */
object CapsTags {
    const val PAGE = "caps_page"
    const val SETTINGS = "caps_settings"
    const val NUMPAD = "numpad"
    const val AMOUNT = "numpad_amount"
    const val PREVIEW = "numpad_preview"
    const val CONFIRM = "numpad_confirm"
    const val SHEET = "cap_sheet"
    const val EDITOR = "cap_editor"
    const val EDITOR_NAME = "cap_editor_name"
    const val EDITOR_AMOUNT = "cap_editor_amount"
    const val EDITOR_SAVE = "cap_editor_save"
    const val ARRANGE = "arrange"
    const val ARRANGE_LIST = "arrange_list"

    fun tile(capId: Long) = "cap_tile_$capId"

    fun spend(spendId: Long) = "spend_$spendId"
}
