package com.callbackdev.tetto.feature.caps

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.GroupShape
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.pageGutter

/** A cap as the order shows it: its icon and its name. */
data class ArrangeItem(val id: Long, val name: String, val icon: ImageVector)

/** The order (PLANNING.md §11 Phase 2), from "Change the order" under the tiles. */
@Composable
fun ArrangeCapsRoute(onBack: () -> Unit, viewModel: CapsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val caps = state?.caps ?: return
    ArrangeCapsScreen(
        items = caps.map { ArrangeItem(it.cap.id, it.cap.name, it.cap.icon.symbol) },
        onOrder = viewModel::reorder,
        onDone = onBack,
    )
}

/**
 * The caps in a list, each dragged by its handle; TalkBack moves them a place at a time. Every
 * drop and every move is written at once ([onOrder]), so leaving by Done or by back keeps it.
 */
@Composable
fun ArrangeCapsScreen(
    items: List<ArrangeItem>,
    onOrder: (List<Long>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = remember(items.map { it.id }.toSet()) { items.toMutableStateList() }
    val listState = rememberLazyListState()
    var dragged by remember { mutableStateOf<Long?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val gutter = pageGutter()
    val moveUp = stringResource(R.string.arrange_move_up)
    val moveDown = stringResource(R.string.arrange_move_down)

    fun move(from: Int, to: Int) {
        if (to !in order.indices || from == to) return
        order.add(to, order.removeAt(from))
    }

    Column(
        modifier
            .fillMaxSize()
            .testTag(CapsTags.ARRANGE)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
    ) {
        Row(
            Modifier.padding(start = gutter.start + 4.dp, end = gutter.end + ScreenMargin, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDone) {
                Icon(TettoIcons.Back, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.change_order),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            stringResource(R.string.arrange_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = gutter.start + ScreenMargin,
                end = gutter.end + ScreenMargin,
                top = 8.dp,
                bottom = 8.dp,
            ),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).testTag(CapsTags.ARRANGE_LIST),
            contentPadding = gutter.contentPadding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(order, key = { _, item -> item.id }) { index, item ->
                val isDragged = dragged == item.id
                Surface(
                    shape = GroupShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .padding(horizontal = ScreenMargin)
                        .then(if (isDragged) Modifier else Modifier.animateItem())
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragged) offset else 0f }
                        .then(if (isDragged) Modifier.shadow(6.dp, GroupShape) else Modifier)
                        .semantics(mergeDescendants = true) {
                            customActions = listOfNotNull(
                                CustomAccessibilityAction(moveUp) {
                                    move(index, index - 1)
                                    onOrder(order.map { it.id })
                                    true
                                }.takeIf { index > 0 },
                                CustomAccessibilityAction(moveDown) {
                                    move(index, index + 1)
                                    onOrder(order.map { it.id })
                                    true
                                }.takeIf { index < order.lastIndex },
                            )
                        },
                ) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CapBadge(item.icon)
                        Spacer(Modifier.size(12.dp))
                        Text(item.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(56.dp)
                                .pointerInput(item.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragged = item.id
                                            offset = 0f
                                        },
                                        onDragEnd = {
                                            dragged = null
                                            offset = 0f
                                            onOrder(order.map { it.id })
                                        },
                                        onDragCancel = {
                                            dragged = null
                                            offset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            offset += amount.y
                                            val at = order.indexOfFirst { it.id == item.id }
                                            val visible = listState.layoutInfo.visibleItemsInfo
                                            val next = visible.firstOrNull { it.index == at + 1 }
                                            val previous = visible.firstOrNull { it.index == at - 1 }
                                            if (next != null && offset > next.size / 2f) {
                                                move(at, at + 1)
                                                offset -= next.size + listState.layoutInfo.mainAxisItemSpacing
                                            } else if (previous != null && -offset > previous.size / 2f) {
                                                move(at, at - 1)
                                                offset += previous.size + listState.layoutInfo.mainAxisItemSpacing
                                            }
                                        },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                TettoIcons.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = gutter.start + ScreenMargin, end = gutter.end + ScreenMargin, bottom = 16.dp)
                .heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.arrange_done))
        }
    }
}
