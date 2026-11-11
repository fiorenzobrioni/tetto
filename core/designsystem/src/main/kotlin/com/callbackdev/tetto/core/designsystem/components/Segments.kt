package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * One choice among a few, all in sight, across the page: Material's segmented button, the control
 * for a single choice of two to five (a cap's period: a day, a week, a month, a year). The fill
 * says which is chosen, and so does the semantics; no check mark, so four words fit a narrow
 * phone. [selected] may be none of them (a one-off cap has no period that returns).
 *
 * @param tag a test tag for each choice, where a test needs one.
 */
@Composable
fun <T> ChoiceSegments(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: ((T) -> String)? = null,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                label = { SegmentLabel(label) },
                modifier = if (tag != null) Modifier.testTag(tag(value)) else Modifier,
            )
        }
    }
}

/**
 * A segment's label: one line at the reader's text size, and only as much smaller as the segment
 * needs where that size does not fit. At twice the size, four segments across a small phone would
 * read «We…» and «Mo…», and a long word in a narrow one («Settimana», four across 320dp) would be
 * cut the same way; a word a little smaller is still a word. At a large text size never below the
 * size it has at the standard setting, and at the standard one a step smaller at most. TalkBack
 * reads it whole either way.
 */
@Composable
fun SegmentLabel(text: String, modifier: Modifier = Modifier) {
    val style = LocalTextStyle.current
    val size = style.fontSize
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = when {
            !size.isSpecified -> null

            scale > 1f -> TextAutoSize.StepBased(minFontSize = (size.value / scale).sp, maxFontSize = size)

            // The sizes tried are counted up from the smallest: a whole number of steps below
            // the label's own size, or a label that fits would be set a hair smaller than it is.
            else -> TextAutoSize.StepBased(
                minFontSize = (size.value - NARROW_STEPS * NARROW_STEP).sp,
                maxFontSize = size,
                stepSize = NARROW_STEP.sp,
            )
        },
    )
}

/** At the standard text size a label gives up 2sp at most, half a point at a time: 12sp from 14sp. */
private const val NARROW_STEP = 0.5f
private const val NARROW_STEPS = 4
