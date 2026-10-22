package com.musicplayer.app.service

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer

class AudioEnhancerService {
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null

    fun attachAlwaysOnEnhancement(audioSessionId: Int) {
        if (audioSessionId == 0) return

        try {
            loudnessEnhancer?.release()
            loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(450)
                enabled = true
            }

            bassBoost?.release()
            bassBoost = BassBoost(0, audioSessionId).apply {
                setStrength(650.toShort())
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        loudnessEnhancer?.release()
        bassBoost?.release()
    }
}