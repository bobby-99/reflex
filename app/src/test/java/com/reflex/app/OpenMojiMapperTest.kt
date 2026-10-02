package com.reflex.app

import com.reflex.app.util.OpenMojiMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMojiMapperTest {

    @Test
    fun testUnicodeFormatting() {
        val u1 = "1F3CB"
        val u2 = "1f3cb"
        val u3 = "u+1f3cb"
        val u4 = "openmoji_1f3cb"

        assertEquals("1f3cb", u1.lowercase().removePrefix("u+").removePrefix("openmoji_"))
        assertEquals("1f3cb", u2.lowercase().removePrefix("u+").removePrefix("openmoji_"))
        assertEquals("1f3cb", u3.lowercase().removePrefix("u+").removePrefix("openmoji_"))
        assertEquals("1f3cb", u4.lowercase().removePrefix("u+").removePrefix("openmoji_"))
    }
}
