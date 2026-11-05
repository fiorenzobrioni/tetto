package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.Share

/**
 * A cap's tile (VISION, «Caps»), its words written by the caller: the icon and the [name]; what is
 * [left] in the figure style, rolling to a new value ([leftValue] gives the direction); the cap
 * and its last day ([detail]); the bar with today's mark; the [pace] where it helps; the
 * [reserve] as a small chip, only while there is one.
 *
 * TalkBack reads it as one sentence, [description], and offers the touch ([clickLabel]: the
 * numpad) and the long press ([longClickLabel]: the cap's sheet) by name.
 */
@Composable
fun CapTile(
    icon: ImageVector,
    name: String,
    left: String,
    leftValue: Long,
    detail: String,
    bar: Share,
    mark: Share?,
    over: Boolean,
    ahead: Boolean,
    description: String,
    clickLabel: String,
    longClickLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    pace: String? = null,
    reserve: String? = null,
) {
    Surface(
        modifier = modifier
            .clip(GroupShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick(label = clickLabel) {
                    onClick()
                    true
                }
                onLongClick(label = longClickLabel) {
                    onLongClick()
                    true
                }
            },
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapBadge(icon)
                Spacer(Modifier.size(12.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            RollingText(
                text = left,
                value = leftValue,
                style = TettoTheme.type.readingValue,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            CapBar(bar = bar, mark = mark, over = over, ahead = ahead)
            if (pace != null || reserve != null) Spacer(Modifier.height(8.dp))
            if (pace != null) {
                Text(
                    pace,
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reserve != null) {
                if (pace != null) Spacer(Modifier.height(6.dp))
                ReserveChip(reserve)
            }
        }
    }
}

/** The "New cap" tile, last in the grid: the same shape, outlined, empty but for its words. */
@Composable
fun NewCapTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            CapBadge(icon)
            Spacer(Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** A cap's icon in its tinted circle, the family's way of setting an icon before a name. */
@Composable
fun CapBadge(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ReserveChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.tabular(),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
