package com.callbackdev.tetto.feature.caps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.NumpadKeys
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.TettoMotion
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.reducedMotion
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.domain.cap.SpendPreview
import com.callbackdev.tetto.core.domain.money.MoneyInput
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.Money

/**
 * The numpad over the caps screen (VISION, «The numpad»), on [state]'s cap: for a new spend, or,
 * with [correcting], for a spend of the open period. Dismissed by the system's gestures only;
 * [onConfirm] with the amount typed. Also the sheet over the home screen (Phase 5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumpadSheet(
    state: CapState,
    currency: AppCurrency,
    onConfirm: (Money) -> Unit,
    onDismiss: () -> Unit,
    onOpenCap: (() -> Unit)?,
    correcting: Money? = null,
    refund: Boolean = false,
    amounts: List<Money> = emptyList(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Numpad(state, currency, onConfirm, onOpenCap, correcting = correcting, refund = refund, amounts = amounts)
    }
}

/**
 * The numpad's content, its typed amount kept across a rotation; [initial] types an amount in
 * before the reader does (the numpad over the home screen, back after Undo). Each key earns the phone's own
 * keyboard tap, or its reject when the buffer refuses it (the tenth digit, a third decimal); Add
 * earns a confirm. A hardware keyboard types too: digits, a dot or a comma, Backspace, Enter.
 */
@Composable
fun Numpad(
    state: CapState,
    currency: AppCurrency,
    onConfirm: (Money) -> Unit,
    onOpenCap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    correcting: Money? = null,
    initial: Money? = null,
    amounts: List<Money> = emptyList(),
    refund: Boolean = false,
) {
    val format = rememberMoneyFormat(currency)
    // A refund gives back at most what this period spent (the refund corrected counted out).
    val most = if (refund) state.spent + (correcting ?: Money.ZERO) else null
    var input by rememberSaveable(stateSaver = moneyInputSaver(currency)) {
        mutableStateOf((correcting ?: initial)?.let { MoneyInput.of(it, currency) } ?: MoneyInput.empty(currency))
    }
    val haptic = LocalHapticFeedback.current
    val press: (MoneyInput.Key) -> Unit = { key ->
        val next = input.press(key)
        haptic.performHapticFeedback(if (next === input) HapticFeedbackType.Reject else HapticFeedbackType.KeyboardTap)
        input = next
    }
    val confirm: () -> Unit = {
        if (input.canAdd && (most == null || input.amount <= most)) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onConfirm(input.amount)
        }
    }
    // One of the cap's habits, added in one touch: the same Add, the same Undo after it.
    val quick: (Money) -> Unit = { amount ->
        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
        onConfirm(amount)
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    NumpadContent(
        state = state,
        format = format,
        input = input,
        correcting = correcting,
        onKey = press,
        onConfirm = confirm,
        onOpenCap = onOpenCap,
        // Never while a spend is corrected, nor for a refund: there the amount is its own.
        amounts = if (correcting == null && !refund) amounts else emptyList(),
        refundMost = most,
        onAmount = quick,
        modifier = modifier
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val key = when (event.key) {
                    Key.Backspace -> MoneyInput.Key.Delete

                    Key.Escape -> return@onPreviewKeyEvent false

                    Key.Enter, Key.NumPadEnter -> {
                        confirm()
                        return@onPreviewKeyEvent true
                    }

                    else -> MoneyInput.keyFor(event.utf16CodePoint.toChar(), format.decimalSeparator)
                }
                if (key == null) {
                    false
                } else {
                    press(key)
                    true
                }
            },
    )
}

/** The numpad drawn from its state: header, amount, preview, keys, the button. */
@Composable
internal fun NumpadContent(
    state: CapState,
    format: MoneyFormat,
    input: MoneyInput,
    correcting: Money?,
    onKey: (MoneyInput.Key) -> Unit,
    onConfirm: () -> Unit,
    onOpenCap: (() -> Unit)?,
    modifier: Modifier = Modifier,
    amounts: List<Money> = emptyList(),
    onAmount: (Money) -> Unit = {},
    refundMost: Money? = null,
) {
    val refund = refundMost != null
    // A refund past what was spent is not taken: the preview says the most, the button waits.
    val tooMuch = refundMost != null && input.amount > refundMost
    Column(
        modifier
            .fillMaxWidth()
            .testTag(CapsTags.NUMPAD)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin)
            .padding(bottom = 16.dp),
    ) {
        Header(state, format, correcting, refund, onOpenCap)
        Spacer(Modifier.height(16.dp))
        // The amount and its preview, one polite live region for TalkBack: each key is heard as
        // what is typed and what it would leave ("€4.50, €11.50 left"), the separator too, which
        // changes the figure and not what is left.
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(CapsTags.READOUT)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        ) {
            Amount(format, input)
            // Before a spend is corrected, its own amount is back in what is left; before a refund
            // is, its own is taken out of it.
            val base = if (refund) state.left - (correcting ?: Money.ZERO) else state.left + (correcting ?: Money.ZERO)
            val preview = if (refund) SpendPreview.ofRefund(base, input.amount) else SpendPreview.of(base, input.amount)
            Text(
                if (tooMuch && refundMost != null) {
                    stringResource(R.string.preview_refund_most, format.full(refundMost))
                } else {
                    previewWords(preview, format)
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (tooMuch || preview.kind != SpendPreview.Kind.FITS) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag(CapsTags.PREVIEW),
            )
        }
        if (amounts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            QuickAmounts(amounts, format, visible = input.isEmpty, onAmount = onAmount)
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
        NumpadKeys(
            separator = format.decimalSeparator.takeIf { input.hasSeparator },
            onDigit = { onKey(MoneyInput.Key.Digit(it)) },
            onSeparator = { onKey(MoneyInput.Key.Separator) },
            onDelete = { onKey(MoneyInput.Key.Delete) },
            onClear = { onKey(MoneyInput.Key.Clear) },
        )
        Spacer(Modifier.height(12.dp))
        val amount = input.amount
        val ready = input.canAdd && !tooMuch
        val label = when {
            correcting != null && ready -> stringResource(R.string.numpad_save_amount, format.full(amount))
            correcting != null -> stringResource(R.string.numpad_save)
            refund && ready -> stringResource(R.string.numpad_refund_amount, format.full(amount))
            refund -> stringResource(R.string.numpad_refund)
            ready -> stringResource(R.string.numpad_add_amount, format.full(amount))
            else -> stringResource(R.string.numpad_add)
        }
        Button(
            onClick = onConfirm,
            enabled = ready,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(CapsTags.CONFIRM),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Header(
    state: CapState,
    format: MoneyFormat,
    correcting: Money?,
    refund: Boolean,
    onOpenCap: (() -> Unit)?,
) {
    val openLabel = stringResource(R.string.numpad_open_cap, state.cap.name)
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onOpenCap != null) {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpenCap)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CapBadge(state.cap.icon.symbol)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.cap.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                when {
                    correcting != null && refund -> stringResource(R.string.numpad_correcting_refund)
                    correcting != null -> stringResource(R.string.numpad_correcting)
                    refund -> stringResource(R.string.numpad_refunding, format.full(state.spent))
                    else -> leftWords(state, format)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The arrow that says the header leads to the cap's page, as a tile's does.
        if (onOpenCap != null) {
            Icon(TettoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The cap's habits above the keys (owner, 6 Nov 2026): its most used amounts, smallest first, each
 * added in one touch ([RecentAmounts][com.callbackdev.tetto.core.domain.cap.RecentAmounts]). Shown
 * while nothing is typed; once a key is pressed they fade, keeping their room, so the keys never
 * move under a finger.
 */
@Composable
private fun QuickAmounts(amounts: List<Money>, format: MoneyFormat, visible: Boolean, onAmount: (Money) -> Unit) {
    val reduced = reducedMotion()
    Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(CapsTags.AMOUNTS), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(TettoMotion.effects(reduced)),
            exit = fadeOut(TettoMotion.effects(reduced)),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth(),
            ) {
                amounts.forEach { amount ->
                    val label = stringResource(R.string.numpad_add_amount, format.full(amount))
                    SuggestionChip(
                        onClick = { onAmount(amount) },
                        label = {
                            Text(
                                format.full(amount),
                                style = MaterialTheme.typography.labelLarge.tabular(),
                                modifier = Modifier.clearAndSetSemantics {},
                            )
                        },
                        modifier = Modifier.semantics { contentDescription = label }.testTag(CapsTags.amount(amount)),
                    )
                }
            }
        }
    }
}

/**
 * The amount typed, in the hero size, tabular, with a caret that does not blink (blinking is
 * noise). Nine digits and the decimals never fit at the hero size on a phone: the figure steps
 * down until it does, never cut.
 */
@Composable
private fun Amount(format: MoneyFormat, input: MoneyInput) {
    val hero = TettoTheme.type.heroNumber
    val ink = if (input.isEmpty) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().testTag(CapsTags.AMOUNT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        BasicText(
            format.typed(input.whole, input.fraction),
            style = hero.copy(color = ink),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = hero.fontSize, stepSize = 2.sp),
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(
            Modifier
                .padding(start = 4.dp)
                .width(3.dp)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
    }
}

/** A buffer saved as what was typed, and typed again on restore: the buffer's own rules apply. */
private fun moneyInputSaver(currency: AppCurrency) = Saver<MoneyInput, String>(
    save = { it.whole + (it.fraction?.let { fraction -> ".$fraction" } ?: "") },
    restore = { saved ->
        saved.fold(MoneyInput.empty(currency)) { input, char ->
            MoneyInput.keyFor(char, '.')?.let(input::press) ?: input
        }
    },
)
