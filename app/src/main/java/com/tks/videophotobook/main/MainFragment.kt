package com.tks.videophotobook.main

import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.tks.videophotobook.R
import com.tks.videophotobook.cameraPerformAutoFocus
import com.tks.videophotobook.cameraRestoreAutoFocus
import com.tks.videophotobook.checkHit
import com.tks.videophotobook.configureRendering
import com.tks.videophotobook.databinding.FragmentMainBinding
import com.tks.videophotobook.deinitAR
import com.tks.videophotobook.deinitRendering
import com.tks.videophotobook.initRendering
import com.tks.videophotobook.setFullScreenMode
import com.tks.videophotobook.settings.SettingViewModel
import com.tks.videophotobook.staging.StagingViewModel
import com.tks.videophotobook.stopAR
import java.util.Timer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.schedule
import kotlin.getValue
import kotlin.text.toFloat

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private val _stagingViewModel: StagingViewModel by activityViewModels()
    private val _settingViewModel: SettingViewModel by activityViewModels()
    private var _nowPlayingTarget: String = ""
    private var isFullScreenMode = false
    private lateinit var _exoPlayer: ExoPlayer
    private lateinit var _surfaceTexture: SurfaceTexture
    private lateinit var _surface: Surface
    private val gestureDetector by lazy {
        GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            @UnstableApi
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val targetName = checkHit(e.x, e.y,binding.viwGlsurface.width.toFloat(), binding.viwGlsurface.height.toFloat())
                if(targetName != _nowPlayingTarget && targetName != "") {
                    /* 動画差し替え */
                    _nowPlayingTarget = targetName
                    switchMedia(targetName)
                }
                else {
                    /* 再生/停止/早送り/巻戻しコントローラ表示/非表示 */
                    if( binding.viwPlayerControls.isFullyVisible)
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
                val targetName = checkHit(e.x, e.y,binding.viwGlsurface.width.toFloat(), binding.viwGlsurface.height.toFloat())
                if(targetName != _nowPlayingTarget && targetName != "") {
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
        _exoPlayer.stop()
        _exoPlayer.clearMediaItems()
        val mediaItem = MediaItem.fromUri(_settingViewModel.getVideoUri(target)!!)
        _exoPlayer.setMediaItem(mediaItem)
        _exoPlayer.prepare()
        _exoPlayer.playWhenReady = true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* スクリーンが暗くならないようにフラグを追加 */
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        /* MainActivityの背景にnull設定 */
        requireActivity().window.decorView.background = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        /* GLSurfaceView初期化 */
        _stagingViewModel.addLogStr(resources.getString(R.string.init_glsurfaceview_s))
        binding.viwGlsurface.setEGLContextClientVersion(3)
        binding.viwGlsurface.holder.setFormat(PixelFormat.TRANSLUCENT)
        binding.viwGlsurface.setEGLConfigChooser(8,8,8,8,0,0)
        binding.viwGlsurface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                initRendering()
            }
            override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                /* Pass rendering parameters to Vuforia Engine */
                configureRendering(width, height, resources.configuration.orientation, requireActivity().display.rotation)
            }
            override fun onDrawFrame(gl: GL10) {
                TODO("Not yet implemented")
            }
        })
        binding.viwGlsurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                deinitRendering()
            }
        })
        _stagingViewModel.addLogStr(resources.getString(R.string.init_glsurfaceview_s))
    }

    override fun onStart() {
        super.onStart()
        /* 横画面固定 */
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        /* ステータスバー非表示 */
        val controller = requireActivity().window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars())
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onStop() {
        super.onStop()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        /* Stop Vuforia Engine and call parent to navigate back */
        stopAR()
        /* ステータスバー表示に戻す */
        val controller = requireActivity().window.insetsController ?: return
        controller.show(WindowInsets.Type.statusBars())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        /* フラグの解除 */
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        deinitAR()
        _exoPlayer.release()
        _surfaceTexture.release()
        _surface.release()
    }
}