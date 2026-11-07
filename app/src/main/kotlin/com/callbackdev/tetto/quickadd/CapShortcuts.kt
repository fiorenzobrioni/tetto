package com.callbackdev.tetto.quickadd

import android.content.Context
import android.graphics.Canvas
import android.os.PersistableBundle
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.callbackdev.tetto.R
import com.callbackdev.tetto.core.data.caps.CapsRepository
import com.callbackdev.tetto.core.data.settings.SettingsRepository
import com.callbackdev.tetto.core.data.widget.QuickAdd
import com.callbackdev.tetto.core.designsystem.icons.CapIconDrawing
import com.callbackdev.tetto.core.designsystem.theme.widgetDress
import com.callbackdev.tetto.core.model.AppPalette
import com.callbackdev.tetto.core.model.CapIcon
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** One cap as a shortcut shows it: its id, its name, its icon. */
data class ShortcutCap(val id: Long, val name: String, val icon: CapIcon)

/**
 * The launcher's shortcuts (PLANNING.md §8): the first four caps in the reader's order, each its
 * icon and name, straight to its numpad over the home screen. Dynamic, not declared in XML: a
 * static shortcut names its package, and the debug build's is another.
 *
 * Published again when the caps, their order, their names or their icons change (the repository's
 * flow, for as long as the process lives: it costs nothing until something is written), and when
 * the app's language changes ([refresh]), since the long label is in words. The launcher keeps
 * them, with the words they were given: a list that would change nothing is never sent, so the
 * launcher's rate limit is spent only on a change.
 */
@Singleton
class CapShortcuts
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val caps: CapsRepository,
    private val settings: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var last: Pair<List<ShortcutCap>, AppPalette>? = null

    fun start() {
        scope.launch {
            combine(caps.states, settings.settings) { states, s ->
                // An ended one-off takes no spend: never a shortcut (owner, 6 Nov 2026).
                states.filterNot { it.ended }.map { ShortcutCap(it.cap.id, it.cap.name, it.cap.icon) } to s.palette
            }
                .distinctUntilChanged()
                .collect { (list, palette) ->
                    last = list to palette
                    publish(context, list, palette)
                }
        }
    }

    /** The app's language changed: the same caps, in the new words. */
    fun refresh() {
        val (list, palette) = last ?: return
        scope.launch { publish(context, list, palette) }
    }

    companion object {
        /** The launcher's long press holds four comfortably, and four is what VISION asks. */
        const val MAX: Int = 4

        private const val EXTRA_ICON = "icon"
        private const val EXTRA_PALETTE = "palette"

        /** The first [MAX] of [caps] as the launcher's shortcuts, unless they are already its. */
        fun publish(context: Context, caps: List<ShortcutCap>, palette: AppPalette = AppPalette.VIVID) {
            val limit =
                minOf(
                    MAX,
                    runCatching {
                        ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
                    }.getOrDefault(MAX),
                )
            val shortcuts = caps.take(limit).mapIndexed { rank, cap ->
                ShortcutInfoCompat.Builder(context, idOf(cap.id))
                    .setShortLabel(cap.name)
                    .setLongLabel(context.getString(R.string.shortcut_add_long, cap.name))
                    .setIcon(icon(context, cap.icon, palette))
                    .setIntent(QuickAdd.intent(context, cap.id))
                    .setRank(rank)
                    .setExtras(
                        PersistableBundle().apply {
                            putString(EXTRA_ICON, cap.icon.key)
                            putString(EXTRA_PALETTE, palette.name)
                        },
                    )
                    .build()
            }
            val published = runCatching { ShortcutManagerCompat.getDynamicShortcuts(context) }.getOrDefault(emptyList())
            if (published.map(::signature) == shortcuts.map(::signature)) return
            try {
                ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
                // A cap deleted since: its shortcut, pinned somewhere, says so rather than opening on nothing.
                val gone = published.map { it.id } - shortcuts.map { it.id }.toSet()
                if (gone.isNotEmpty()) {
                    ShortcutManagerCompat.disableShortcuts(context, gone, context.getString(R.string.shortcut_gone))
                }
            } catch (_: IllegalStateException) {
                // The launcher's rate limit: the next change, or the next start, publishes them.
            }
        }

        internal fun idOf(capId: Long): String = "cap_$capId"

        private fun signature(info: ShortcutInfoCompat): List<String?> = listOf(
            info.id,
            info.shortLabel.toString(),
            info.longLabel?.toString(),
            info.extras?.getString(EXTRA_ICON),
            info.extras?.getString(EXTRA_PALETTE),
            info.rank.toString(),
        )

        /**
         * The cap's symbol on the dress's container, as the caps screen's badge draws it: an
         * adaptive bitmap, so the launcher gives it its own shape; the symbol inside the safe zone.
         */
        private fun icon(context: Context, icon: CapIcon, palette: AppPalette): IconCompat {
            val scheme = widgetDress(palette).lightScheme
            val density = context.resources.displayMetrics.density
            val side = (ADAPTIVE_DP * density).roundToInt()
            val bitmap = createBitmap(side, side)
            val canvas = Canvas(bitmap)
            canvas.drawColor(scheme.primaryContainer.toArgb())
            val glyph = side * GLYPH_SHARE
            val corner = (side - glyph) / 2f
            CapIconDrawing.draw(canvas, icon, corner, corner, glyph, scheme.onPrimaryContainer.toArgb())
            return IconCompat.createWithAdaptiveBitmap(bitmap)
        }

        /** An adaptive icon's full side, and the symbol's share of it, inside its 72 dp safe zone. */
        private const val ADAPTIVE_DP = 108f
        private const val GLYPH_SHARE = 0.4f
    }
}
