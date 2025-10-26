package com.tks.videophotobook.main

import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentMainBinding
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Prevent screen from dimming */
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viwGlsurface.setEGLContextClientVersion(3)
        binding.viwGlsurface.holder.setFormat(PixelFormat.TRANSLUCENT)
        binding.viwGlsurface.setEGLConfigChooser(8,8,8,8,0,0)
        binding.viwGlsurface.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
                TODO("Not yet implemented")
            }
            override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
                TODO("Not yet implemented")
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}