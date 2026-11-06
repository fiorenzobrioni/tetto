package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.R
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.Share

/**
 * A cap's tile (VISION, «Caps»), its words written by the caller: the icon and the [name]; the
 * [figure], what is left or, [over], by how much, alone on its line in the figure style and
 * rolling to a new value ([figureValue] gives the direction), with its word ([CapFigure]); the
 * cap and its last day ([detail]); the bar with today's mark; the [pace] where it helps; the
 * [reserve] as a small chip, only while there is one.
 *
 * TalkBack reads it as one sentence, [description], and offers the touch ([clickLabel]: the
 * numpad) and the long press ([longClickLabel]: the cap's sheet) by name. Not [enabled], it is a
 * picture of a tile (the guide's examples, the appearance's preview): no touch, no ripple, and
 * TalkBack reads its sentence without calling it a button.
 */
@Composable
fun CapTile(
    icon: ImageVector,
    name: String,
    figure: String,
    figureValue: Long,
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
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.capTouch(enabled, description, clickLabel, longClickLabel, onClick, onLongClick),
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapBadge(icon)
                Spacer(Modifier.size(12.dp))
                CapName(name)
            }
            Spacer(Modifier.height(12.dp))
            CapFigure(figure, figureValue, over)
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

/**
 * A cap's row (owner, 6 Nov 2026), the caps screen's list: the tile's words across the page,
 * shorter. The icon; the [name] and, at the row's end, the [figure] with its word on one line; the
 * cap and its last day ([detail]); the bar with today's mark. The pace and the reserve stay in the
 * cap's sheet and in TalkBack's [description]: what keeps a row short.
 *
 * Touch, long press and [enabled] as [CapTile]'s.
 */
@Composable
fun CapRow(
    icon: ImageVector,
    name: String,
    figure: String,
    figureValue: Long,
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
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.capTouch(enabled, description, clickLabel, longClickLabel, onClick, onLongClick),
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            CapBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                // Side by side where both fit; where they do not (a long name, a large text size),
                // the figure goes under the name, so neither is squeezed into a broken word.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    CapName(name, Modifier.weight(1f))
                    CapFigureInline(figure, figureValue, over)
                }
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                CapBar(bar = bar, mark = mark, over = over, ahead = ahead)
            }
        }
    }
}

/**
 * The touch a tile and a row share: one sentence for TalkBack, the numpad on a touch and the cap's
 * sheet on a long press, each named. A picture ([enabled] false) has no touch at all: a disabled
 * one would still be read as a button.
 */
private fun Modifier.capTouch(
    enabled: Boolean,
    description: String,
    clickLabel: String,
    longClickLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = clip(GroupShape)
    .then(if (enabled) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick) else Modifier)
    .clearAndSetSemantics {
        contentDescription = description
        if (enabled) {
            role = Role.Button
            onClick(label = clickLabel) {
                onClick()
                true
            }
            onLongClick(label = longClickLabel) {
                onLongClick()
                true
            }
        }
    }

/** A cap's name: two lines at most, and never a word cut in two: a long name steps down first. */
@Composable
private fun CapName(name: String, modifier: Modifier = Modifier) {
    val title = MaterialTheme.typography.titleMedium
    BasicText(
        name,
        style = title.copy(color = LocalContentColor.current),
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        autoSize = WholeWordsAutoSize(max = title.fontSize),
    )
}

/**
 * A cap's figure with the word that says what it is, on a tile: "ancora / 12 €" where the
 * language puts the word first, "€12 / left" where it follows. The figure keeps a line of its own,
 * so it is never broken, and the tile is as tall whatever the amount.
 */
@Composable
private fun CapFigure(figure: String, figureValue: Long, over: Boolean) {
    val first = booleanResource(R.bool.cap_word_first)
    Column {
        if (first) FigureWord(over)
        RollingText(text = figure, value = figureValue, style = TettoTheme.type.readingValue, color = figureInk(over))
        if (!first) FigureWord(over)
    }
}

/**
 * The same on a row, where there is the width: "ancora 12 €", "€12 left", on their baseline. One
 * line, so its leading is trimmed to the figure: what keeps a row short.
 */
@Composable
private fun CapFigureInline(figure: String, figureValue: Long, over: Boolean) {
    val first = booleanResource(R.bool.cap_word_first)
    Row {
        if (first) {
            FigureWord(over, Modifier.alignByBaseline())
            Spacer(Modifier.width(6.dp))
        }
        RollingText(
            text = figure,
            value = figureValue,
            style = TettoTheme.type.readingValue.copy(lineHeight = RowFigureLine),
            color = figureInk(over),
            modifier = Modifier.alignByBaseline(),
        )
        if (!first) {
            Spacer(Modifier.width(6.dp))
            FigureWord(over, Modifier.alignByBaseline())
        }
    }
}

@Composable
private fun figureInk(over: Boolean): Color =
    if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

/**
 * The figure's word, small; in the figure's ink when it says over: over is said in words, not by
 * the colour alone.
 */
@Composable
private fun FigureWord(over: Boolean, modifier: Modifier = Modifier) {
    Text(
        stringResource(if (over) R.string.cap_word_over else R.string.cap_word_left),
        style = MaterialTheme.typography.labelLarge,
        color = if (over) figureInk(true) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The "New cap" row, last in the list: a row's shape, outlined, its badge and its words. */
@Composable
fun NewCapRow(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            CapBadge(icon)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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

/** A row's figure line: the reading value's 24 sp on 28, not the tile's 32. */
private val RowFigureLine = 28.sp
