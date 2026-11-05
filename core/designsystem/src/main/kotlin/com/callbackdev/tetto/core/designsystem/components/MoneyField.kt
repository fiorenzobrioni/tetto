package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.callbackdev.tetto.core.designsystem.format.MoneyFormat
import com.callbackdev.tetto.core.domain.money.MoneyInput
import com.callbackdev.tetto.core.model.Money

/**
 * A cap's amount, typed through the numpad's own buffer (PLANNING.md §4.1): digits and the locale's
 * separator only, at most nine digits and the currency's decimals, parsed exactly. A change the
 * buffer would refuse is not taken. The currency's symbol stands where the locale writes it.
 *
 * The text follows [amount] when it changes from outside (a cap filled in, a currency changed and
 * its amounts rescaled), and is the reader's otherwise, so "12." stays as typed.
 */
@Composable
fun MoneyField(
    amount: Money,
    format: MoneyFormat,
    label: String,
    onAmount: (Money) -> Unit,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Done,
) {
    var text by rememberSaveable { mutableStateOf(shownText(amount, format)) }
    LaunchedEffect(amount, format) {
        if (typedInput(text, format)?.amount != amount) text = shownText(amount, format)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            val input = typedInput(new, format) ?: return@OutlinedTextField
            text = bufferText(input, format)
            onAmount(input.amount)
        },
        label = { Text(label) },
        prefix = format.prefix.trim().takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        suffix = format.suffix.trim().takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier,
    )
}

private fun shownText(amount: Money, format: MoneyFormat): String =
    if (amount.isPositive) bufferText(MoneyInput.of(amount, format.currency), format) else ""

/** [text] typed key by key into a fresh buffer; null when a character is not a key or is refused. */
private fun typedInput(text: String, format: MoneyFormat): MoneyInput? =
    text.fold(MoneyInput.empty(format.currency) as MoneyInput?) { input, char ->
        val current = input ?: return null
        val key = MoneyInput.keyFor(char, format.decimalSeparator) ?: return null
        current.press(key).takeIf { it !== current }
    }

private fun bufferText(input: MoneyInput, format: MoneyFormat): String =
    input.whole + (input.fraction?.let { format.decimalSeparator + it } ?: "")
