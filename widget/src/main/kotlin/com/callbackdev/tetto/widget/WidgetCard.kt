package com.callbackdev.tetto.widget

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Paint
import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import androidx.annotation.StyleRes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.callbackdev.tetto.core.designsystem.theme.WidgetDress
import com.callbackdev.tetto.core.designsystem.theme.widgetCardContainer
import com.callbackdev.tetto.core.designsystem.theme.widgetDress
import com.callbackdev.tetto.core.domain.widget.WidgetType
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import java.util.Locale

/*
 * The widgets' side of the design system (`docs/adr/0003-widgets.md`): the family's card, its
 * launcher's corner, insets, grounds, inks and type scale; the system face, because a launcher
 * draws `RemoteViews` in it whatever the app's own setting is.
 */

/** The dress a card writes in: the reader's palette, or the wallpaper's schemes when they asked. */
fun widgetDressFor(context: Context, settings: UserSettings): WidgetDress = if (settings.dynamicColor) {
    widgetDress(settings.palette, dynamicLightColorScheme(context), dynamicDarkColorScheme(context))
} else {
    widgetDress(settings.palette)
}

/**
 * The inks and marks a card draws with, resolved once for the ground it really has.
 *
 * @property accent a cap's arc and bar while its spending keeps to the days.
 * @property attention a cap spending ahead of the days: the arc, the bar.
 * @property over a cap past its cap and its reserve: the arc, the bar, and the figure that says
 *   "over" in words, which are never left to it alone.
 * @property track the ring's and the bar's track: the ink, faint, so it reads on any ground, a
 *   see-through card over a wallpaper included.
 * @property halo the card's own ground, opaque, drawn under the mark for today where it crosses
 *   the arc; null on a see-through card, whose ground is the wallpaper.
 */
data class WidgetPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val attention: Color,
    val over: Color,
    val track: Color,
    val halo: Color?,
    val darkGround: Boolean,
) {
    val primaryInk: ColorProvider get() = ColorProvider(primary)
    val secondaryInk: ColorProvider get() = ColorProvider(secondary)
    val overInk: ColorProvider get() = ColorProvider(over)
}

/** [widgetInk]'s answer, dressed in the colours it names. */
fun widgetPalette(context: Context, look: WidgetLook, dress: WidgetDress): WidgetPalette {
    val night = isNight(context)
    val delegates = look.ground == WidgetGround.SYSTEM || look.ground == WidgetGround.COLOR
    val bright = look.opacityPct < INK_TRUST_FLOOR_PCT && delegates && wallpaperWantsDarkInk(context)
    return widgetPaletteFor(widgetInk(look.ground, look.opacityPct, night, bright), look, dress, night)
}

/** The palette for an [ink] already decided: pure, so a test pins every ground's inks. */
fun widgetPaletteFor(ink: WidgetInk, look: WidgetLook, dress: WidgetDress, night: Boolean): WidgetPalette {
    val solid = look.opacityPct >= INK_TRUST_FLOOR_PCT
    val ground = cardGround(look, dress, night)
    return when (ink) {
        WidgetInk.OVER_COLOR -> WidgetPalette(
            primary = Color.White,
            secondary = Color.White.copy(alpha = QUIET_ALPHA),
            accent = Color.White,
            attention = dress.darkColors.attention,
            over = dress.darkScheme.error,
            track = Color.White.copy(alpha = TRACK_ALPHA),
            halo = ground.takeIf { solid },
            darkGround = true,
        )

        WidgetInk.ON_LIGHT, WidgetInk.ON_DARK -> {
            val dark = ink.darkGround
            val scheme = dress.scheme(dark)
            WidgetPalette(
                primary = scheme.onSurface,
                secondary = scheme.onSurfaceVariant,
                accent = scheme.primary,
                attention = dress.colors(dark).attention,
                over = scheme.error,
                track = scheme.onSurface.copy(alpha = TRACK_ALPHA),
                halo = ground.takeIf { solid },
                darkGround = dark,
            )
        }
    }
}

/** The card's ground at full strength: what [widgetCardFill] thins by the reader's opacity. */
fun cardGround(look: WidgetLook, dress: WidgetDress, night: Boolean): Color = when (look.ground) {
    WidgetGround.LIGHT -> dress.lightScheme.surface
    WidgetGround.DARK -> dress.darkScheme.surface
    WidgetGround.SYSTEM -> dress.scheme(night).surface
    WidgetGround.COLOR -> widgetCardContainer(look.cardColor)
}

/** The card's fill at the reader's opacity: the ground thins, the ink never does. */
fun widgetCardFill(look: WidgetLook, dress: WidgetDress, night: Boolean): Color =
    cardGround(look, dress, night).copy(alpha = look.opacityPct.coerceIn(0, 100) / 100f)

/**
 * The phone's night mode at render time. Glance's day/night providers are resolved by the
 * launcher, and a host that flips the card without the words leaves dark ink on a dark card:
 * every colour here is resolved against one answer.
 */
fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * Whether the wallpaper behind a see-through card can carry dark text, by the system's own
 * account ([WallpaperColors.HINT_SUPPORTS_DARK_TEXT]). Read as the affirmative signal it is:
 * dark ink only when the system says the ground is bright, light ink whenever it says nothing.
 */
fun wallpaperWantsDarkInk(context: Context): Boolean {
    val manager = runCatching { WallpaperManager.getInstance(context) }.getOrNull() ?: return false
    val colors = runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull()
        ?: runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_LOCK) }.getOrNull()
        ?: return false
    return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
}

/**
 * The card every widget lives in: the launcher's corner, the ground at the reader's opacity, the
 * insets for what sits against each edge, and one door into the app for the whole card (a cap's
 * own touch, inside it, adds a spend instead).
 */
@Composable
internal fun WidgetCard(
    model: WidgetModel,
    dress: WidgetDress,
    paddingHorizontal: Dp,
    paddingVertical: Dp,
    content: @Composable (WidgetPalette) -> Unit,
) {
    val context = LocalContext.current
    val palette = remember(model.look, dress) { widgetPalette(context, model.look, dress) }
    val tap = WidgetIntents.openApp(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WidgetCornerRes)
            .then(if (tap != null) GlanceModifier.clickable(actionStartActivity(tap)) else GlanceModifier),
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(widgetCardFill(model.look, dress, isNight(context)))),
        ) {}
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = paddingHorizontal, vertical = paddingVertical),
        ) {
            content(palette)
        }
    }
}

/**
 * The model this card draws, read again inside the composition whenever [WidgetRefresh] ticks.
 * [initial] is what `provideGlance` loaded at revision [loadedAt], so the first frame costs
 * nothing more and only a real change reloads. Each frame reports what it drew, so whatever asked
 * for the redraw can stay until it is drawn ([WidgetRefresh.awaitDrawn]).
 */
@Composable
internal fun rememberWidgetModel(
    appWidgetId: Int,
    initial: WidgetModel,
    loadedAt: Long,
    reload: suspend () -> WidgetModel,
): WidgetModel {
    val revision by WidgetRefresh.revision.collectAsState()
    val model by produceState(initial, revision) {
        val read = if (revision != loadedAt) reload() else initial
        value = read
        WidgetRefresh.drawn(appWidgetId, revision)
    }
    return model
}

/**
 * The card with nothing to show, or that could not read the caps: one message, with the touch
 * that helps where there is one (the whole card opens the app).
 */
@Composable
internal fun MessageContent(title: String, hint: String?, palette: WidgetPalette) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start,
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = palette.primaryInk,
                fontSize = WidgetType.BODY.sp,
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 2,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = TextStyle(color = palette.secondaryInk, fontSize = WidgetType.LABEL.sp),
                maxLines = 3,
            )
        }
    }
}

/** The locale a card formats in, off the context Glance composes with. */
internal fun Context.widgetLocale(): Locale = resources.configuration.locales[0]

internal fun fontScale(context: Context): Float = context.resources.configuration.fontScale

/**
 * How many lines [text] takes at [width], laid out as a `TextView` would lay it out: measured,
 * where a budget would reserve the most a sentence could take and leave the rest as air.
 */
internal fun measureWidgetLines(context: Context, text: String, sizeSp: Float, width: Dp, weight: TextWeight): Int {
    val metrics = context.resources.displayMetrics
    val widthPx = (width.value * metrics.density / FACE_MARGIN).toInt()
    if (text.isEmpty()) return 0
    if (widthPx <= 0) return Int.MAX_VALUE
    val paint = widgetPaint(context, weight, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, metrics))
    // A TextView's own breaking: the high-quality strategy, no hyphens.
    return StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
        .setIncludePad(true)
        .setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .build()
        .lineCount
}

/**
 * The three weights a card sets text in, each with the appearance Glance sets it in
 * (`text_faces.xml`): its weight, in the theme's device-default family.
 */
internal enum class TextWeight(@StyleRes val appearance: Int) {
    REGULAR(R.style.WidgetTextFace_Regular),
    MEDIUM(R.style.WidgetTextFace_Medium),
    BOLD(R.style.WidgetTextFace_Bold),
}

/**
 * A paint in the face the launcher will draw [weight] in, at [sizePx]: the very span Glance puts
 * on a weighted `Text`, applied here. Measured in plain sans-serif instead, a maker's own face can
 * come out a tenth wider than budgeted, and a figure is cut.
 */
internal fun widgetPaint(context: Context, weight: TextWeight, sizePx: Float): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        TextAppearanceSpan(context, weight.appearance).updateMeasureState(this)
        textSize = sizePx
    }

/**
 * The width of [text] in ems of the system face at [weight]: independent of the density and of
 * the reader's font scale, which the layouts apply themselves; with the margin a launcher's face
 * may want more of.
 */
internal fun textEm(context: Context, text: String, weight: TextWeight): Float =
    widgetPaint(context, weight, EM_PROBE_PX).measureText(text) * FACE_MARGIN / EM_PROBE_PX

/**
 * How much wider than measured a card budgets every line: a launcher may set a line a little wider
 * (its hinting, a face substituted for a digit), and a figure cut by a few dp is a figure lost.
 * `WidgetFitTest` draws every form 5% wider than measured and finds nothing cut.
 */
internal const val FACE_MARGIN = 1.06f
private const val EM_PROBE_PX = 100f

/**
 * The card's corner: the launcher's own. The platform names the radius the home screen gives its
 * widgets, and every card of the family takes it.
 */
internal val WidgetCornerRes = android.R.dimen.system_app_widget_background_radius

private const val QUIET_ALPHA = 0.75f
private const val TRACK_ALPHA = 0.2f
