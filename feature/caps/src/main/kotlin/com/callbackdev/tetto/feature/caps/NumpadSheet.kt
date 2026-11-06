package com.callbackdev.tetto.feature.caps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
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
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Numpad(state, currency, onConfirm, onOpenCap, correcting = correcting)
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
) {
    val format = rememberMoneyFormat(currency)
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
        if (input.canAdd) {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            onConfirm(input.amount)
        }
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
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag(CapsTags.NUMPAD)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenMargin)
            .padding(bottom = 16.dp),
    ) {
        Header(state, format, correcting, onOpenCap)
        Spacer(Modifier.height(16.dp))
        Amount(format, input)
        // Before a spend is corrected, its own amount is back in what is left.
        val base = state.left + (correcting ?: Money.ZERO)
        Text(
            previewWords(SpendPreview.of(base, input.amount), format),
            style = MaterialTheme.typography.titleMedium,
            color = if (base <
                input.amount
            ) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(CapsTags.PREVIEW)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
        Spacer(Modifier.height(16.dp))
        NumpadKeys(
            separator = format.decimalSeparator.takeIf { input.hasSeparator },
            onDigit = { onKey(MoneyInput.Key.Digit(it)) },
            onSeparator = { onKey(MoneyInput.Key.Separator) },
            onDelete = { onKey(MoneyInput.Key.Delete) },
            onClear = { onKey(MoneyInput.Key.Clear) },
        )
        Spacer(Modifier.height(12.dp))
        val amount = input.amount
        val label = when {
            correcting != null && input.canAdd -> stringResource(R.string.numpad_save_amount, format.full(amount))
            correcting != null -> stringResource(R.string.numpad_save)
            input.canAdd -> stringResource(R.string.numpad_add_amount, format.full(amount))
            else -> stringResource(R.string.numpad_add)
        }
        Button(
            onClick = onConfirm,
            enabled = input.canAdd,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag(CapsTags.CONFIRM),
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun Header(state: CapState, format: MoneyFormat, correcting: Money?, onOpenCap: (() -> Unit)?) {
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
                if (correcting != null) stringResource(R.string.numpad_correcting) else leftWords(state, format),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
