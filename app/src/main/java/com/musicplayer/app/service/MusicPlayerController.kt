package com.musicplayer.app.service

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicplayer.app.data.model.Song

class MusicPlayerController(
    context: Context,
    private val onSongFinished: () -> Unit
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val audioEnhancer = AudioEnhancerService()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioEnhancer.attachAlwaysOnEnhancement(audioSessionId)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onSongFinished()
                }
            }
        })
        audioEnhancer.attachAlwaysOnEnhancement(exoPlayer.audioSessionId)
    }

    fun prepareSongFromStart(song: Song, autoPlay: Boolean) {
        val mediaItem = MediaItem.fromUri(song.uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(0L)
        exoPlayer.playWhenReady = autoPlay
    }

    fun togglePlayPause(): Boolean {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
        return exoPlayer.isPlaying
    }

    fun seekToFraction(fraction: Float) {
        val duration = exoPlayer.duration
        if (duration > 0) {
            exoPlayer.seekTo((duration * fraction).toLong())
        }
    }

    fun isPlaying(): Boolean = exoPlayer.isPlaying

    fun getCurrentPositionMs(): Long = exoPlayer.currentPosition.coerceAtLeast(0L)

    fun getDurationMs(): Long = exoPlayer.duration.coerceAtLeast(0L)

    fun getCurrentProgress(): Float {
        val duration = exoPlayer.duration
        val position = exoPlayer.currentPosition
        return if (duration > 0) {
            (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    fun release() {
        audioEnhancer.release()
        exoPlayer.release()
    }
}