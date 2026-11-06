package com.callbackdev.tetto.feature.caps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money
import com.callbackdev.tetto.core.model.Spend
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The numpad over the home screen (PLANNING.md §7, Taps): the caps screen's numpad, on one cap, in a
 * sheet over whatever is on screen. After Add the sheet becomes one line, "€5.00 added to Coffee",
 * with what is left and Undo, for a few seconds (as long as the reader's accessibility timeout
 * asks, longer with TalkBack), then closes by itself; a touch outside closes it at once. [onAdded]
 * hears of each spend added; [onOpenApp] opens Tetto, from the numpad's header.
 */
@Composable
fun QuickAddRoute(
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    onAdded: () -> Unit = {},
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val amounts by viewModel.amounts.collectAsStateWithLifecycle()
    val current = state ?: return
    QuickAddSheet(
        state = current,
        phase = phase,
        amounts = amounts,
        onAdd = { viewModel.add(it, onAdded) },
        onUndo = viewModel::undo,
        onClose = onClose,
        onOpenApp = onOpenApp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    state: QuickAddUiState,
    phase: QuickAddPhase,
    amounts: List<Money>,
    onAdd: (Money) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: () -> Unit = { scope.launch { sheet.hide() }.invokeOnCompletion { onClose() } }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet) {
        QuickAddContent(state, phase, onAdd, onUndo, close, onOpenApp, amounts)
    }
}

/** What the sheet holds: the numpad, the line after Add, or the word that the cap is gone. */
@Composable
internal fun QuickAddContent(
    state: QuickAddUiState,
    phase: QuickAddPhase,
    onAdd: (Money) -> Unit,
    onUndo: () -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit,
    amounts: List<Money> = emptyList(),
) {
    val cap = state.cap
    val format = rememberMoneyFormat(state.currency)
    val reduced = reducedMotion()
    if (cap == null) {
        Gone(onOpenApp)
        return
    }
    AnimatedContent(
        targetState = phase is QuickAddPhase.Added,
        transitionSpec = {
            (fadeIn(TettoMotion.effects(reduced)) togetherWith fadeOut(TettoMotion.effects(reduced)))
                .using(SizeTransform(clip = false) { _, _ -> TettoMotion.spatial(reduced) })
        },
        label = "quick add",
    ) { added ->
        if (added && phase is QuickAddPhase.Added) {
            AddedLine(cap, phase.spend, format, state, onUndo, onClose)
        } else {
            key(phase.round) {
                Numpad(
                    state = cap,
                    currency = state.currency,
                    onConfirm = { if (phase is QuickAddPhase.Typing) onAdd(it) },
                    onOpenCap = onOpenApp,
                    initial = (phase as? QuickAddPhase.Typing)?.initial,
                    amounts = amounts,
                )
            }
        }
    }
}

/**
 * "€5.00 added to Coffee", what is left after it, and Undo; the sheet closes by itself after the
 * reader's accessible timeout, unless Undo brings the numpad back first.
 */
@Composable
private fun AddedLine(
    cap: CapState,
    spend: Spend,
    format: MoneyFormat,
    state: QuickAddUiState,
    onUndo: () -> Unit,
    onClose: () -> Unit,
) {
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(spend.id) {
        val wait = accessibility?.calculateRecommendedTimeoutMillis(
            CLOSE_AFTER_MILLIS,
            containsIcons = true,
            containsText = true,
            containsControls = true,
        ) ?: CLOSE_AFTER_MILLIS
        delay(wait)
        onClose()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(top = 4.dp, bottom = 24.dp)
            .heightIn(min = 56.dp)
            .testTag(CapsTags.QUICK_ADDED),
    ) {
        CapBadge(cap.cap.icon.symbol)
        Column(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(
                stringResource(R.string.notice_added, format.full(spend.amount), cap.cap.name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                afterWords(cap, format, state),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cap.over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onUndo, modifier = Modifier.heightIn(min = 48.dp).testTag(CapsTags.QUICK_UNDO)) {
            Text(stringResource(R.string.action_undo))
        }
    }
}

/** "€7 left until Sunday"; over, "€3 over · starts again on Monday". */
@Composable
private fun afterWords(cap: CapState, format: MoneyFormat, state: QuickAddUiState): String = if (cap.over) {
    stringResource(R.string.quick_after_over, leftWords(cap, format), againWords(cap.startsAgain, state.today))
} else {
    stringResource(R.string.quick_after, leftWords(cap, format), untilWords(cap.lastDay, state.today))
}

/** The cap the door named is no longer there: deleted in the app since the card was drawn. */
@Composable
private fun Gone(onOpenApp: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .padding(bottom = 24.dp)
            .testTag(CapsTags.QUICK_GONE),
    ) {
        Text(stringResource(R.string.quick_gone_title), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            stringResource(R.string.quick_gone_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 16.dp))
        Button(onClick = onOpenApp, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
            Text(stringResource(R.string.quick_open_app))
        }
    }
}

/** How long the line after Add stays, before the reader's accessibility timeout stretches it. */
private const val CLOSE_AFTER_MILLIS = 4_000L
