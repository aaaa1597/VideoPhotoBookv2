package com.tks.videophotobook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.tks.videophotobook.databinding.FragmentMainBinding
import com.tks.videophotobook.settings.MARKER_VIDEO_MAP_JSON
import java.io.File

class MainFragment : Fragment() {
    private lateinit var _binding: FragmentMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* marker_video_map.json(=マーカー/動画紐付け情報)の存在チェック */
        val file = File(requireContext().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        if ( !file.exists()) {
            /* 存在しない場合、SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
            findNavController().navigate(R.id.action_mainFragment_to_settingFragment_slide)
        }
    }
}
