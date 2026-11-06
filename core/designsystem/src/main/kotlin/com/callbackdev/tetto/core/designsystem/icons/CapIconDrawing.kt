package com.callbackdev.tetto.core.designsystem.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import androidx.core.graphics.createBitmap
import com.callbackdev.tetto.core.model.CapIcon
import java.util.concurrent.ConcurrentHashMap

/**
 * A cap's symbol on an Android canvas, for what Compose does not draw: a home-screen card's
 * bitmaps and a launcher shortcut's icon. The same path data as [symbol] ([SymbolPaths]), so the
 * drawing is one wherever the cap is shown.
 */
object CapIconDrawing {
    private val parsed = ConcurrentHashMap<CapIcon, Path>()

    /** The symbol on its 960-unit grid, y from -960 to 0, parsed once. */
    private fun pathOf(icon: CapIcon): Path = parsed.getOrPut(icon) { PathParser.createPathFromPathData(icon.pathData) }

    /** Draws [icon] filling the square at [left], [top], [side] pixels a side, in [color]. */
    fun draw(canvas: Canvas, icon: CapIcon, left: Float, top: Float, side: Float, color: Int) {
        val scale = side / GRID
        val path = Path(pathOf(icon)).apply {
            transform(
                Matrix().apply {
                    setScale(scale, scale)
                    // The grid's y runs from -960 to 0: down by the side, then to the square.
                    postTranslate(left, top + side)
                },
            )
        }
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    /** [icon] alone in a bitmap [sidePx] a side, in [color], on nothing. */
    fun bitmap(icon: CapIcon, sidePx: Int, color: Int): Bitmap {
        val side = sidePx.coerceAtLeast(1)
        return createBitmap(side, side).also { draw(Canvas(it), icon, 0f, 0f, side.toFloat(), color) }
    }

    private const val GRID = 960f
}
