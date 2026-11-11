package com.callbackdev.tetto.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app's line icons, drawn here on a 24-unit grid: one 1.8 stroke, round caps and joins, the
 * weight of Material Symbols' outlined set, so they sit beside Material's own components
 * (`docs/adr/0002-design-language.md`); a shape Tetto needs is drawn here the same way. Drawn rather than taken from a library:
 * a dozen shapes did not justify a new dependency, and every one of them is geometry a reader can check.
 */
object TettoIcons {
    val Settings: ImageVector by lazy {
        icon("settings") {
            // A gear of eight teeth around a hub: points computed, not traced.
            val teeth = 8
            for (i in 0 until teeth * 4) {
                val angle = 2 * PI * i / (teeth * 4) - PI / 2
                val radius = if ((i / 2) % 2 == 0) 9.6 else 7.4
                val x = (12 + radius * cos(angle)).toFloat()
                val y = (12 + radius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
            circle(12f, 12f, 3f)
        }
    }

    val Back: ImageVector by lazy {
        icon("back", autoMirror = true) {
            moveTo(19f, 12f)
            lineTo(5f, 12f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("chevron_right", autoMirror = true) {
            moveTo(9.5f, 6f)
            lineTo(15.5f, 12f)
            lineTo(9.5f, 18f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        icon("chevron_left", autoMirror = true) {
            moveTo(14.5f, 6f)
            lineTo(8.5f, 12f)
            lineTo(14.5f, 18f)
        }
    }

    val Check: ImageVector by lazy {
        icon("check") {
            moveTo(5f, 12.5f)
            lineTo(10f, 17.5f)
            lineTo(19f, 7f)
        }
    }

    val Plus: ImageVector by lazy {
        icon("plus") {
            moveTo(12f, 5f)
            lineTo(12f, 19f)
            moveTo(5f, 12f)
            lineTo(19f, 12f)
        }
    }

    val Info: ImageVector by lazy {
        icon("info") {
            circle(12f, 12f, 9f)
            moveTo(12f, 11f)
            lineTo(12f, 16.5f)
            moveTo(12f, 7.6f)
            lineTo(12f, 7.7f)
        }
    }

    val Warning: ImageVector by lazy {
        icon("warning") {
            moveTo(12f, 3.8f)
            lineTo(21f, 19.8f)
            lineTo(3f, 19.8f)
            close()
            moveTo(12f, 9.5f)
            lineTo(12f, 14f)
            moveTo(12f, 16.9f)
            lineTo(12f, 17f)
        }
    }

    /** A bell: the evening reminder. */
    val Bell: ImageVector by lazy {
        icon("bell") {
            moveTo(6f, 16.5f)
            lineTo(6f, 11f)
            curveTo(6f, 7.5f, 8.7f, 5f, 12f, 5f)
            curveTo(15.3f, 5f, 18f, 7.5f, 18f, 11f)
            lineTo(18f, 16.5f)
            lineTo(19.5f, 18f)
            lineTo(4.5f, 18f)
            close()
            moveTo(10f, 20.5f)
            curveTo(10.4f, 21.3f, 11.1f, 21.7f, 12f, 21.7f)
            curveTo(12.9f, 21.7f, 13.6f, 21.3f, 14f, 20.5f)
        }
    }

    /** A globe: a second time zone, a city's clock. */
    val Globe: ImageVector by lazy {
        icon("globe") {
            circle(12f, 12f, 9f)
            ellipse(12f, 12f, 4f, 9f)
            moveTo(3f, 12f)
            lineTo(21f, 12f)
        }
    }

    /** A magnifier: a search field. */
    val Search: ImageVector by lazy {
        icon("search") {
            circle(10.5f, 10.5f, 6f)
            moveTo(15f, 15f)
            lineTo(20f, 20f)
        }
    }

    /** A home screen's cards: two small ones over a wide one, the sizes the widgets take. */
    val Widgets: ImageVector by lazy {
        icon("widgets") {
            roundRect(4f, 4f, 11f, 11f, 1.8f)
            roundRect(13f, 4f, 20f, 11f, 1.8f)
            roundRect(4f, 13f, 20f, 20f, 1.8f)
        }
    }

    /** A battery with a bolt: the battery promise. */
    val Battery: ImageVector by lazy {
        icon("battery") {
            moveTo(5f, 7f)
            lineTo(17f, 7f)
            curveTo(18.1f, 7f, 19f, 7.9f, 19f, 9f)
            lineTo(19f, 15f)
            curveTo(19f, 16.1f, 18.1f, 17f, 17f, 17f)
            lineTo(5f, 17f)
            curveTo(3.9f, 17f, 3f, 16.1f, 3f, 15f)
            lineTo(3f, 9f)
            curveTo(3f, 7.9f, 3.9f, 7f, 5f, 7f)
            close()
            moveTo(21.5f, 10.5f)
            lineTo(21.5f, 13.5f)
            moveTo(11.5f, 9f)
            lineTo(8.5f, 12.3f)
            lineTo(12.5f, 12.3f)
            lineTo(9.5f, 15f)
        }
    }

    /** A shield: the privacy statement. */
    val Shield: ImageVector by lazy {
        icon("shield") {
            moveTo(12f, 3f)
            lineTo(19f, 6f)
            lineTo(19f, 11f)
            curveTo(19f, 15.5f, 16f, 19f, 12f, 21f)
            curveTo(8f, 19f, 5f, 15.5f, 5f, 11f)
            lineTo(5f, 6f)
            close()
            moveTo(9f, 12f)
            lineTo(11.2f, 14.2f)
            lineTo(15.2f, 10f)
        }
    }

    /** The numpad's delete key: a key cap pointing back, with a cross. */
    val Backspace: ImageVector by lazy {
        icon("backspace", autoMirror = true) {
            moveTo(8.5f, 5.5f)
            lineTo(19f, 5.5f)
            curveTo(20.1f, 5.5f, 21f, 6.4f, 21f, 7.5f)
            lineTo(21f, 16.5f)
            curveTo(21f, 17.6f, 20.1f, 18.5f, 19f, 18.5f)
            lineTo(8.5f, 18.5f)
            lineTo(3f, 12f)
            close()
            moveTo(11.5f, 9.5f)
            lineTo(16.5f, 14.5f)
            moveTo(16.5f, 9.5f)
            lineTo(11.5f, 14.5f)
        }
    }

    /** What a row is dragged by: two columns of three dots. */
    val DragHandle: ImageVector by lazy {
        icon("drag_handle") {
            for (x in listOf(9.5f, 14.5f)) {
                for (y in listOf(7f, 12f, 17f)) circle(x, y, 0.6f)
            }
        }
    }

    /**
     * A roof: two slopes from the ridge and a chimney standing on the right one, at the line
     * icons' weight. A cap, where a glyph names caps (the caps screen's tip, the guide's chapter
     * on it): a cap is a tetto, a roof. It stays a roof while the app's mark is a house (owner,
     * 6 Nov 2026): it names a cap, not the app.
     */
    val Roof: ImageVector by lazy {
        icon("roof") {
            moveTo(3.5f, 16.7f)
            lineTo(12f, 9.1f)
            lineTo(20.5f, 16.7f)
            moveTo(16.9f, 7.3f)
            lineTo(16.9f, 13.5f)
        }
    }

    /**
     * An arrow round a circle that starts again where it began: a period that ends and the next
     * one. Five sixths of the circle, clockwise from the top right, its head at the top left.
     */
    val Renew: ImageVector by lazy {
        icon("renew") {
            moveTo(15.5f, 5.94f)
            arcTo(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 8.5f, y1 = 5.94f)
            moveTo(5.01f, 5.64f)
            lineTo(8.5f, 5.94f)
            lineTo(7.02f, 9.11f)
        }
    }

    /**
     * Money back (owner, 7 Nov 2026): an arrow that turns back on itself, its head pointing to the
     * start, as a return is. Its own shape, so a refund never wears the renewal's circle.
     */
    val Refund: ImageVector by lazy {
        icon("refund", autoMirror = true) {
            // The head, pointing back to the left.
            moveTo(9f, 5f)
            lineTo(4.5f, 9.5f)
            lineTo(9f, 14f)
            // The shaft, out to the right and round, coming back under itself.
            moveTo(4.5f, 9.5f)
            lineTo(14.5f, 9.5f)
            arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 14.5f, y1 = 19.5f)
            lineTo(10.5f, 19.5f)
        }
    }

    /** The numpad: three rows of three keys and the zero, as dots. */
    val Keypad: ImageVector by lazy {
        icon("keypad") {
            for (y in listOf(4.5f, 9.5f, 14.5f)) {
                for (x in listOf(7f, 12f, 17f)) circle(x, y, 0.9f)
            }
            circle(12f, 19.5f, 0.9f)
        }
    }

    /** A ledger: a bound book with two ruled lines, what Tetto does not keep. */
    val Ledger: ImageVector by lazy {
        icon("ledger") {
            roundRect(5.5f, 3.5f, 18.5f, 20.5f, 2f)
            moveTo(9f, 3.5f)
            lineTo(9f, 20.5f)
            moveTo(12f, 8.5f)
            lineTo(15.5f, 8.5f)
            moveTo(12f, 12f)
            lineTo(15.5f, 12f)
        }
    }

    /** A bin: deleting a spend. */
    val Trash: ImageVector by lazy {
        icon("trash") {
            moveTo(4.5f, 6.5f)
            lineTo(19.5f, 6.5f)
            moveTo(9.5f, 6.5f)
            lineTo(9.5f, 4.5f)
            lineTo(14.5f, 4.5f)
            lineTo(14.5f, 6.5f)
            moveTo(6.5f, 6.5f)
            lineTo(7.5f, 19.5f)
            lineTo(16.5f, 19.5f)
            lineTo(17.5f, 6.5f)
            moveTo(10.5f, 10f)
            lineTo(10.5f, 16f)
            moveTo(13.5f, 10f)
            lineTo(13.5f, 16f)
        }
    }

    /** A calendar page with two rings and a marked range of days: a one-off cap's dates. */
    val Calendar: ImageVector by lazy {
        icon("calendar") {
            roundRect(4f, 5.5f, 20f, 20f, 2.5f)
            moveTo(4f, 10f)
            lineTo(20f, 10f)
            moveTo(8.5f, 3.5f)
            lineTo(8.5f, 7f)
            moveTo(15.5f, 3.5f)
            lineTo(15.5f, 7f)
            moveTo(8f, 14.5f)
            lineTo(16f, 14.5f)
        }
    }

    /** A closed padlock: the lock in Settings, and the page that waits for it. */
    val Lock: ImageVector by lazy {
        icon("lock") {
            roundRect(5f, 10.5f, 19f, 20.5f, 2.5f)
            moveTo(8f, 10.5f)
            lineTo(8f, 7.5f)
            curveTo(8f, 5.3f, 9.8f, 3.5f, 12f, 3.5f)
            curveTo(14.2f, 3.5f, 16f, 5.3f, 16f, 7.5f)
            lineTo(16f, 10.5f)
            moveTo(12f, 14.5f)
            lineTo(12f, 16.5f)
        }
    }

    /** A cross: closes a page that covers the screen (a one-off cap's dates). */
    val Close: ImageVector by lazy {
        icon("close") {
            moveTo(6f, 6f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(6f, 18f)
        }
    }

    /** A pencil at work: a cap's editor, from its sheet. */
    val Edit: ImageVector by lazy {
        icon("edit") {
            moveTo(4.5f, 19.5f)
            lineTo(8.5f, 19.5f)
            lineTo(19f, 9f)
            lineTo(15f, 5f)
            lineTo(4.5f, 15.5f)
            close()
            moveTo(12.8f, 7.2f)
            lineTo(16.8f, 11.2f)
        }
    }

    /** An arrow rising out of a tray: a file written for the reader to keep (the backup). */
    val Export: ImageVector by lazy {
        icon("export") {
            tray()
            moveTo(12f, 15f)
            lineTo(12f, 3.5f)
            moveTo(7.5f, 8f)
            lineTo(12f, 3.5f)
            lineTo(16.5f, 8f)
        }
    }

    /** An arrow coming down into a tray: a file taken back in (the restore). */
    val Import: ImageVector by lazy {
        icon("import") {
            tray()
            moveTo(12f, 3.5f)
            lineTo(12f, 15f)
            moveTo(7.5f, 10.5f)
            lineTo(12f, 15f)
            lineTo(16.5f, 10.5f)
        }
    }

    /** A sheet ruled in rows and columns: a table for a spreadsheet (the CSV). */
    val Table: ImageVector by lazy {
        icon("table") {
            roundRect(4f, 4f, 20f, 20f, 2f)
            moveTo(4f, 9.5f)
            lineTo(20f, 9.5f)
            moveTo(4f, 14.8f)
            lineTo(20f, 14.8f)
            moveTo(10f, 9.5f)
            lineTo(10f, 20f)
        }
    }

    /** The tray a file leaves or enters: three sides of a box, open at the top. */
    private fun PathBuilder.tray() {
        moveTo(4.5f, 15.5f)
        lineTo(4.5f, 19.5f)
        lineTo(19.5f, 19.5f)
        lineTo(19.5f, 15.5f)
    }

    private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
        moveTo(left + r, top)
        lineTo(right - r, top)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right, y1 = top + r)
        lineTo(right, bottom - r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right - r, y1 = bottom)
        lineTo(left + r, bottom)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left, y1 = bottom - r)
        lineTo(left, top + r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left + r, y1 = top)
        close()
    }

    private fun icon(name: String, autoMirror: Boolean = false, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = "tetto_$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror,
        ).path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = draw,
        ).build()

    private const val STROKE = 1.8f
}

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) = ellipse(cx, cy, r, r)

private fun PathBuilder.ellipse(cx: Float, cy: Float, rx: Float, ry: Float) {
    moveTo(cx - rx, cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx + rx, y1 = cy)
    arcTo(rx, ry, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = cx - rx, y1 = cy)
    close()
}
