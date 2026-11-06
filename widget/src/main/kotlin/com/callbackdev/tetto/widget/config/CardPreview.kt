package com.callbackdev.tetto.widget.config

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.callbackdev.tetto.core.model.AppCurrency
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.R
import com.callbackdev.tetto.widget.WidgetCornerRes
import com.callbackdev.tetto.widget.WidgetKind
import com.callbackdev.tetto.widget.WidgetModel
import com.callbackdev.tetto.widget.glance.GlanceCardContent
import com.callbackdev.tetto.widget.words.WordsCardContent
import java.time.LocalDate

/**
 * A launcher grant, named by its cells: the family's reference sizes, the ones the layout tests
 * measure against, so what a chip shows is what the tests hold.
 */
internal enum class PreviewSize(val label: String, val size: DpSize) {
    ONE_BY_ONE("1×1", DpSize(85.dp, 85.dp)),
    TWO_BY_ONE("2×1", DpSize(159.dp, 85.dp)),
    FOUR_BY_ONE("4×1", DpSize(340.dp, 85.dp)),
    TWO_BY_TWO("2×2", DpSize(159.dp, 189.dp)),
    FOUR_BY_TWO("4×2", DpSize(340.dp, 189.dp)),
    FOUR_BY_THREE("4×3", DpSize(340.dp, 293.dp)),
}

/** Where a card of [kind] lands when first placed: its provider's `targetCell*`. */
internal fun defaultPreviewSize(kind: WidgetKind): PreviewSize = when (kind) {
    WidgetKind.GLANCE -> PreviewSize.FOUR_BY_TWO
    WidgetKind.WORDS -> PreviewSize.FOUR_BY_ONE
}

/**
 * The size this card really has on the home screen in portrait (the launcher's
 * `minWidth × maxHeight`), or null before its first layout.
 */
internal fun placedWidgetSize(context: Context, appWidgetId: Int): DpSize? {
    val options = runCatching { AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId) }.getOrNull()
        ?: return null
    val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
    return if (width > 0 && height > 0) DpSize(width.dp, height.dp) else null
}

/**
 * The card as it will look, on a ground that stands in for a wallpaper, with the sizes it can be
 * given underneath. It opens on the size the card really has when the launcher has said it: the
 * reader's own card first.
 */
@Composable
internal fun PreviewSection(kind: WidgetKind, model: WidgetModel?, placed: DpSize?, sample: Boolean) {
    var size by remember { mutableStateOf(placed ?: defaultPreviewSize(kind).size) }
    PreviewStage { CardPreview(kind, model, size) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag(WidgetConfigTags.SIZES),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        placed?.let { here ->
            FilterChip(
                selected = size == here,
                onClick = { size = here },
                label = { Text(stringResource(R.string.widget_config_size_placed)) },
            )
        }
        PreviewSize.entries.forEach { option ->
            FilterChip(
                selected = size == option.size && size != placed,
                onClick = { size = option.size },
                label = { Text(option.label) },
            )
        }
    }
    if (sample) {
        Text(
            text = stringResource(R.string.widget_config_preview_sample),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
        )
    }
    Text(
        text = stringResource(R.string.widget_config_resize_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
    )
}

/** The ground a preview stands on: the theme's containers, standing in for a wallpaper. */
@Composable
internal fun PreviewStage(content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer)))
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        content()
    }
}

/**
 * The card itself, not a lookalike: Glance composes the same content function the receiver runs
 * into `RemoteViews` for the chosen size, and `RemoteViews.apply` inflates them as the launcher
 * does. The host swallows every touch, so the card's own doors never open from a preview, and it
 * is one picture to a screen reader.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
internal fun CardPreview(kind: WidgetKind, model: WidgetModel?, size: DpSize, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var views by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(kind, model, size) {
        if (model == null) return@LaunchedEffect
        views = runCatching {
            GlanceRemoteViews().compose(context, size) {
                when (kind) {
                    WidgetKind.GLANCE -> GlanceCardContent(model)
                    WidgetKind.WORDS -> WordsCardContent(model)
                }
            }.remoteViews
        }.getOrNull()
    }
    val description = stringResource(R.string.widget_config_preview_desc)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().semantics { contentDescription = description }) {
        val scale = minOf(1f, maxWidth / size.width)
        Box(modifier = Modifier.fillMaxWidth().height(size.height * scale), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .requiredSize(size.width, size.height)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(dimensionResource(WidgetCornerRes))),
            ) {
                views?.let { remote ->
                    AndroidView(
                        factory = { TouchlessFrame(it) },
                        update = { frame ->
                            frame.removeAllViews()
                            runCatching { remote.apply(frame.context, frame) }.getOrNull()?.let { frame.addView(it) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@SuppressLint("ViewConstructor")
private class TouchlessFrame(context: Context) : FrameLayout(context) {
    init {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = true
}

/**
 * «At a glance» as the reader's [caps] stand on the home screen, on the family's default card, at
 * the size it is placed at: the first run's last page, where the launcher can place one
 * (PLANNING.md §8, the pin request). The card itself, as [CardPreview] draws it.
 */
@Composable
fun HomeCardPreview(caps: List<CapState>, currency: AppCurrency, settings: UserSettings, today: LocalDate) {
    val model = remember(caps, currency, settings, today) {
        WidgetModel(look = WidgetLook(), settings = settings, currency = currency, today = today, caps = caps)
    }
    PreviewStage { CardPreview(WidgetKind.GLANCE, model, defaultPreviewSize(WidgetKind.GLANCE).size) }
}
