package com.reflex.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Low-latency ambient tick sound synthesizer and player for FocusTimerService.
 *
 * Characteristics:
 * - Synthesizes a subtle, quiet mechanical/wood click (18ms PCM waveform, 0.20f peak amplitude).
 * - Plays via SoundPool for ultra-low latency hardware mixing, with AudioTrack MODE_STATIC as instant fallback.
 * - Uses USAGE_ASSISTANCE_SONIFICATION with no AudioManager focus request so background music/podcasts continue uninterrupted.
 * - Entirely self-contained; zero network calls or bundled asset dependencies.
 */
class FocusTickPlayer(private val context: Context) {

    companion object {
        private const val TAG = "FocusTickPlayer"
        const val SAMPLE_RATE = 44100
        const val DURATION_MS = 18
        const val AMPLITUDE = 0.20 // 0.15f - 0.25f range
        const val TICK_FILE_NAME = "reflex_focus_tick.wav"

        fun synthesizeTickWaveform(
            sampleRate: Int = SAMPLE_RATE,
            durationMs: Int = DURATION_MS,
            peakAmplitude: Double = AMPLITUDE
        ): ShortArray {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val pcmSamples = ShortArray(numSamples)

            val tau1 = 0.0035
            val tau2 = 0.0020
            val attackDuration = 0.0006

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val attack = if (t < attackDuration) t / attackDuration else 1.0

                val wave1 = sin(2.0 * PI * 1200.0 * t) * exp(-t / tau1) * 0.75
                val wave2 = sin(2.0 * PI * 2600.0 * t) * exp(-t / tau2) * 0.25

                var amp = peakAmplitude * attack * (wave1 + wave2)

                val samplesFromEnd = numSamples - 1 - i
                if (samplesFromEnd < 40) {
                    amp *= (samplesFromEnd / 40.0)
                }

                val sampleVal = (amp * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmSamples[i] = sampleVal.toShort()
            }
            return pcmSamples
        }

        fun createWavBytes(pcmSamples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
            val numSamples = pcmSamples.size
            val dataSize = numSamples * 2
            val totalChunkSize = 36 + dataSize
            val byteRate = sampleRate * 1 * 2

            val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            // RIFF header
            buffer.put('R'.code.toByte())
            buffer.put('I'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.put('F'.code.toByte())
            buffer.putInt(totalChunkSize)
            buffer.put('W'.code.toByte())
            buffer.put('A'.code.toByte())
            buffer.put('V'.code.toByte())
            buffer.put('E'.code.toByte())

            // fmt chunk
            buffer.put('f'.code.toByte())
            buffer.put('m'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put(' '.code.toByte())
            buffer.putInt(16)
            buffer.putShort(1.toShort()) // PCM format
            buffer.putShort(1.toShort()) // Mono channel
            buffer.putInt(sampleRate)
            buffer.putInt(byteRate)
            buffer.putShort(2.toShort()) // Block align
            buffer.putShort(16.toShort()) // Bits per sample

            // data chunk
            buffer.put('d'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.put('t'.code.toByte())
            buffer.put('a'.code.toByte())
            buffer.putInt(dataSize)

            for (sample in pcmSamples) {
                buffer.putShort(sample)
            }

            return buffer.array()
        }
    }

    private var soundPool: SoundPool? = null
    private var soundId: Int = 0
    private var currentStreamId: Int = 0
    @Volatile
    private var isSoundPoolLoaded: Boolean = false

    private var fallbackTrack: AudioTrack? = null

    init {
        try {
            val pcmSamples = synthesizeTickWaveform(SAMPLE_RATE, DURATION_MS, AMPLITUDE)
            initAudioTrackFallback(pcmSamples)
            initSoundPool(pcmSamples)
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
        }
    }

    private fun initSoundPool(pcmSamples: ShortArray) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build().apply {
                    setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0 && sampleId == soundId) {
                            isSoundPoolLoaded = true
                        }
                    }
                }

            val tickFile = getOrCreateTickWavFile(context, pcmSamples, SAMPLE_RATE)
            if (tickFile != null && tickFile.exists()) {
                soundId = soundPool?.load(tickFile.absolutePath, 1) ?: 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SoundPool", e)
        }
    }

    private fun initAudioTrackFallback(pcmSamples: ShortArray) {
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSamples = maxOf(pcmSamples.size, (minBufSize / 2) + 1)
            val paddedSamples = ShortArray(bufferSamples)
            System.arraycopy(pcmSamples, 0, paddedSamples, 0, pcmSamples.size)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(paddedSamples, 0, paddedSamples.size)
            fallbackTrack = track
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing fallback AudioTrack", e)
        }
    }

    /**
     * Plays a single tick sound.
     */
    fun playTick() {
        try {
            if (isSoundPoolLoaded && soundId != 0) {
                val sp = soundPool ?: return
                currentStreamId = sp.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
                return
            }

            // Fallback to AudioTrack if SoundPool is still decoding or unavailable
            val track = fallbackTrack
            if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing tick", e)
        }
    }

    /**
     * Stops any currently playing tick immediately.
     */
    fun stop() {
        try {
            if (currentStreamId != 0) {
                soundPool?.stop(currentStreamId)
                currentStreamId = 0
            }
            val track = fallbackTrack
            if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tick sound", e)
        }
    }

    /**
     * Releases audio resources.
     */
    fun release() {
        try {
            stop()
            soundPool?.release()
            soundPool = null
            isSoundPoolLoaded = false
            soundId = 0

            fallbackTrack?.release()
            fallbackTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing FocusTickPlayer", e)
        }
    }

    private fun getOrCreateTickWavFile(context: Context, pcmSamples: ShortArray, sampleRate: Int): File? {
        return try {
            val file = File(context.cacheDir, TICK_FILE_NAME)
            if (!file.exists() || file.length() == 0L) {
                val wavBytes = createWavBytes(pcmSamples, sampleRate)
                FileOutputStream(file).use { it.write(wavBytes) }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write WAV file to cache", e)
            null
        }
    }
}
