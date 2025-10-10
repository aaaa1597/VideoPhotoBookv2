package com.tks.videophotobook.settings

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
//import androidx.media3.datasource.DefaultDataSource
//import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.tks.videophotobook.R

class VideoThumbnailPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private var player: ExoPlayer? = null
    private var isPrepared = false

    @OptIn(UnstableApi::class)
    fun setVideoUri(uri: Uri) {
        useController = true            /* Controllerを使う */
        controllerAutoShow = false      /* 状態変化などで自動表示しない */
        controllerHideOnTouch = true    /* タッチで表示 */
        controllerShowTimeoutMs = 2000  /* 2秒で非表示 */
//      showController()                /* 明示的に表示 */

        player?.release()
        player = null
        player = ExoPlayer.Builder(context).build().also { exoPlayer ->
            setPlayer(exoPlayer)
            this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
//          /* ExoPlayerがDocument URIに対応してないのに備えてInputStream経由で再生 */
//          val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
//                                .createMediaSource(mediaItem)
//          exoPlayer.setMediaSource(mediaSource)
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            exoPlayer.playWhenReady = true
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) {
                    e.printStackTrace()
                }
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    Log.d("aaaaa", "playWhenReady=${playWhenReady} playbackState: $playbackState")
                }
//                override fun onRenderedFirstFrame() {
//                    exoPlayer.pause()
//                    exoPlayer.seekTo(1000)
//                    exoPlayer.removeListener(this)
//                    isPrepared = true
//                }
            })
        }
    }

    @OptIn(UnstableApi::class)
    fun setFileNotFoundMp4() {
        player?.release()
        player = null
        player = ExoPlayer.Builder(context).build().also { exoPlayer ->
            setPlayer(exoPlayer)
            this.useController = false
            this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            val uri = "android.resource://${context.packageName}/${R.raw.double_tap_to_choose_a_video}"
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    exoPlayer.pause()
                    exoPlayer.seekTo(0)
                    exoPlayer.removeListener(this)
                }
            })
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying)    it.pause()
            else if (isPrepared) it.play()
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

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
