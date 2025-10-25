package com.tks.videophotobook.staging

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.tks.videophotobook.R
import com.tks.videophotobook.databinding.FragmentStagingBinding

class StagingFragment : Fragment() {
    private var _binding: FragmentStagingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentStagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                view.viewTreeObserver.removeOnPreDrawListener(this)

                /* Fragment起動アニメーション開始で呼ばれる */
                view.postOnAnimation {
                    Log.d("aaaaa", "アニメーション開始！")
                    /* 設定時間と同じ時間を設定する。 */
                    view.postDelayed({
                        Log.d("aaaaa", "アニメーション完了！")
                        binding.viwTop.background = null
                    }, resources.getInteger(R.integer.config_navAnimTime400).toLong()) // アニメーション時間と合わせて調整
                }

                return true
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}