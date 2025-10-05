package com.tks.videophotobook.settings

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

class VideoThumbnailPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private var player: ExoPlayer? = null
    private var isPrepared = false

    @OptIn(UnstableApi::class)
    fun setVideoUri(uri: Uri) {
        player = ExoPlayer.Builder(context).build().also { exoPlayer ->
            this.player = exoPlayer
            this.player = exoPlayer
            this.useController = false
            this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            this.player = exoPlayer

            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            exoPlayer.playWhenReady = true
            exoPlayer.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    exoPlayer.removeListener(this)
                    isPrepared = true
                }
            })
        }
    }

    fun releasePlayer() {
        player?.release()
        player = null
        isPrepared = false
    }

    fun play() {
        if (isPrepared) player?.play()
    }

    fun pause() {
        player?.pause()
    }
}
