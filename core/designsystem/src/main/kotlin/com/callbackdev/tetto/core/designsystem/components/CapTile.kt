package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.tetto.core.designsystem.R
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.TettoTheme
import com.callbackdev.tetto.core.designsystem.theme.tabular
import com.callbackdev.tetto.core.model.Share

/**
 * A cap's tile (VISION, «Caps»), its words written by the caller: the icon and the [name], with
 * the arrow that opens the cap's sheet; the [figure], what is left or, [over], by how much, in the
 * figure style and rolling to a new value ([figureValue] gives the direction), its word on the
 * same line ([CapFigure]); the cap ([detail], "of €20") and, on a line of its own, its last day
 * ([dateLine], "until Sunday"), so every tile has the same lines and the bars of a row stand
 * level; the bar with today's mark; the [pace] where it helps; the [reserve] as a small chip at
 * the tile's foot, only while there is one.
 *
 * TalkBack reads it as one sentence, [description], and offers the touch ([clickLabel]: the
 * numpad) and the long press ([longClickLabel]: the cap's sheet) by name; the header's arrow is
 * the long press's for a finger that has not found it (owner, 6 Nov 2026), and TalkBack already
 * offers it by name. Not [enabled], it is a picture of a tile (the guide's examples, the
 * appearance's preview): no touch, no ripple, no arrow, and TalkBack reads its sentence without
 * calling it a button.
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
    dateLine: String? = null,
    pace: String? = null,
    reserve: String? = null,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.capTouch(enabled, description, clickLabel, longClickLabel, onClick, onLongClick),
        shape = GroupShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        // Two parts, the second at the tile's foot: in a row of tiles as tall as the tallest, the
        // reserve's chips stand on one line, and what is above them keeps its place.
        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            // The header reaches into the tile's end margin, so its arrow costs the name almost
            // none of its width: "Restaurants" stays whole beside its badge on a phone.
            Column {
                CapHeader(icon, name, onOpen = onLongClick.takeIf { enabled })
                Spacer(Modifier.height(8.dp))
                CapBody(figure, figureValue, over, detail, dateLine, bar, mark, ahead, pace)
            }
            if (reserve != null) ReserveChip(reserve, Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp))
        }
    }
}

/** Under a tile's header: the figure and its word, the cap and its day, the bar, the pace. */
@Composable
private fun CapBody(
    figure: String,
    figureValue: Long,
    over: Boolean,
    detail: String,
    dateLine: String?,
    bar: Share,
    mark: Share?,
    ahead: Boolean,
    pace: String?,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        CapFigure(figure, figureValue, over)
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (dateLine != null) {
            Text(
                dateLine,
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        CapBar(bar = bar, mark = mark, over = over, ahead = ahead)
        if (pace != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                pace,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A tile's header: the icon, the name and, where the tile can be touched ([onOpen]), the arrow
 * that says the cap has a page of its own; the header opens it. Its own touch, inside the tile's:
 * TalkBack reads the tile as one control and offers the same page by name.
 */
@Composable
private fun CapHeader(icon: ImageVector, name: String, onOpen: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = if (onOpen != null) 4.dp else 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CapBadge(icon)
        Spacer(Modifier.size(12.dp))
        CapName(name, Modifier.weight(1f, fill = false))
        if (onOpen != null) {
            Icon(
                TettoIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp).size(18.dp),
            )
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
        Row(
            Modifier.padding(start = 16.dp, end = if (enabled) 4.dp else 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            // The arrow to the cap's sheet, as on a tile (owner, 6 Nov 2026).
            if (enabled) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        TettoIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
 * A cap's figure with the word that says what it is, on a tile, on one line and their baseline:
 * "ancora 12 €" where the language puts the word first, "€12 left" where it follows. The word is
 * measured first and the figure takes the rest, stepping down only where a long amount would not
 * fit beside it ([RollingText]): one line always, never broken, so every tile is as tall whatever
 * the amount, and the bars of a row stand level.
 */
@Composable
private fun CapFigure(figure: String, figureValue: Long, over: Boolean) {
    val first = booleanResource(R.bool.cap_word_first)
    Row(Modifier.fillMaxWidth()) {
        if (first) {
            FigureWord(over, Modifier.alignByBaseline())
            Spacer(Modifier.width(6.dp))
        }
        RollingText(
            text = figure,
            value = figureValue,
            style = TettoTheme.type.readingValue,
            color = figureInk(over),
            modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
        )
        if (!first) {
            Spacer(Modifier.width(6.dp))
            FigureWord(over, Modifier.alignByBaseline())
        }
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

/**
 * The reserve, as a small chip in the tertiary container: a role that is amber in neither dress,
 * so the chip never reads as the attention ink of a bar ahead of the days (owner, 6 Nov 2026).
 */
@Composable
private fun ReserveChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.tabular(),
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** A row's figure line: the reading value's 24 sp on 28, not the tile's 32. */
private val RowFigureLine = 28.sp

/**
 * An ended one-off cap's tile (owner, 6 Nov 2026), after the running caps and muted: its icon in a quiet
 * badge, its [name], when it ended ([ended], "Ended on 20 Nov") and what its dates spent of the cap
 * ([detail], "€720 of €800", or with how far over: over is said in words), its bar in a quiet ink.
 * A touch opens its sheet (no numpad: its dates are over); TalkBack reads [description] and offers
 * the sheet by [clickLabel].
 */
@Composable
fun EndedCapTile(
    icon: ImageVector,
    name: String,
    ended: String,
    detail: String,
    bar: Share,
    over: Boolean,
    description: String,
    clickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.endedTouch(description, clickLabel, onClick),
        shape = GroupShape,
        // No outline: an outline is «New cap»'s, the one thing to touch to make something; an
        // ended cap is set back by its quieter ground, its badge and its bar instead.
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietBadge(icon)
                Spacer(Modifier.size(12.dp))
                CapName(name, Modifier.weight(1f, fill = false))
                Icon(
                    TettoIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp).size(18.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                EndedWords(ended, detail, over)
                Spacer(Modifier.height(10.dp))
                CapBar(bar = bar, mark = null, over = over, ahead = false, muted = true)
            }
        }
    }
}

/**
 * An ended one-off cap's row in the list: [EndedCapTile]'s words across the page, as short as a
 * running cap's row (owner, 7 Nov 2026): the name, then when it ended and what its dates spent on
 * one line ("Ended on 4 Nov · €280 of €300", wrapping only where the text is large), then the bar.
 */
@Composable
fun EndedCapRow(
    icon: ImageVector,
    name: String,
    ended: String,
    detail: String,
    bar: Share,
    over: Boolean,
    description: String,
    clickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.endedTouch(description, clickLabel, onClick),
        shape = GroupShape,
        // No outline: an outline is «New cap»'s, the one thing to touch to make something; an
        // ended cap is set back by its quieter ground, its badge and its bar instead.
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuietBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                CapName(name)
                EndedLine(ended, detail, over)
                Spacer(Modifier.height(4.dp))
                CapBar(bar = bar, mark = null, over = over, ahead = false, muted = true)
            }
            Icon(
                TettoIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

/** "Ended on 20 Nov" over "€720 of €800": the second in the over ink when it says over. */
@Composable
private fun EndedWords(ended: String, detail: String, over: Boolean) {
    Text(
        ended,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        detail,
        style = MaterialTheme.typography.bodyMedium.tabular(),
        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** "Ended on 20 Nov · €720 of €800" as one line, its figures in the over ink when they say over. */
@Composable
private fun EndedLine(ended: String, detail: String, over: Boolean) {
    val separator = stringResource(R.string.ended_separator)
    val error = MaterialTheme.colorScheme.error
    Text(
        buildAnnotatedString {
            append(ended)
            append(separator)
            if (over) withStyle(SpanStyle(color = error)) { append(detail) } else append(detail)
        },
        style = MaterialTheme.typography.bodyMedium.tabular(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A cap's icon in a quiet circle: an ended cap's, set back from the others' tinted badges. */
@Composable
private fun QuietBadge(icon: ImageVector) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** An ended cap's touch: its sheet, named; one sentence for TalkBack. */
private fun Modifier.endedTouch(description: String, clickLabel: String, onClick: () -> Unit): Modifier =
    clip(GroupShape)
        .clickable(onClick = onClick)
        .clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick(label = clickLabel) {
                onClick()
                true
            }
        }
