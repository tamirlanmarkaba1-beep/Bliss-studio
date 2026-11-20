package com.nova.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Lang { JSON, KOTLIN, LUA, PLAIN }

fun langOf(fileName: String): Lang = when {
    fileName.endsWith(".json") -> Lang.JSON
    fileName.endsWith(".kt") -> Lang.KOTLIN
    fileName.endsWith(".lua") -> Lang.LUA
    else -> Lang.PLAIN
}

private val cKeyword = Color(0xFFCC7832)
private val cString = Color(0xFF6A8759)
private val cNumber = Color(0xFF6897BB)
private val cComment = Color(0xFF808080)
private val cKey = Color(0xFF9876AA)

private val reNumber = Regex("""-?\b\d+(?:\.\d+)?f?\b""")
private val reString = Regex(""""(?:[^"\\\n]|\\.)*"""")
private val reKey = Regex(""""(?:[^"\\\n]|\\.)*"(?=\s*:)""")
private val reJsonLit = Regex("""\b(?:true|false|null)\b""")
private val reKotlinKw = Regex("""\b(?:val|var|fun|class|object|if|else|when|for|while|return|import|package|true|false|null|in|is|as|enum|interface|override|private|public)\b""")
private val reLuaKw = Regex("""\b(?:function|end|if|then|else|elseif|for|while|do|return|local|true|false|nil|and|or|not|in|break)\b""")
private val reSlashComment = Regex("""//[^\n]*""")
private val reDashComment = Regex("""--[^\n]*""")

fun highlight(src: String, lang: Lang): AnnotatedString = buildAnnotatedString {
    append(src)
    fun paint(re: Regex, c: Color) {
        for (m in re.findAll(src)) addStyle(SpanStyle(color = c), m.range.first, m.range.last + 1)
    }
    when (lang) {
        Lang.JSON -> { paint(reNumber, cNumber); paint(reJsonLit, cKeyword); paint(reString, cString); paint(reKey, cKey) }
        Lang.KOTLIN -> { paint(reNumber, cNumber); paint(reKotlinKw, cKeyword); paint(reString, cString); paint(reSlashComment, cComment) }
        Lang.LUA -> { paint(reNumber, cNumber); paint(reLuaKw, cKeyword); paint(reString, cString); paint(reDashComment, cComment) }
        Lang.PLAIN -> {}
    }
}

private class SyntaxTransformation(val lang: Lang) : VisualTransformation {
    override fun filter(text: AnnotatedString) =
        TransformedText(highlight(text.text, lang), OffsetMapping.Identity)
}

@Composable
fun CodeEditor(
    text: String,
    onChange: (String) -> Unit,
    lang: Lang,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        color = NovaColors.text
    )
    val lines = text.count { it == '\n' } + 1
    val gutter = remember(lines) { (1..lines).joinToString("\n") }
    val vt = remember(lang) { SyntaxTransformation(lang) }
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Row(modifier.background(NovaColors.bg).verticalScroll(vScroll)) {
        Text(
            gutter,
            style = style.copy(color = NovaColors.dim, textAlign = TextAlign.End),
            modifier = Modifier.width(36.dp).background(NovaColors.panel).padding(end = 6.dp)
        )
        BasicTextField(
            value = text,
            onValueChange = onChange,
            readOnly = readOnly,
            textStyle = style,
            cursorBrush = SolidColor(NovaColors.accent),
            visualTransformation = vt,
            modifier = Modifier
                .padding(start = 6.dp)
                .horizontalScroll(hScroll)
                .defaultMinSize(minWidth = 320.dp, minHeight = 200.dp)
        )
    }
}
