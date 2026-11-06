package com.callbackdev.tetto.core.designsystem.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A name's size that keeps its words whole: from [max] down by [step] to [min], the first size at
 * which no word breaks across two lines and the text fits its lines. A tile's name beside its badge
 * has about ninety dp on a phone, and "Restaurants" at the title's size broke as "Restaurant / s"
 * (the README's first screenshot); the home screen's cards step a long name down the same way
 * before a word of it breaks. At [min] the name takes what it is given: an ellipsis, or a break
 * where no smaller size helps.
 */
class WholeWordsAutoSize(
    private val max: TextUnit,
    private val min: TextUnit = 13.sp,
    private val step: TextUnit = 1.sp,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = max.value
        while (size > min.value) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.hasVisualOverflow && !layout.breaksAWord(text.text)) return size.sp
            size -= step.value
        }
        return min
    }

    override fun equals(other: Any?): Boolean =
        other is WholeWordsAutoSize && other.max == max && other.min == min && other.step == step

    override fun hashCode(): Int = (max.hashCode() * 31 + min.hashCode()) * 31 + step.hashCode()
}

/** Whether a line ends inside a word: a letter or a digit on both sides of a line's end. */
internal fun TextLayoutResult.breaksAWord(text: String): Boolean = (0 until lineCount - 1).any { line ->
    val end = getLineEnd(line)
    end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
}

/**
 * A sentence's size that keeps it to [lines] lines where a small step down is enough: from [max]
 * down by [step] to [min], the largest size at which the text takes [lines] lines at most, and
 * [max] where none does, so a long sentence or a large text size wraps onto more lines rather than
 * being cut or shrunk for nothing. The caller lays the text out with no line limit.
 */
class FewLinesAutoSize(
    private val max: TextUnit,
    private val min: TextUnit,
    private val lines: Int = 2,
    private val step: TextUnit = 1.sp,
) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = max.value
        while (size >= min.value) {
            if (performLayout(constraints, text, size.sp).lineCount <= lines) return size.sp
            size -= step.value
        }
        return max
    }

    override fun equals(other: Any?): Boolean = other is FewLinesAutoSize &&
        other.max == max &&
        other.min == min &&
        other.lines == lines &&
        other.step == step

    override fun hashCode(): Int = ((max.hashCode() * 31 + min.hashCode()) * 31 + lines) * 31 + step.hashCode()
}
