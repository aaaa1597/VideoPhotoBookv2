package com.tks.videophotobook.ar

import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import com.tks.videophotobook.cameraPerformAutoFocus
import com.tks.videophotobook.cameraRestoreAutoFocus
import com.tks.videophotobook.checkHit
import com.tks.videophotobook.configureRendering
import com.tks.videophotobook.deinitAR
import com.tks.videophotobook.deinitRendering
import com.tks.videophotobook.initRendering
import com.tks.videophotobook.setVideoTexture
import com.tks.videophotobook.nativeSetVideoSize
import com.tks.videophotobook.renderFrame
import com.tks.videophotobook.setFullScreenMode
import com.tks.videophotobook.setTextures
import com.tks.videophotobook.SettingViewModel
import com.tks.videophotobook.databinding.FragmentArBinding
import com.tks.videophotobook.startAR
import com.tks.videophotobook.stopAR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.schedule

class MainFragment : Fragment() {
    private var _binding: FragmentArBinding? = null
    private val binding get() = _binding!!
    private val _stagingViewModel: StagingViewModel by activityViewModels()
    private var _nowPlayingTarget: String = ""
    private var isFullScreenMode = false
    private lateinit var _exoPlayer: ExoPlayer
    private var exoPlayerIsPlaying = false
    private lateinit var _surfaceTexture: SurfaceTexture
    private lateinit var _surface: Surface
    private val gestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            @UnstableApi
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val targetName = checkHit(e.x, e.y, binding.viwGlsurface.width.toFloat(), binding.viwGlsurface.height.toFloat())
                if (targetName != _nowPlayingTarget && targetName != "") {
                    /* 動画差し替え */
                    _nowPlayingTarget = targetName
                    switchMedia(targetName)
                }
                else {
                    /* 再生/停止/早送り/巻戻しコントローラ表示/非表示 */
                    if (binding.viwPlayerControls.isFullyVisible)
                        binding.viwPlayerControls.hide()
                    else
                        binding.viwPlayerControls.show()
                }

                cameraPerformAutoFocus()
                Timer("RestoreAutoFocus", false).schedule(2000) {
                    cameraRestoreAutoFocus()
                }
                return true
            }

            @UnstableApi
            override fun onDoubleTap(e: MotionEvent): Boolean {
                super.onDoubleTap(e)
                val targetName = checkHit(e.x, e.y, binding.viwGlsurface.width.toFloat(), binding.viwGlsurface.height.toFloat())
                if (targetName != _nowPlayingTarget && targetName != "") {
                    /* 動画差し替え */
                    _nowPlayingTarget = targetName
                    switchMedia(targetName)
                }
                else {
                    /* フルスクリーンモード切替 */
                    isFullScreenMode = !isFullScreenMode
                    setFullScreenMode(isFullScreenMode)
                }
                return true
            }
        })
    }

    /* 指定Target動画に差替え */
    private fun switchMedia(target: String) {
        Log.d("aaaaa", "target=${target}")
        _exoPlayer.stop()
        _exoPlayer.clearMediaItems()
        Log.d("aaaaa", "target=${target} VideoUri=${SettingViewModel.getVideoUri(target)} ==Uri.EMPTY(${SettingViewModel.getVideoUri(target)== Uri.EMPTY})")
        val mediaItem = MediaItem.fromUri(SettingViewModel.getVideoUri(target)!!)
        _exoPlayer.setMediaItem(mediaItem)
        _exoPlayer.prepare()
        _exoPlayer.playWhenReady = true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v("aaaaa", "1-1. MainFragment::onCreate()")
        super.onCreate(savedInstanceState)
        /* スクリーンが暗くならないようにフラグを追加 */
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        /* MainActivityの背景にnull設定 */
        requireActivity().window.decorView.background = null

        SettingViewModel.logoutMarkerVideoSet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.v("aaaaa", "1-2. MainFragment::onCreateView()")
        _binding = FragmentArBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.v("aaaaa", "1-3. MainFragment::onViewCreated()")
        super.onViewCreated(view, savedInstanceState)

        /* タップ/ダブルタップ定義 */
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                gestureDetector.onTouchEvent(event)
                v.performClick()
                true
            }
            else
                false
        }

        /* Create the ExoPlayer */
        _exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE /* Loop Playback. */
            playWhenReady = false /* Start playback immediately. */

            addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    /* Pass the video size to the C++ side. */
                    nativeSetVideoSize(videoSize.width, videoSize.height)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    Log.d("aaaaa", "onIsPlayingChanged isPlaying=$isPlaying")
                    exoPlayerIsPlaying = isPlaying
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("aaaaa", "error!! ExoPlayer error: ${error.errorCodeName}, ${error.errorCode}, ${error.message}")
                }
            })
        }

        /* GLSurfaceView初期化 */
        _stagingViewModel.addLogStr(resources.getString(R.string.init_glsurfaceview_s))
        binding.viwGlsurface.setEGLContextClientVersion(3)
        binding.viwGlsurface.holder.setFormat(PixelFormat.TRANSLUCENT)
        binding.viwGlsurface.setEGLConfigChooser(8,8,8,8,0,0)
        binding.viwGlsurface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                Log.v("aaaaa", "1-6. MainFragment::GLSurfaceView::onSurfaceCreated()")
                initRendering()
            }

            @OptIn(UnstableApi::class)
            override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                Log.v("aaaaa", "1-7. MainFragment::GLSurfaceView::onSurfaceChanged()")
                /* Pass rendering parameters to Vuforia Engine */
                configureRendering(width, height, resources.configuration.orientation, requireActivity().display.rotation)
                /* pause.pngテクスチャ設定 */
                setTextures(_stagingViewModel.pauseWidth, _stagingViewModel.pauseHeight, _stagingViewModel.pauseTexture)

                val textureId = setVideoTexture()
                if (textureId < 0)
                    throw RuntimeException("Failed to create native texture")

                lifecycleScope.launch(Dispatchers.Main) {
                    /* Initialize the surfaceTexture/Surface */
                    _surfaceTexture = SurfaceTexture(textureId)
                    _surface = Surface(_surfaceTexture)
                    _exoPlayer.setVideoSurface(_surface)
                    binding.viwPlayerControls.player = _exoPlayer
                    binding.viwPlayerControls.bringToFront()
                }
            }

            override fun onDrawFrame(gl: GL10) {
//                Log.v("aaaaa", "1-8. MainFragment::GLSurfaceView::onDrawFrame()")
                if(exoPlayerIsPlaying)
                    _surfaceTexture.updateTexImage()

                /* OpenGL rendering of Video Background and augmentations is implemented in native code */
                val delectedTarget = renderFrame(_nowPlayingTarget)
                if(delectedTarget == "waiting...") return

                if(_nowPlayingTarget!=delectedTarget && delectedTarget!="")
                    Log.d("aaaaa", "!!! Detected Target Changed !!! targetName=$_nowPlayingTarget -> $delectedTarget")

                if(_nowPlayingTarget!="" && delectedTarget=="") {
                    _nowPlayingTarget = delectedTarget
                    CoroutineScope(Dispatchers.Main).launch {
                        _exoPlayer.pause()
                    }
                }
                else if(_nowPlayingTarget != delectedTarget) {
                    _nowPlayingTarget = delectedTarget
                    /* loadingIndicatorは非表示に */
                    CoroutineScope(Dispatchers.Main).launch {
                        switchMedia(delectedTarget)
                    }
                }
            }
        })
        binding.viwGlsurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.v("aaaaa", "1-9. MainFragment::GLSurfaceView::surfaceDestroyed()")
                deinitRendering()
            }
        })
        _stagingViewModel.addLogStr(resources.getString(R.string.init_glsurfaceview_s))
    }

    override fun onStart() {
        Log.v("aaaaa", "1-4. MainFragment::onStart()")
        super.onStart()
        /* ステータスバー非表示 */
        val controller = requireActivity().window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        Log.v("aaaaa", "1-5. MainFragment::onResume()")
        super.onResume()
        /* vuforia開始 */
        val retErr = startAR()
        if(retErr != 0) {
            /* Vuforia初期化失敗 */
            val titlestr:String = resources.getString(R.string.init_vuforia_err)
            val errstr:String = Utils.getErrorMessage(requireContext(),retErr)
            AlertDialog.Builder(requireContext())
                .setTitle(titlestr)
                .setMessage(errstr)
                .setPositiveButton(R.string.ok) {
                        dialog, which ->
                    dialog.dismiss()
                    requireActivity().finish()
                }
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.v("aaaaa", "1-10. MainFragment::onPause()")
        /* Stop Vuforia Engine and call parent to navigate back */
        stopAR()
    }

    override fun onStop() {
        Log.v("aaaaa", "1-11. MainFragment::onStop()")
        super.onStop()
        /* ステータスバー表示に戻す */
        val controller = requireActivity().window.insetsController ?: return
        controller.show(WindowInsets.Type.statusBars())
    }

    override fun onDestroyView() {
        Log.v("aaaaa", "1-11. MainFragment::onDestroyView()")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.v("aaaaa", "1-12. MainFragment::onDestroy()")
        super.onDestroy()
        /* フラグの解除 */
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        deinitAR()
        _exoPlayer.release()
        _surfaceTexture.release()
        _surface.release()
    }
}