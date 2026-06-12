package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    baseColor: Color = TextPrimary,
    baseFontSize: TextUnit = 13.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    lineHeight: TextUnit = 18.sp
) {
    if (markdown.isBlank()) {
        return
    }

    val lines = remember(markdown) { markdown.split("\n") }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var displayedCount = 0
        for (line in lines) {
            if (displayedCount >= maxLines) break
            
            val trimmed = line.trim()
            when {
                // Horizontal divider
                trimmed == "---" || trimmed == "***" -> {
                    Divider(
                        color = SlateCardBorder,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    displayedCount++
                }
                
                // Headers (# H1, ## H2, ### H3, #### H4)
                trimmed.startsWith("# ") -> {
                    val headerText = trimmed.removePrefix("# ")
                    Text(
                        text = parseMarkdownInline(headerText, AccentTealBright),
                        color = TextPurple,
                        fontSize = (baseFontSize.value * 1.35f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                    displayedCount++
                }
                trimmed.startsWith("## ") -> {
                    val headerText = trimmed.removePrefix("## ")
                    Text(
                        text = parseMarkdownInline(headerText, AccentTealBright),
                        color = AccentTealBright,
                        fontSize = (baseFontSize.value * 1.25f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    displayedCount++
                }
                trimmed.startsWith("### ") -> {
                    val headerText = trimmed.removePrefix("### ")
                    Text(
                        text = parseMarkdownInline(headerText, AccentTealBright),
                        color = TextPrimary,
                        fontSize = (baseFontSize.value * 1.15f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 1.dp)
                    )
                    displayedCount++
                }
                trimmed.startsWith("#### ") -> {
                    val headerText = trimmed.removePrefix("#### ")
                    Text(
                        text = parseMarkdownInline(headerText, AccentTealBright),
                        color = TextPrimary,
                        fontSize = (baseFontSize.value * 1.05f).sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp, bottom = 1.dp)
                    )
                    displayedCount++
                }
                
                // Quote block (> text)
                trimmed.startsWith("> ") || trimmed == ">" -> {
                    val quoteText = trimmed.removePrefix("> ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(IntrinsicSize.Min)
                                .background(AccentTeal, RoundedCornerShape(2.dp))
                                .align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownInline(quoteText, AccentTealBright),
                            color = TextMuted,
                            fontSize = baseFontSize,
                            fontStyle = FontStyle.Italic,
                            lineHeight = lineHeight
                        )
                    }
                    displayedCount++
                }
                
                // Checked Checklist Item (- [x] or - [X])
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                    val rawTaskText = trimmed.substring(6)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AccentTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownInline(rawTaskText, AccentTealBright),
                            color = TextMuted,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    displayedCount++
                }
                
                // Unchecked Checklist Item (- [ ])
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val rawTaskText = trimmed.substring(6)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .border(1.dp, TextMuted, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseMarkdownInline(rawTaskText, AccentTealBright),
                            color = baseColor,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight
                        )
                    }
                    displayedCount++
                }
                
                // Unordered list item (- item or * item)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                    val rawItemText = trimmed.removePrefix("- ").removePrefix("* ").removePrefix("• ")
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 1.dp)
                    ) {
                        Text(
                            text = "•",
                            color = AccentTealBright,
                            fontSize = baseFontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseMarkdownInline(rawItemText, AccentTealBright),
                            color = baseColor,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight,
                            maxLines = if (maxLines != Int.MAX_VALUE) maxLines - displayedCount else Int.MAX_VALUE,
                            overflow = overflow
                        )
                    }
                    displayedCount++
                }
                
                // Ordered list item (1. item)
                trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(". ") && trimmed.split(". ").firstOrNull()?.all { it.isDigit() } == true -> {
                    val prefix = trimmed.substringBefore(". ") + "."
                    val rawItemText = trimmed.substringAfter(". ")
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 6.dp, top = 1.dp, bottom = 1.dp)
                    ) {
                        Text(
                            text = prefix,
                            color = AccentTealBright,
                            fontSize = baseFontSize,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = parseMarkdownInline(rawItemText, AccentTealBright),
                            color = baseColor,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight,
                            maxLines = if (maxLines != Int.MAX_VALUE) maxLines - displayedCount else Int.MAX_VALUE,
                            overflow = overflow
                        )
                    }
                    displayedCount++
                }
                
                // Regular paragraph
                else -> {
                    if (trimmed.isNotEmpty()) {
                        Text(
                            text = parseMarkdownInline(trimmed, AccentTealBright),
                            color = baseColor,
                            fontSize = baseFontSize,
                            lineHeight = lineHeight,
                            maxLines = if (maxLines != Int.MAX_VALUE) maxLines - displayedCount else Int.MAX_VALUE,
                            overflow = overflow
                        )
                        displayedCount++
                    } else if (line.isEmpty() && displayedCount > 0) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

fun parseMarkdownInline(text: String, primaryColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            val remain = text.substring(index)
            
            // Code block
            if (remain.startsWith("`")) {
                val match = remain.drop(1).indexOf("`")
                if (match != -1) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = primaryColor.copy(alpha = 0.15f), color = primaryColor))
                    append(remain.substring(1, match + 1))
                    pop()
                    index += match + 2
                    continue
                }
            }
            
            // Bold Italic
            if (remain.startsWith("***")) {
                val match = remain.drop(3).indexOf("***")
                if (match != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(remain.substring(3, match + 3))
                    pop()
                    index += match + 6
                    continue
                }
            }
            if (remain.startsWith("___")) {
                val match = remain.drop(3).indexOf("___")
                if (match != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(remain.substring(3, match + 3))
                    pop()
                    index += match + 6
                    continue
                }
            }

            // Bold
            if (remain.startsWith("**")) {
                val match = remain.drop(2).indexOf("**")
                if (match != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(remain.substring(2, match + 2))
                    pop()
                    index += match + 4
                    continue
                }
            }
            if (remain.startsWith("__")) {
                val match = remain.drop(2).indexOf("__")
                if (match != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(remain.substring(2, match + 2))
                    pop()
                    index += match + 4
                    continue
                }
            }

            // Italic
            if (remain.startsWith("*")) {
                val match = remain.drop(1).indexOf("*")
                if (match != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(remain.substring(1, match + 1))
                    pop()
                    index += match + 2
                    continue
                }
            }
            if (remain.startsWith("_")) {
                val match = remain.drop(1).indexOf("_")
                if (match != -1) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(remain.substring(1, match + 1))
                    pop()
                    index += match + 2
                    continue
                }
            }
            
            append(text[index])
            index++
        }
    }
}
