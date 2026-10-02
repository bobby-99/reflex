package com.reflex.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.Color
import com.reflex.app.ui.theme.DarkInkText
import com.reflex.app.ui.theme.CopperPrimary
import java.util.Locale

class TaskHighlightVisualTransformation(
    private val textColor: Color = DarkInkText,
    private val highlightColor: Color = CopperPrimary
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val highlighted = buildAnnotatedString {
            append(originalText)

            if (originalText.isBlank()) return@buildAnnotatedString

            val matchedRanges = mutableListOf<IntRange>()

            fun applyHighlight(start: Int, end: Int) {
                if (start < 0 || end > originalText.length || start >= end) return
                val isOverlap = matchedRanges.any { !(end <= it.first || start >= it.last) }
                if (!isOverlap) {
                    matchedRanges.add(start until end)
                    addStyle(
                        style = SpanStyle(
                            color = textColor,
                            background = highlightColor,
                            fontWeight = FontWeight.Bold
                        ),
                        start = start,
                        end = end
                    )
                }
            }

            // 1. Explicit Calendar Date Regex (e.g. "sept 14th", "12 Aug", "12th Aug", "17th Sept", "20 jan 2027", "Aug 12", "Sept 17th", "20th of Jan")
            val monthsPart = "(?:september|sept|sep|january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|october|oct|november|nov|december|dec)"
            val calendarDateRegex = Regex("(?i)\\b(\\d{1,2}(?:st|nd|rd|th)?\\s+(?:of\\s+)?$monthsPart(?:\\s+\\d{4})?|$monthsPart\\s+\\d{1,2}(?:st|nd|rd|th)?(?:\\s+\\d{4})?)\\b")
            calendarDateRegex.findAll(originalText).forEach { match ->
                applyHighlight(match.range.first, match.range.last + 1)
            }

            // 2. Relative Duration Regex ("in 5min", "45min", "1h10min", "in 1hr4min", "in 2 hours", "5min", "10 min", "in 5 days", "in 2 weeks")
            val durationRegex = Regex("(?i)\\b((?:in\\s+)?(?:\\d+\\s*(?:h|hr|hrs|hour|hours))?\\s*(?:\\d+\\s*(?:m|min|mins|minute|minutes))|(?:in\\s+)?\\d+\\s*(?:h|hr|hrs|hour|hours)|in\\s+\\d+\\s+day(?:s)?|in\\s+\\d+\\s+week(?:s)?)\\b")
            durationRegex.findAll(originalText).forEach { match ->
                applyHighlight(match.range.first, match.range.last + 1)
            }

            // 3. Explicit Time Regex (e.g. "10:46am", "8pm", "3:15 pm", "2 p.m.", "10 o'clock", "14:30", "10 30 pm")
            val timeRegex = Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|p\\.m\\.|a\\.m\\.|p\\s+m|a\\s+m|o['’]?\\s*clock)|[01]?\\d|2[0-3]:[0-5]\\d|\\d{1,2}\\s+\\d{2}\\s*(?:am|pm))\\b")
            timeRegex.findAll(originalText).forEach { match ->
                applyHighlight(match.range.first, match.range.last + 1)
            }

            // 4. Priority and Keyword Phrases (Multi-word first)
            val keywords = listOf(
                "repeat every day", "repeat every week", "repeat every month", "repeat every year", "repeat every weekday",
                "every month", "every weekday", "every week", "every year", "every day", "everyday",
                "repeat every monday", "repeat every tuesday", "repeat every wednesday", "repeat every thursday", "repeat every friday", "repeat every saturday", "repeat every sunday",
                "repeat every mon", "repeat every tue", "repeat every tues", "repeat every wed", "repeat every thu", "repeat every thur", "repeat every thurs", "repeat every fri", "repeat every sat", "repeat every sun",
                "every monday", "every tuesday", "every wednesday", "every thursday", "every friday", "every saturday", "every sunday",
                "every mon", "every tue", "every tues", "every wed", "every thu", "every thur", "every thurs", "every fri", "every sat", "every sun",
                "daily", "weekly", "monthly", "yearly", "annually", "weekdays",
                "repeat", "repeating", "repeats",
                "high priority", "priority high", "medium priority", "med priority", "priority medium",
                "low priority", "priority low", "top priority", "end of day", "next week",
                "next monday", "next mon", "next tuesday", "next tue", "next tues",
                "next wednesday", "next wed", "next thursday", "next thu", "next thur", "next thurs",
                "next friday", "next fri", "next saturday", "next sat", "next sunday", "next sun",
                "this monday", "this mon", "this tuesday", "this tue", "this tues",
                "this wednesday", "this wed", "this thursday", "this thu", "this thur", "this thurs",
                "this friday", "this fri", "this saturday", "this sat", "this sunday", "this sun",
                "!!!", "!!", "!", "p1", "p2", "p3", "urgent", "asap", "important",
                "today", "2day", "tomorrow", "tmrw", "tmr", "yesterday", "tonight",
                "morning", "mrng", "mrn", "noon", "midday", "afternoon", "evening", "evng", "eve", "evn", 
                "eod", "eow", "night", "nite", "midnight",
                "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
                "mon", "tue", "tues", "wed", "thu", "thur", "thurs", "fri", "sat", "sun"
            )

            for (kw in keywords) {
                val pattern = if (kw.all { it.isLetterOrDigit() }) "\\b${Regex.escape(kw)}\\b" else Regex.escape(kw)
                Regex("(?i)$pattern").findAll(originalText).forEach { match ->
                    applyHighlight(match.range.first, match.range.last + 1)
                }
            }
        }

        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
