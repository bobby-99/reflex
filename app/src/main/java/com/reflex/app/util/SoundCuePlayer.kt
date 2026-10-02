package com.reflex.app.util

import android.media.AudioManager
import android.media.ToneGenerator

object SoundCuePlayer {

    fun playTingChime() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
        } catch (e: Exception) {
            AppLog.w("SoundCuePlayer", "Failed to play audio cue tone", e)
        }
    }
}
