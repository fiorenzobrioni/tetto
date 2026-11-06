package com.callbackdev.tetto.feature.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.tetto.core.designsystem.format.rememberMoneyFormat
import com.callbackdev.tetto.core.designsystem.icons.TettoIcons
import com.callbackdev.tetto.core.designsystem.theme.ScreenMargin
import com.callbackdev.tetto.core.designsystem.theme.padding
import com.callbackdev.tetto.core.designsystem.theme.pageGutter
import com.callbackdev.tetto.core.designsystem.R as DesignR

@Composable
fun GuideRoute(onBack: () -> Unit, viewModel: GuideViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GuideScreen(state = state, onBack = onBack)
}

/**
 * The guide (VISION, «Settings»; PLANNING.md §11 Phase 6): what the caps screen answers, how a
 * period, the reserve and forgiveness work, the numpad and fixing a spend, the home screen, your
 * data, and what Tetto does not do and which kind of app does. From the card first in Settings and
 * from the caps screen's first-day tip: a definition offered before the reader has met the thing
 * does not stick, so the guide stays where the reader can come back to it.
 *
 * Two rules hold it in shape. It **never teaches a control**: it says what a part of the screen is
 * for, not which button to press. And it **teaches by showing the app's own components** (two of
 * the caps screen's tiles, the bar's story over four weeks, the numpad's amount and preview), drawn
 * by the engine from sample figures, each captioned as an example, in the reader's currency and
 * week, so no sample reads as the reader's money and none looks foreign to it. That is the one
 * thing it reads from the settings, and why it has a view model ([GuideViewModel]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(state: GuideUiState?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(TettoIcons.Back, contentDescription = stringResource(DesignR.string.action_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the settings are read the examples would be in the wrong currency for a frame.
        if (state != null) GuideContent(state, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun GuideContent(state: GuideUiState, modifier: Modifier) {
    val format = rememberMoneyFormat(state.currency)
    val sample = remember(state) { GuideSample(state.currency, state.weekStart) }
    Column(
        modifier = modifier
            .testTag(GuideTags.CONTENT)
            .verticalScroll(rememberScrollState())
            .padding(pageGutter(sideInsets = false))
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        Chapter(TettoIcons.Roof, stringResource(R.string.guide_caps_title))
        Paragraph(stringResource(R.string.guide_caps_p1))
        TilesSample(sample, format)
        Caption(stringResource(R.string.guide_caps_caption, weekdayName(sample.today.dayOfWeek)))
        Feature(R.string.guide_caps_left_title, R.string.guide_caps_left_body)
        Feature(R.string.guide_caps_bar_title, R.string.guide_caps_bar_body)
        Feature(R.string.guide_caps_pace_title, R.string.guide_caps_pace_body)
        Feature(R.string.guide_caps_live_title, R.string.guide_caps_live_body)

        Chapter(TettoIcons.Renew, stringResource(R.string.guide_period_title))
        Paragraph(stringResource(R.string.guide_period_p1))
        StorySample(sample, format)
        Caption(stringResource(R.string.guide_period_caption, format.short(sample.storyCap)))
        Feature(R.string.guide_period_reserve_title, R.string.guide_period_reserve_body)
        Feature(R.string.guide_period_forgiven_title, R.string.guide_period_forgiven_body)
        Feature(R.string.guide_period_calendar_title, R.string.guide_period_calendar_body)
        Feature(R.string.guide_period_change_title, R.string.guide_period_change_body)
        Feature(R.string.guide_period_closing_title, R.string.guide_period_closing_body)
        Feature(R.string.guide_period_frozen_title, R.string.guide_period_frozen_body)

        Chapter(TettoIcons.Keypad, stringResource(R.string.guide_numpad_title))
        Paragraph(stringResource(R.string.guide_numpad_p1))
        NumpadSample(sample, format)
        Caption(
            stringResource(
                R.string.guide_numpad_caption,
                format.full(sample.typed),
                format.short(sample.coffee.left),
            ),
        )
        Feature(R.string.guide_numpad_preview_title, R.string.guide_numpad_preview_body)
        Feature(R.string.guide_numpad_undo_title, R.string.guide_numpad_undo_body)
        Feature(R.string.guide_numpad_sheet_title, R.string.guide_numpad_sheet_body)
        Feature(R.string.guide_numpad_keys_title, R.string.guide_numpad_keys_body)

        Chapter(TettoIcons.Widgets, stringResource(R.string.guide_home_title))
        Feature(R.string.guide_home_two_title, R.string.guide_home_two_body)
        Feature(R.string.guide_home_add_title, R.string.guide_home_add_body)
        Feature(R.string.guide_home_dates_title, R.string.guide_home_dates_body)
        Feature(R.string.guide_home_reminder_title, R.string.guide_home_reminder_body)

        Chapter(TettoIcons.Shield, stringResource(R.string.guide_data_title))
        Paragraph(stringResource(R.string.guide_data_p1))
        Paragraph(stringResource(R.string.guide_data_p2))
        Paragraph(stringResource(R.string.guide_data_p3))

        Chapter(TettoIcons.Ledger, stringResource(R.string.guide_not_title))
        Paragraph(stringResource(R.string.guide_not_p1))
        Paragraph(stringResource(R.string.guide_not_p2))
        Feature(R.string.guide_not_total_title, R.string.guide_not_total_body)
        Feature(R.string.guide_not_move_title, R.string.guide_not_move_body)
        Feature(R.string.guide_not_alerts_title, R.string.guide_not_alerts_body)
        Feature(R.string.guide_not_currency_title, R.string.guide_not_currency_body)
    }
}

// The prose kit.

/**
 * A chapter's title with its glyph: a heading, so TalkBack can move from one chapter to the next.
 * The glyph is one title line tall, in the reader's text size, and stands by the first line: at
 * twice the text a title runs to three lines, and a fixed glyph centred on them would float.
 */
@Composable
private fun Chapter(icon: ImageVector, text: String) {
    val line = MaterialTheme.typography.titleLarge.lineHeight
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        // Decoration: the title beside it says it all.
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(with(LocalDensity.current) { line.toDp() }),
        )
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
}

/** Prose to be read, not scanned: bodyLarge. */
@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

/** One thing a part of the screen does: its name, then what it is for. */
@Composable
private fun Feature(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

/** What the sample above it stands for: always said, so no example reads as the reader's money. */
@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** What the tests find the page and its examples by. */
object GuideTags {
    const val CONTENT = "guide_content"
    const val TILES = "guide_tiles"
    const val STORY = "guide_story"
    const val NUMPAD = "guide_numpad"
}
