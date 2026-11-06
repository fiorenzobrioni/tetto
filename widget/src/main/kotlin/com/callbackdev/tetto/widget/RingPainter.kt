package com.callbackdev.tetto.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.callbackdev.tetto.core.designsystem.icons.CapIconDrawing
import com.callbackdev.tetto.core.model.CapIcon
import com.callbackdev.tetto.core.model.CapState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What a cap's ring shows (PLANNING.md §7): what was spent of the cap and its reserve ([spent],
 * 0 to 1), where today falls in the period ([mark], none for a day's cap or one over), whether
 * spending runs [ahead] of the days, whether it is [over], and the cap's [icon] in the middle.
 */
internal data class RingSpec(
    val spent: Float,
    val mark: Float?,
    val ahead: Boolean,
    val over: Boolean,
    val icon: CapIcon,
) {
    companion object {
        fun of(state: CapState): RingSpec = RingSpec(
            spent = if (state.over) 1f else (state.bar.part.toDouble() / state.bar.whole).toFloat().coerceIn(0f, 1f),
            mark = state.todayMark?.takeIf {
                !state.over
            }?.let { (it.part.toDouble() / it.whole).toFloat().coerceIn(0f, 1f) },
            ahead = state.aheadOfDays,
            over = state.over,
            icon = state.cap.icon,
        )
    }
}

/** The ring's colours, from the card's [WidgetPalette]. */
internal data class RingInks(
    val accent: Color,
    val attention: Color,
    val over: Color,
    val track: Color,
    val mark: Color,
    val icon: Color,
    val halo: Color?,
) {
    companion object {
        fun of(palette: WidgetPalette): RingInks = RingInks(
            accent = palette.accent,
            attention = palette.attention,
            over = palette.over,
            track = palette.track,
            mark = palette.primary,
            icon = palette.primary,
            halo = palette.halo,
        )
    }
}

/**
 * A cap's ring, painted into a bitmap of exactly the size it is shown at, with the cap's icon in
 * its middle: the one drawing a card cannot build from boxes (an arc with round caps, in any of the
 * card's inks, the mark for today at any day of the period).
 *
 * Kept small on purpose: Android caps the bitmap memory of a `RemoteViews`, and exceeding it is a
 * crash. One ring is `side² × 4` bytes and [MAX_SIDE_PX] caps the side, so the largest ring a card
 * can ask for is 0.7 MB and a one-row ring about 50 KB, one bitmap per card; a sharper ring past
 * that size would not be visible anyway.
 */
internal object RingPainter {
    /** The largest side painted: a 104 dp ring on a 4.0 density screen. */
    const val MAX_SIDE_PX: Int = 416

    /** The stroke, as a share of the ring's side: a card is read from further away than a screen. */
    const val STROKE_SHARE: Float = 0.11f

    /** The icon, as a share of the ring's side, inside the hole the stroke leaves. */
    const val ICON_SHARE: Float = 0.42f

    fun strokeOf(side: Dp): Dp = maxOf(side * STROKE_SHARE, MIN_STROKE)

    fun paint(sideDp: Dp, density: Float, spec: RingSpec, inks: RingInks): Bitmap {
        val side = (sideDp.value * density).roundToInt().coerceIn(1, MAX_SIDE_PX)
        val px = side / sideDp.value.coerceAtLeast(1f)
        val bitmap = createBitmap(side, side)
        val canvas = Canvas(bitmap)
        val stroke = strokeOf(sideDp).value * px
        val inset = stroke / 2f
        val oval = RectF(inset, inset, side - inset, side - inset)
        val radius = (side - stroke) / 2f
        val center = side / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
        }
        paint.color = inks.track.toArgb()
        canvas.drawArc(oval, 0f, 360f, false, paint)

        val share = spec.spent.coerceIn(0f, 1f)
        if (share > 0f) {
            paint.color = when {
                spec.over -> inks.over
                spec.ahead -> inks.attention
                else -> inks.accent
            }.toArgb()
            paint.strokeCap = if (share >= 1f) Paint.Cap.BUTT else Paint.Cap.ROUND
            canvas.drawArc(oval, -90f, 360f * share, false, paint)
        }

        val mark = spec.mark
        if (mark != null && mark > 0f && mark < 1f) {
            val angle = (mark * 360f - 90f) * DEG
            val reach = stroke / 2f + MARK_OVERHANG.value * px
            val fromX = center + (radius - reach) * cos(angle)
            val fromY = center + (radius - reach) * sin(angle)
            val toX = center + (radius + min(reach, inset)) * cos(angle)
            val toY = center + (radius + min(reach, inset)) * sin(angle)
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            inks.halo?.let { halo ->
                line.color = halo.toArgb()
                line.strokeWidth = MARK_HALO.value * px
                canvas.drawLine(fromX, fromY, toX, toY, line)
            }
            line.color = inks.mark.toArgb()
            line.strokeWidth = MARK_WIDTH.value * px
            canvas.drawLine(fromX, fromY, toX, toY, line)
        }

        val icon = minOf(side * ICON_SHARE, MAX_ICON.value * px)
        val corner = center - icon / 2f
        CapIconDrawing.draw(canvas, spec.icon, corner, corner, icon, inks.icon.toArgb())
        return bitmap
    }

    private val MIN_STROKE = 3.dp
    private val MARK_WIDTH = 2.dp
    private val MARK_HALO = 5.dp
    private val MARK_OVERHANG = 3.dp
    private val MAX_ICON = 48.dp
    private const val DEG = (PI / 180).toFloat()
}

/** A cap's icon alone, for a slot or an item of «At a glance»: a bitmap at the size shown. */
internal fun iconBitmap(icon: CapIcon, sideDp: Dp, density: Float, color: Color): Bitmap = CapIconDrawing.bitmap(
    icon,
    (sideDp.value * density).roundToInt().coerceIn(1, RingPainter.MAX_SIDE_PX),
    color.toArgb(),
)
