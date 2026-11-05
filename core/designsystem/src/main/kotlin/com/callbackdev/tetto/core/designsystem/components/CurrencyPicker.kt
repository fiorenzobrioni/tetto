package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.callbackdev.tetto.core.designsystem.R
import com.callbackdev.tetto.core.designsystem.format.Currencies
import com.callbackdev.tetto.core.designsystem.format.CurrencyChoice
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter

/**
 * The app's currency, chosen from every one the phone knows (PLANNING.md §4.1, VISION «Settings»),
 * over the page on the whole screen: a hundred and fifty currencies are a page, not a dialog's
 * list. The search on top, [explanation] under it where the caller has one, then the list in the
 * language's alphabetical order, each with its code and symbol, opened on the reader's. A touch
 * chooses and closes.
 */
@Composable
fun CurrencyPickerDialog(
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    explanation: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        CurrencyPickerPage(selected, onPick, onDismiss, explanation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerPage(selected: String, onPick: (String) -> Unit, onBack: () -> Unit, explanation: String? = null) {
    val locale = LocalConfiguration.current.locales[0]
    val currencies = remember(locale, selected) { Currencies.all(locale, keep = selected) }
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(currencies, query) { currencies.filter { it.matches(query) } }
    // Opened on the reader's currency, a few rows down from the top, so the list says where it stands.
    val list = rememberLazyListState(
        initialFirstVisibleItemIndex = (currencies.indexOfFirst { it.code == selected } - ROWS_ABOVE).coerceAtLeast(0),
    )
    val gutter = pageGutter(sideInsets = false)
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.currency_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(TettoIcons.Back, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets.add(gutter.asInsets()),
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.currency_search)) },
                    leadingIcon = { Icon(TettoIcons.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(gutter)
                        .padding(horizontal = ScreenMargin)
                        .padding(bottom = 8.dp)
                        .testTag(CurrencyPickerTags.SEARCH),
                )
                // Under the search, not in the list: the list opens on the reader's currency, and
                // would scroll it out of sight.
                if (explanation != null) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(gutter)
                            .padding(start = ScreenMargin + 4.dp, end = ScreenMargin + 4.dp, bottom = 8.dp),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = list,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .selectableGroup()
                .testTag(CurrencyPickerTags.LIST),
            contentPadding = gutter.contentPadding(bottom = 24.dp),
        ) {
            items(shown, key = { it.code }) { currency ->
                CurrencyRow(currency, selected = currency.code == selected) { onPick(currency.code) }
            }
            if (shown.isEmpty()) {
                item(key = "not-found") {
                    Text(
                        text = stringResource(R.string.currency_not_found, query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }
}

/** A choice: the row is the touch, the radio button only shows it. "EUR · €" under the name. */
@Composable
private fun CurrencyRow(currency: CurrencyChoice, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = ScreenMargin + 4.dp, vertical = 10.dp)
            .testTag(CurrencyPickerTags.currency(currency.code)),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(currency.name, style = MaterialTheme.typography.bodyLarge)
            val note = if (currency.symbol == currency.code) currency.code else "${currency.code} · ${currency.symbol}"
            Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "Euro (EUR)": a currency's name in the reader's language, with its code. */
@Composable
fun currencyNamed(code: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val name = remember(code, locale) { Currencies.name(code, locale) }
    return stringResource(R.string.currency_named, name, code)
}

/** Rows shown over the reader's currency when the list opens on it. */
private const val ROWS_ABOVE = 2

/** Hooks for the UI tests. */
object CurrencyPickerTags {
    const val SEARCH = "currency_search"
    const val LIST = "currency_list"

    fun currency(code: String) = "currency_$code"
}
