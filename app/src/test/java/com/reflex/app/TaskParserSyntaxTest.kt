package com.reflex.app

import com.reflex.app.util.QuickAddHelperData
import com.reflex.app.util.TaskParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskParserSyntaxTest {

    @Test
    fun testAllQuickAddHelperExamplesParseSuccessfully() {
        for (example in QuickAddHelperData.EXAMPLES) {
            val result = TaskParser.parse(example.exampleText)

            assertEquals(
                "Title should match expected clean title for '${example.exampleText}'",
                example.expectedTitle.lowercase(),
                result.title.lowercase()
            )

            assertEquals(
                "Priority should match for '${example.exampleText}'",
                example.expectedPriority,
                result.priority
            )

            if (example.hasDate) {
                assertNotNull(
                    "DueDate should be parsed for '${example.exampleText}'",
                    result.dueDate
                )
            }

            if (example.hasTime) {
                assertNotNull(
                    "DueTime should be parsed for '${example.exampleText}'",
                    result.dueTime
                )
            }

            if (example.isRepeating) {
                assertTrue(
                    "Recurrence should be parsed for '${example.exampleText}'",
                    result.recurrenceFrequency != com.reflex.app.data.RecurrenceFrequency.NONE
                )
            }
        }
    }

    @Test
    fun testPlaceholderTextParsesSuccessfully() {
        val result = TaskParser.parse(QuickAddHelperData.PLACEHOLDER_TEXT)
        assertEquals("Call mom", result.title)
        assertEquals(com.reflex.app.data.Priority.HIGH, result.priority)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
    }
}
