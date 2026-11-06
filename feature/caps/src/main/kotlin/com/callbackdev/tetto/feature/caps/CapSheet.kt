package com.callbackdev.tetto.feature.caps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.CapBar
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.GroupHeader
import com.callbackdev.tetto.core.designsystem.components.InfoRow
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Spend
import java.time.LocalDate

/**
 * A cap's sheet (VISION, «A cap's sheet»), from a long press on its tile or the numpad's header.
 * Not a history: this period, its spends (correctable, deletable, with Undo), the last periods'
 * lines that explain the reserve, and what can be done with the cap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapSheet(
    state: CapState,
    data: CapSheetData?,
    currency: AppCurrency,
    today: LocalDate,
    actions: CapSheetActions,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CapSheetContent(
            state,
            data ?: CapSheetData(state.cap.id, emptyList(), emptyList()),
            rememberMoneyFormat(currency),
            today,
            actions,
        )
    }
}

/** What a cap's sheet can do. */
class CapSheetActions(
    val onCorrect: (Spend) -> Unit,
    val onDelete: (Spend) -> Unit,
    val onClearReserve: () -> Unit,
    val onEdit: () -> Unit,
    val onDeleteCap: () -> Unit,
)

@Composable
internal fun CapSheetContent(
    state: CapState,
    data: CapSheetData,
    format: MoneyFormat,
    today: LocalDate,
    actions: CapSheetActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag(CapsTags.SHEET)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(Modifier.padding(horizontal = ScreenMargin), verticalAlignment = Alignment.CenterVertically) {
            CapBadge(state.cap.icon.symbol)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.cap.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    rangeWords(state.period, today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        CapBar(
            bar = state.bar,
            mark = state.todayMark,
            over = state.over,
            ahead = state.aheadOfDays,
            modifier = Modifier.padding(horizontal = ScreenMargin),
        )

        GroupHeader(stringResource(R.string.sheet_this_period))
        SettingsGroup {
            InfoRow(stringResource(R.string.sheet_cap), format.short(state.cap.amount))
            if (state.cap.reserve.isPositive) {
                GroupDivider()
                InfoRow(stringResource(R.string.sheet_reserve), format.short(state.cap.reserve))
            }
            GroupDivider()
            InfoRow(stringResource(R.string.sheet_spent), format.short(state.spent))
            GroupDivider()
            if (state.over) {
                InfoRow(stringResource(R.string.sheet_over), format.short(state.overBy))
            } else {
                InfoRow(stringResource(R.string.sheet_left), format.short(state.left))
            }
            // The tile's pace, here too: the list's rows leave it out.
            state.pace?.let { pace ->
                GroupDivider()
                InfoRow(
                    stringResource(R.string.sheet_pace),
                    stringResource(R.string.sheet_pace_value, format.short(pace)),
                )
            }
        }

        GroupHeader(stringResource(R.string.sheet_spends))
        if (data.spends.isEmpty()) {
            Text(
                stringResource(R.string.sheet_no_spends),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp),
            )
        } else {
            SettingsGroup {
                data.spends.forEachIndexed { index, spend ->
                    if (index > 0) GroupDivider()
                    key(spend.id) { SpendRow(spend, format, actions) }
                }
            }
        }

        if (data.lines.isNotEmpty()) {
            GroupHeader(stringResource(R.string.sheet_last_periods))
            data.lines.forEach { line ->
                Text(
                    lineWords(line, state.cap.kind, format, today),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Column(Modifier.padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.cap.reserve.isPositive) {
                OutlinedButton(onClick = actions.onClearReserve, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sheet_clear_reserve))
                }
            }
            OutlinedButton(onClick = actions.onEdit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sheet_edit))
            }
            TextButton(onClick = actions.onDeleteCap, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sheet_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * A spend of this period: its amount, its day and time. A touch corrects it, a swipe deletes it;
 * TalkBack offers both by name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendRow(spend: Spend, format: MoneyFormat, actions: CapSheetActions) {
    val correctLabel = stringResource(R.string.spend_correct)
    val deleteLabel = stringResource(R.string.spend_delete)
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        onDismiss = { actions.onDelete(spend) },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp),
                contentAlignment = if (dismiss.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Alignment.CenterEnd
                } else {
                    Alignment.CenterStart
                },
            ) {
                Icon(TettoIcons.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClickLabel = correctLabel) { actions.onCorrect(spend) }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(deleteLabel) {
                            actions.onDelete(spend)
                            true
                        },
                    )
                }
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(CapsTags.spend(spend.id)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                format.full(spend.amount),
                style = MaterialTheme.typography.bodyLarge.tabular(),
                modifier = Modifier.weight(1f),
            )
            Text(
                momentWords(spend.addedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
