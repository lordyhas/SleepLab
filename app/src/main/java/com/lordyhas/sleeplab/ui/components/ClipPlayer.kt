package com.lordyhas.sleeplab.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Plays one snore clip at a time; [playingPath] drives play/stop icons. */
class ClipPlayer {
    var playingPath by mutableStateOf<String?>(null)
        private set

    private var player: MediaPlayer? = null

    fun toggle(path: String) {
        if (playingPath == path) {
            stop()
            return
        }
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(path)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            playingPath = path
        } catch (e: Exception) {
            Log.w("ClipPlayer", "Cannot play $path", e)
            stop()
        }
    }

    fun stop() {
        player?.release()
        player = null
        playingPath = null
    }
}

/** A [ClipPlayer] released automatically when the composable leaves the screen. */
@Composable
fun rememberClipPlayer(): ClipPlayer {
    val player = remember { ClipPlayer() }
    DisposableEffect(player) { onDispose { player.stop() } }
    return player
}
