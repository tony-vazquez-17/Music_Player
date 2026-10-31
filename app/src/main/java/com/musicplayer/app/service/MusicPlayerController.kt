package com.musicplayer.app.service

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaSession
import com.musicplayer.app.data.model.Song



class MusicPlayerController(
    private val context: Context,
    private val onSongFinished: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private val audioEnhancer = AudioEnhancerService()
    private val mediaSession: MediaSession = 
        MediaSession.Builder(context, QueueAwarePlayer(exoPlayer, onNext, onPrevious)).build()

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
        PlayerSessionHolder.session = mediaSession
        context.startService(Intent(context, PlaybackService::class.java))
    }

    fun prepareSongFromStart(song: Song, autoPlay: Boolean) {
        val mediaItem = MediaItem.Builder()
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()
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
        PlayerSessionHolder.session = null
        context.stopService(Intent(context, PlaybackService::class.java))
        mediaSession.release()
        audioEnhancer.release()
        exoPlayer.release()
    }
}