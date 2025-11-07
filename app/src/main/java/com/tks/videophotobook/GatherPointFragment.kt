package com.tks.videophotobook

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.tks.videophotobook.ar.ArActivity
import com.tks.videophotobook.databinding.FragmentGatherpointBinding
import com.tks.videophotobook.settings.MARKER_VIDEO_MAP_JSON
import com.tks.videophotobook.settings.MarkerVideoSet
import java.io.File

class GatherPointFragment : Fragment() {
    private var _binding: FragmentGatherpointBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentGatherpointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imvMenu.setOnClickListener {
            findNavController().navigate(R.id.action_gatherPointFragment_to_settingFragment_slide)
        }

        binding.btnStart.setOnClickListener {
            val intent = Intent(requireContext(), ArActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        /* marker_video_map.json(=マーカー/動画紐付け情報)の存在チェック */
        val file = File(requireContext().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        if ( !file.exists()) {
            /* ファイルが存在しない → SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
            GuidedDialog().show(parentFragmentManager, "GuidedDialog")
            return
        }

        val jsonList = file.readText()
        if (jsonList.isEmpty() || jsonList == "[]") {
            /* データ空 → SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
            GuidedDialog().show(parentFragmentManager, "GuidedDialog")
            return
        }

        val markerVideoSetList = MarkerVideoSet.loadFromJsonFile(file)
        val allEmpty = markerVideoSetList.all { it.videoUri == Uri.EMPTY }
        /* 何も設定されてない → SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
        if(allEmpty) {
            GuidedDialog().show(parentFragmentManager, "GuidedDialog")
            return
        }

        /* 設定されてる → プログレス消去 */
        binding.pgbLoading.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
