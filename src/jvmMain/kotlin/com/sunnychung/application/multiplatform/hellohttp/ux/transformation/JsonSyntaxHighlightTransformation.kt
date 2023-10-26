package com.sunnychung.application.multiplatform.hellohttp.ux.transformation

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.sunnychung.application.multiplatform.hellohttp.ux.local.AppColor

val OBJECT_KEY_REGEX = "(?<!\\\\)(\"(?:[^\"]|\\\\\\\")*?(?<!\\\\)\")\\s*:".toRegex()
val STRING_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(?<!\\\\)(\"(?:[^\"]|\\\\\\\")*?(?<!\\\\)\")".toRegex()
val NUMBER_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)\\b".toRegex()
val BOOLEAN_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(true|false)\\b".toRegex()
val NOTHING_LITERAL_REGEX = "(?<!\\\\)\"\\s*:\\s*(null|undefined)\\b".toRegex()

class JsonSyntaxHighlightTransformation(val colours: AppColor) : VisualTransformation {

    val objectKeyStyle = SpanStyle(color = colours.syntaxColor.objectKey)
    val stringLiteralStyle = SpanStyle(color = colours.syntaxColor.stringLiteral)
    val numberLiteralStyle = SpanStyle(color = colours.syntaxColor.numberLiteral)
    val booleanLiteralStyle = SpanStyle(color = colours.syntaxColor.booleanLiteral)
    val nothingLiteralStyle = SpanStyle(color = colours.syntaxColor.nothingLiteral)

    var lastTextHash: Int? = null
    var lastResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()

        if (lastTextHash == text.text.hashCode() && lastResult != null) {
            return lastResult!!
        }

        listOf(
            OBJECT_KEY_REGEX to objectKeyStyle,
            STRING_LITERAL_REGEX to stringLiteralStyle,
            NUMBER_LITERAL_REGEX to numberLiteralStyle,
            BOOLEAN_LITERAL_REGEX to booleanLiteralStyle,
            NOTHING_LITERAL_REGEX to nothingLiteralStyle,
        ).forEach { (regex, style) ->
            regex.findAll(s).forEach { m ->
                val range = m.groups[1]!!.range
                spans += AnnotatedString.Range(style, range.start, range.endInclusive + 1)
            }
        }
        lastTextHash = text.text.hashCode()
        lastResult = TransformedText(AnnotatedString(s, text.spanStyles + spans), OffsetMapping.Identity)
        return lastResult!!
    }
}