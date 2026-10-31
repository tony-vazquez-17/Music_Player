package com.musicplayer.app.service

import android.content.Intent
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

object PlayerSessionHolder {
    var session: MediaSession? = null
}

//Hace que los botones anterior/siguiente de la notificación usen tu cola.
class QueueAwarePlayer(
    player: Player,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit
) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

    override fun isCommandAvailable(command: Int): Boolean =
        availableCommands.contains(command)

    override fun hasNextMediaItem(): Boolean = true
    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() = onNext()
    override fun seekToPrevious() = onPrevious()
    override fun seekToNextMediaItem() = onNext()
    override fun seekToPreviousMediaItem() = onPrevious()
}

class PlaybackService : MediaSessionService() {

    override fun onCreate() {
        super.onCreate()
        PlayerSessionHolder.session?.let { addSession(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        PlayerSessionHolder.session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = PlayerSessionHolder.session?.player
        if (player == null || !player.playWhenReady) stopSelf()
    }
}