package com.reflex.app

import com.reflex.app.data.SettingItemType
import com.reflex.app.data.SettingsSchema
import com.reflex.app.service.FocusPhase
import com.reflex.app.util.FocusTickPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class FocusTickSynthesisTest {

    @Test
    fun testSynthesizeTickWaveformCharacteristics() {
        val sampleRate = 44100
        val durationMs = 18
        val peakAmplitude = 0.20
        val pcm = FocusTickPlayer.synthesizeTickWaveform(sampleRate, durationMs, peakAmplitude)

        // Verify sample count corresponds to 18ms at 44.1kHz
        val expectedSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        assertEquals(expectedSamples, pcm.size)
        assertTrue("Waveform must be around 15-20ms", pcm.size in 660..882)

        // Verify smooth attack starts at zero
        assertEquals(0.toShort(), pcm[0])

        // Verify end decays smoothly
        assertTrue(abs(pcm.last().toInt()) < 100)

        // Verify amplitude within the 0.15f - 0.25f range
        val maxVal = pcm.maxOf { abs(it.toInt()) }
        val maxAmp = maxVal.toDouble() / Short.MAX_VALUE
        assertTrue("Max amplitude ($maxAmp) should be <= 0.25f", maxAmp <= 0.25)
        assertTrue("Max amplitude ($maxAmp) should be >= 0.10f", maxAmp >= 0.10)
    }

    @Test
    fun testCreateWavBytesFormat() {
        val sampleRate = 44100
        val pcm = FocusTickPlayer.synthesizeTickWaveform(sampleRate, 18, 0.20)
        val wavBytes = FocusTickPlayer.createWavBytes(pcm, sampleRate)

        // Verify minimum header size
        assertTrue(wavBytes.size == 44 + pcm.size * 2)

        val buffer = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        val riff = ByteArray(4)
        buffer.get(riff)
        assertEquals("RIFF", String(riff))

        val chunkSize = buffer.int
        assertEquals(36 + pcm.size * 2, chunkSize)

        val wave = ByteArray(4)
        buffer.get(wave)
        assertEquals("WAVE", String(wave))

        val fmt = ByteArray(4)
        buffer.get(fmt)
        assertEquals("fmt ", String(fmt))

        val subchunk1Size = buffer.int
        assertEquals(16, subchunk1Size)

        val audioFormat = buffer.short
        assertEquals(1.toShort(), audioFormat) // PCM

        val numChannels = buffer.short
        assertEquals(1.toShort(), numChannels) // Mono

        val sRate = buffer.int
        assertEquals(sampleRate, sRate)

        val byteRate = buffer.int
        assertEquals(sampleRate * 2, byteRate)

        val blockAlign = buffer.short
        assertEquals(2.toShort(), blockAlign)

        val bitsPerSample = buffer.short
        assertEquals(16.toShort(), bitsPerSample)

        val dataTag = ByteArray(4)
        buffer.get(dataTag)
        assertEquals("data", String(dataTag))

        val dataSize = buffer.int
        assertEquals(pcm.size * 2, dataSize)
    }

    @Test
    fun testSettingsSchemaContainsTickSetting() {
        val focusDef = SettingsSchema.SC["focus"]
        assertNotNull("Focus settings screen definition must exist", focusDef)

        val sessionGroup = focusDef!!.groups.find { it.heading == "Session" }
        assertNotNull("Session group must exist under Focus settings", sessionGroup)

        val tickItem = sessionGroup!!.items.find { it.key == "f_tick" }
        assertNotNull("f_tick item must be present in Session group", tickItem)
        assertEquals(SettingItemType.TG, tickItem!!.type)
        assertEquals("Ambient tick sound", tickItem.label)
        assertFalse("Default for f_tick should be false", tickItem.defaultBool)
    }

    @Test
    fun testWorkPhasesClassification() {
        val workPhases = setOf(FocusPhase.WORK, FocusPhase.TIMED_FLOW, FocusPhase.OPEN_FLOW)
        val breakPhases = setOf(FocusPhase.SHORT_BREAK, FocusPhase.LONG_BREAK)

        // Ensure proper partition of phases
        assertTrue(workPhases.contains(FocusPhase.WORK))
        assertTrue(workPhases.contains(FocusPhase.TIMED_FLOW))
        assertTrue(workPhases.contains(FocusPhase.OPEN_FLOW))
        assertFalse(workPhases.contains(FocusPhase.SHORT_BREAK))
        assertFalse(workPhases.contains(FocusPhase.LONG_BREAK))

        assertTrue(breakPhases.contains(FocusPhase.SHORT_BREAK))
        assertTrue(breakPhases.contains(FocusPhase.LONG_BREAK))
    }
}
