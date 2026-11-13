package com.jjmluz.nationslingo

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Picks the largest size, from [max] down to [min], at which the text fits without a word broken across
 * lines. Country names in some languages have long single words, like "Zentralafrikanische".
 */
data class WholeWordsAutoSize(val min: TextUnit, val max: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        var size = max.value
        while (size > min.value) {
            val layout = performLayout(constraints, text, size.sp)
            if (!layout.hasVisualOverflow && !layout.breaksWord(text)) return size.sp
            size -= 1
        }
        return min
    }

    private fun TextLayoutResult.breaksWord(text: AnnotatedString): Boolean =
        (0 until lineCount - 1).any { line ->
            val end = getLineEnd(line)
            end in 1 until text.length && text[end - 1].isLetterOrDigit() && text[end].isLetterOrDigit()
        }
}
