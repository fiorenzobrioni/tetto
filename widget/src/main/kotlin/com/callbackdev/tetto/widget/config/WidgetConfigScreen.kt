package com.callbackdev.tetto.widget.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.tetto.core.designsystem.components.CapBadge
import com.callbackdev.tetto.core.designsystem.components.GroupDivider
import com.callbackdev.tetto.core.designsystem.components.GroupHeader
import com.callbackdev.tetto.core.designsystem.components.SettingsGroup
import com.callbackdev.tetto.core.designsystem.components.SwitchRow
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.icons.symbol
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.theme.widgetCardContainer
import com.callbackdev.tetto.core.model.CapState
import com.callbackdev.tetto.core.model.UserSettings
import com.callbackdev.tetto.core.model.WidgetCardColor
import com.callbackdev.tetto.core.model.WidgetGround
import com.callbackdev.tetto.core.model.WidgetLook
import com.callbackdev.tetto.widget.R
import com.callbackdev.tetto.widget.WidgetKind
import com.callbackdev.tetto.widget.WidgetModel
import com.callbackdev.tetto.widget.isNight
import com.callbackdev.tetto.widget.widgetCardFill
import com.callbackdev.tetto.widget.widgetDressFor
import kotlin.math.roundToInt

/** Tags for the tests. */
object WidgetConfigTags {
    const val LIST = "widget_config_list"
    const val SIZES = "widget_config_sizes"
    const val ALL_CAPS = "widget_config_all_caps"
    const val SENTENCE = "widget_config_sentence"
    const val CLOSEST = "widget_config_closest"
    const val DONE = "widget_config_done"

    fun cap(id: Long) = "widget_config_cap_$id"
}

/**
 * One card's settings (PLANNING.md §7), reached when it is placed and from the launcher's long
 * press: the card at the top, at the sizes it can be given; then what it is painted on, and which
 * caps it shows (and, on «In words», its sentence). Every choice is saved as it is made and redraws
 * this one card; «Reset this card» puts it back as it was placed, «Done» only closes the door.
 *
 * [model] is the reader's caps as the card reads them; [sample] says the preview draws sample caps
 * instead, because there is none yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    kind: WidgetKind,
    look: WidgetLook,
    model: WidgetModel?,
    preview: WidgetModel?,
    placed: DpSize?,
    onLook: (WidgetLook) -> Unit,
    onOpacityDrag: (Int) -> Unit,
    onOpacityDone: () -> Unit,
    onDone: () -> Unit,
    sample: Boolean = false,
) {
    val gutter = pageGutter(sideInsets = false)
    val settings = model?.settings ?: UserSettings()
    val caps = model?.caps.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_config_title)) },
                windowInsets = TopAppBarDefaults.windowInsets.add(gutter.asInsets()),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(gutter)
                .padding(bottom = 24.dp)
                .testTag(WidgetConfigTags.LIST),
        ) {
            PreviewSection(kind, preview?.copy(look = look), placed, sample)

            GroupHeader(stringResource(R.string.widget_config_background))
            SettingsGroup {
                GroundChoices(look, settings, onLook)
                GroupDivider()
                OpacityRow(look.opacityPct, onOpacityDrag, onOpacityDone)
            }

            when (kind) {
                WidgetKind.GLANCE -> {
                    GroupHeader(stringResource(R.string.widget_config_caps))
                    SettingsGroup { GlanceCapsChoice(look, caps, onLook) }
                }

                WidgetKind.WORDS -> {
                    GroupHeader(stringResource(R.string.widget_config_cap))
                    SettingsGroup { WordsCapChoice(look, caps, onLook) }
                    SettingsGroup(Modifier.padding(top = 12.dp)) {
                        SwitchRow(
                            label = stringResource(R.string.widget_config_sentence),
                            note = stringResource(R.string.widget_config_sentence_note),
                            checked = look.showSentence,
                            onChange = { onLook(look.copy(showSentence = it)) },
                            modifier = Modifier.testTag(WidgetConfigTags.SENTENCE),
                        )
                    }
                }
            }

            // The one control that undoes the others: outlined, in the error colour, at the foot.
            OutlinedButton(
                onClick = { onLook(WidgetLook()) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp),
            ) {
                Text(stringResource(R.string.widget_config_reset))
            }
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .testTag(WidgetConfigTags.DONE),
            ) {
                Text(stringResource(R.string.widget_config_done))
            }
        }
    }
}

/**
 * «At a glance»'s caps: every cap, or the ones ticked. Unticking «Every cap» starts from all of
 * them ticked, so nothing disappears from the card at once; the last one ticked cannot be unticked
 * (a card with no cap chosen would show them all, which is not what unticking says).
 */
@Composable
private fun GlanceCapsChoice(look: WidgetLook, caps: List<CapState>, onLook: (WidgetLook) -> Unit) {
    val chosen = look.glanceCaps?.filter { id -> caps.any { it.cap.id == id } }?.toSet()?.takeIf { it.isNotEmpty() }
    SwitchRow(
        label = stringResource(R.string.widget_config_all_caps),
        note = stringResource(
            if (chosen ==
                null
            ) {
                R.string.widget_config_all_caps_note
            } else {
                R.string.widget_config_chosen_caps_note
            },
        ),
        checked = chosen == null,
        onChange = { all ->
            onLook(look.copy(glanceCaps = if (all) null else caps.map { it.cap.id }.toSet()))
        },
        modifier = Modifier.testTag(WidgetConfigTags.ALL_CAPS),
    )
    if (caps.isEmpty()) {
        GroupDivider()
        NoCapsNote()
    } else if (chosen != null) {
        caps.forEach { state ->
            GroupDivider()
            val ticked = state.cap.id in chosen
            CapRow(
                state = state,
                selected = ticked,
                role = Role.Checkbox,
                enabled = !ticked || chosen.size > 1,
                onPick = {
                    val next = if (ticked) chosen - state.cap.id else chosen + state.cap.id
                    onLook(look.copy(glanceCaps = next))
                },
            ) { enabled -> Checkbox(checked = ticked, onCheckedChange = null, enabled = enabled) }
        }
    }
}

/** «In words»'s cap: the one closest to its cap, or one the reader names. */
@Composable
private fun WordsCapChoice(look: WidgetLook, caps: List<CapState>, onLook: (WidgetLook) -> Unit) {
    val chosen = look.wordsCap?.takeIf { id -> caps.any { it.cap.id == id } }
    Column(Modifier.selectableGroup()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = chosen == null, onClick = {
                    onLook(look.copy(wordsCap = null))
                }, role = Role.RadioButton)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(WidgetConfigTags.CLOSEST),
        ) {
            RadioButton(selected = chosen == null, onClick = null)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(R.string.widget_config_closest), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.widget_config_closest_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (caps.isEmpty()) {
            GroupDivider()
            NoCapsNote()
        }
        caps.forEach { state ->
            GroupDivider()
            CapRow(
                state = state,
                selected = chosen == state.cap.id,
                role = Role.RadioButton,
                enabled = true,
                onPick = { onLook(look.copy(wordsCap = state.cap.id)) },
            ) { RadioButton(selected = chosen == state.cap.id, onClick = null) }
        }
    }
}

@Composable
private fun NoCapsNote() {
    Text(
        text = stringResource(R.string.widget_config_no_caps),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/** A cap to pick: its control, its badge and its name, the whole row the touch target. */
@Composable
private fun CapRow(
    state: CapState,
    selected: Boolean,
    role: Role,
    enabled: Boolean,
    onPick: () -> Unit,
    control: @Composable (Boolean) -> Unit,
) {
    val modifier = if (role == Role.Checkbox) {
        Modifier.toggleable(value = selected, enabled = enabled, role = role, onValueChange = { onPick() })
    } else {
        Modifier.selectable(selected = selected, enabled = enabled, role = role, onClick = onPick)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(WidgetConfigTags.cap(state.cap.id)),
    ) {
        control(enabled)
        CapBadge(state.cap.icon.symbol, modifier = Modifier.padding(start = 12.dp))
        Text(
            text = state.cap.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
    }
}

/** What the ground question offers, in order. Every [WidgetGround] has its row. */
internal val WidgetGroundChoices: List<Pair<WidgetGround, Int>> = listOf(
    WidgetGround.LIGHT to R.string.widget_bg_light,
    WidgetGround.DARK to R.string.widget_bg_dark,
    WidgetGround.SYSTEM to R.string.widget_bg_system,
    WidgetGround.COLOR to R.string.widget_bg_color,
)

/** The six, in their order. Every [WidgetCardColor] has its swatch. */
internal val WidgetCardColorChoices: List<Pair<WidgetCardColor, Int>> = listOf(
    WidgetCardColor.BLUE to R.string.widget_color_blue,
    WidgetCardColor.AZURE to R.string.widget_color_azure,
    WidgetCardColor.GREEN to R.string.widget_color_green,
    WidgetCardColor.TEAL to R.string.widget_color_teal,
    WidgetCardColor.PLUM to R.string.widget_color_plum,
    WidgetCardColor.CLAY to R.string.widget_color_clay,
)

/**
 * What kind of card, each row with a swatch of the ground it paints; and, once «A colour» is
 * picked, which colour, as a strip of swatches with the chosen one's name under it (a swatch is
 * never the only label).
 */
@Composable
private fun GroundChoices(look: WidgetLook, settings: UserSettings, onLook: (WidgetLook) -> Unit) {
    val context = LocalContext.current
    val dress = widgetDressFor(context, settings)
    val night = isNight(context)
    Column(Modifier.selectableGroup()) {
        WidgetGroundChoices.forEach { (ground, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = look.ground == ground, onClick = {
                        onLook(look.copy(ground = ground))
                    }, role = Role.RadioButton)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                RadioButton(selected = look.ground == ground, onClick = null)
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
                val swatch = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                val full = look.copy(ground = ground, opacityPct = 100)
                if (ground == WidgetGround.SYSTEM) {
                    // The phone's choice, drawn as both of its answers on a diagonal.
                    Canvas(swatch) {
                        drawRect(dress.lightScheme.surface)
                        drawPath(
                            Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            },
                            dress.darkScheme.surface,
                        )
                    }
                } else {
                    Box(swatch.background(widgetCardFill(full, dress, night)))
                }
            }
        }
    }
    if (look.ground == WidgetGround.COLOR) {
        ColorSwatches(look.cardColor) { onLook(look.copy(cardColor = it)) }
    }
}

@Composable
private fun ColorSwatches(selected: WidgetCardColor, onPick: (WidgetCardColor) -> Unit) {
    Column(modifier = Modifier.padding(start = 50.dp, end = 10.dp, bottom = 12.dp)) {
        // Each colour is a 48dp target around its 36dp disc, side by side: the gaps between the
        // discs are the targets' own. A narrow phone wraps the row.
        FlowRow(modifier = Modifier.selectableGroup()) {
            WidgetCardColorChoices.forEach { (color, label) ->
                val chosen = color == selected
                val name = stringResource(label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .selectable(selected = chosen, onClick = { onPick(color) }, role = Role.RadioButton)
                        .semantics { contentDescription = name },
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (chosen) 2.dp else 0.dp,
                                color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape,
                            )
                            .padding(if (chosen) 4.dp else 0.dp)
                            .clip(CircleShape)
                            .background(widgetCardContainer(color)),
                    ) {
                        if (chosen) {
                            // Every card colour is a dark ground under white ink.
                            Icon(
                                TettoIcons.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(WidgetCardColorChoices.first { it.first == selected }.second),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** How solid the card is: the name and the value on one line, the slider under them, in steps of 5%. */
@Composable
private fun OpacityRow(pct: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when (pct) {
                    100 -> stringResource(R.string.widget_opacity_full)
                    0 -> stringResource(R.string.widget_opacity_transparent)
                    else -> stringResource(R.string.widget_opacity_percent, pct)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pct.toFloat(),
            onValueChange = { raw -> onChange((raw / 5f).roundToInt() * 5) },
            onValueChangeFinished = onDone,
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}
