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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.CapBar
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.GroupHeader
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
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
    val onRefund: () -> Unit = {},
    val onAgain: () -> Unit = onEdit,
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
    if (state.ended) {
        EndedSheetContent(state, data, format, today, actions, modifier)
        return
    }
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

        // One rule for the sheet's figures, the receipt's and the spends': cents on every one as
        // soon as one has them, so a column reads as a column ("€20.00", "€3.20"), and none where
        // none has ("€20", "€8") (owner, 6 Nov 2026).
        val unit = format.currency.unit.minor
        val cents = (
            listOf(state.cap.amount, state.cap.reserve, state.spent, state.left) +
                data.spends.map { it.amount }
            )
            .any { it.minor % unit != 0L }
        val figure: (Money) -> String = if (cents) format::full else format::short

        GroupHeader(stringResource(R.string.sheet_this_period))
        Receipt(state, figure)
        // The tile's pace, here too: the list's rows leave it out.
        state.pace?.let { pace ->
            Text(
                stringResource(R.string.sheet_pace_line, format.short(pace)),
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ScreenMargin + 16.dp).padding(top = 8.dp),
            )
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
                    key(spend.id) { SpendRow(spend, figure, actions) }
                }
            }
        }
        // Money back (owner, 6 Nov 2026): only where something was spent to give back.
        if (state.spent.isPositive) {
            TextButton(
                onClick = actions.onRefund,
                modifier = Modifier.padding(horizontal = ScreenMargin - 12.dp).padding(top = 4.dp),
            ) {
                Icon(TettoIcons.Renew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sheet_refund))
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
 * An ended one-off's sheet (owner, 6 Nov 2026): its dates, what they spent of the cap as a receipt,
 * with what was left to spare or how far over (forgiven), and its two actions: «Again, with new
 * dates», which opens the editor with its name, icon and amount kept, and «Delete». No spend can be
 * added, corrected or given back: its dates are over.
 */
@Composable
private fun EndedSheetContent(
    state: CapState,
    data: CapSheetData,
    format: MoneyFormat,
    today: LocalDate,
    actions: CapSheetActions,
    modifier: Modifier,
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
            mark = null,
            over = state.over,
            ahead = false,
            muted = true,
            modifier = Modifier.padding(horizontal = ScreenMargin),
        )
        val unit = format.currency.unit.minor
        val cents = listOf(state.cap.amount, state.spent).any { it.minor % unit != 0L }
        val figure: (Money) -> String = if (cents) format::full else format::short
        Spacer(Modifier.height(16.dp))
        EndedReceipt(state, figure)
        Text(
            stringResource(R.string.sheet_ended_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenMargin).padding(top = 16.dp),
        )
        if (data.lines.size > 1) {
            GroupHeader(stringResource(R.string.sheet_last_periods))
            data.lines.drop(1).forEach { line ->
                Text(
                    lineWords(line, state.cap.kind, format, today),
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Column(Modifier.padding(horizontal = ScreenMargin), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = actions.onAgain,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(CapsTags.AGAIN),
            ) {
                Icon(TettoIcons.Renew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sheet_again))
            }
            TextButton(onClick = actions.onDeleteCap, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sheet_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * How an ended one-off's dates went, as a receipt: the cap, less what was spent, and under a rule
 * what was left to spare, or how far over, forgiven. One sentence for TalkBack.
 */
@Composable
private fun EndedReceipt(state: CapState, figure: (Money) -> String) {
    val result = if (state.over) {
        stringResource(R.string.sheet_receipt_ended_over, figure(state.overBy))
    } else {
        stringResource(R.string.sheet_receipt_ended_spare, figure(state.left))
    }
    val parts = listOf(
        stringResource(R.string.sheet_receipt_cap, figure(state.cap.amount)),
        stringResource(R.string.sheet_receipt_spent, figure(state.spent)),
    )
    val sentence = stringResource(R.string.sheet_receipt_sentence, parts.joinToString(", "), result)
    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(CapsTags.RECEIPT)
            .clearAndSetSemantics { contentDescription = sentence },
    ) {
        ReceiptLine(null, stringResource(R.string.sheet_cap), figure(state.cap.amount))
        ReceiptLine(MINUS, stringResource(R.string.sheet_spent), figure(state.spent))
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        val ink = if (state.over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (state.over) R.string.sheet_ended_forgiven else R.string.sheet_ended_spare),
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                modifier = Modifier.weight(1f).padding(start = SIGN_WIDTH),
            )
            Text(
                figure(if (state.over) state.overBy else state.left),
                style = TettoTheme.type.readingValue,
                color = ink,
            )
        }
    }
}

/**
 * A spend of this period: its amount, its day and time. A touch corrects it, a swipe deletes it;
 * TalkBack offers both by name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendRow(spend: Spend, figure: (Money) -> String, actions: CapSheetActions) {
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
            Column(Modifier.weight(1f)) {
                // A refund reads as money back: its size after a minus, and the word under it.
                Text(
                    if (spend.refund) MINUS + figure(spend.size) else figure(spend.size),
                    style = MaterialTheme.typography.bodyLarge.tabular(),
                    color = if (spend.refund) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                if (spend.refund) {
                    Text(
                        stringResource(R.string.spend_refund),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                momentWords(spend.addedAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * This period's figures as a receipt (owner, 6 Nov 2026): the cap, plus the reserve, less what was
 * spent, and under a rule what is left, or how far over, in the figure's size; a column of
 * tabular figures, signs apart, so the sum reads at a glance. TalkBack reads it as the one
 * sentence it is: "€20 cap, plus €4 in reserve, less €8 spent: €16 left".
 */
@Composable
private fun Receipt(state: CapState, figure: (Money) -> String) {
    val parts = listOfNotNull(
        stringResource(R.string.sheet_receipt_cap, figure(state.cap.amount)),
        state.cap.reserve.takeIf { it.isPositive }?.let { stringResource(R.string.sheet_receipt_reserve, figure(it)) },
        stringResource(R.string.sheet_receipt_spent, figure(state.spent)),
    )
    val result = if (state.over) {
        stringResource(R.string.sheet_receipt_over, figure(state.overBy))
    } else {
        stringResource(R.string.sheet_receipt_left, figure(state.left))
    }
    val sentence = stringResource(R.string.sheet_receipt_sentence, parts.joinToString(", "), result)
    Column(
        Modifier
            .padding(horizontal = ScreenMargin)
            .fillMaxWidth()
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(CapsTags.RECEIPT)
            .clearAndSetSemantics { contentDescription = sentence },
    ) {
        ReceiptLine(null, stringResource(R.string.sheet_cap), figure(state.cap.amount))
        if (state.cap.reserve.isPositive) {
            ReceiptLine(PLUS, stringResource(R.string.sheet_reserve), figure(state.cap.reserve))
        }
        ReceiptLine(MINUS, stringResource(R.string.sheet_spent), figure(state.spent))
        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        val ink = if (state.over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (state.over) R.string.sheet_over else R.string.sheet_left),
                style = MaterialTheme.typography.titleMedium,
                color = ink,
                modifier = Modifier.weight(1f).padding(start = SIGN_WIDTH),
            )
            Text(
                figure(if (state.over) state.overBy else state.left),
                style = TettoTheme.type.readingValue,
                color = ink,
            )
        }
    }
}

/** A line of the receipt: its sign in a column of its own, its word, its figure at the end. */
@Composable
private fun ReceiptLine(sign: String?, label: String, amount: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            sign.orEmpty(),
            style = MaterialTheme.typography.bodyLarge.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(SIGN_WIDTH),
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(amount, style = MaterialTheme.typography.bodyLarge.tabular())
    }
}

private const val PLUS = "+"

/** The minus sign, not a hyphen: the same width as the plus in tabular figures. */
private const val MINUS = "\u2212"
private val SIGN_WIDTH = 20.dp
