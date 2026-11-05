package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.R
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.tabular

/**
 * The numpad's keys (VISION, «The numpad»): 1 to 9, the locale's decimal [separator] (none for a
 * currency without decimals: the place stays empty, so the keys never move), 0, and delete, whose
 * long press clears. Large, airy, borderless; at least the family's touch target and taller where
 * there is room. The keys only report: the sheet decides what a key does, and the haptic it earns.
 */
@Composable
fun NumpadKeys(
    separator: Char?,
    onDigit: (Int) -> Unit,
    onSeparator: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))) {
            Row(Modifier.fillMaxWidth()) {
                row.forEach { digit -> DigitKey(digit, onDigit) }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if (separator != null) {
                Key(onClick = onSeparator, label = null) {
                    Text(separator.toString(), style = keyStyle())
                }
            } else {
                Box(Modifier.weight(1f))
            }
            DigitKey(0, onDigit)
            val clear = stringResource(R.string.numpad_clear)
            Key(
                onClick = onDelete,
                onLongClick = onClear,
                longClickLabel = clear,
                label = stringResource(R.string.numpad_delete),
            ) {
                Icon(TettoIcons.Backspace, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun RowScope.DigitKey(digit: Int, onDigit: (Int) -> Unit) {
    Key(onClick = { onDigit(digit) }, label = null) {
        Text(digit.toString(), style = keyStyle())
    }
}

@Composable
private fun keyStyle() = MaterialTheme.typography.headlineMedium.tabular()

@Composable
private fun RowScope.Key(
    onClick: () -> Unit,
    label: String?,
    onLongClick: (() -> Unit)? = null,
    longClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .weight(1f)
            .padding(4.dp)
            .heightIn(min = KeyHeight)
            .clip(RoundedCornerShape(KeyHeight / 2))
            .combinedClickable(
                role = Role.Button,
                onLongClick = onLongClick,
                onLongClickLabel = longClickLabel,
                onClick = onClick,
            )
            .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private val KeyHeight = 60.dp
