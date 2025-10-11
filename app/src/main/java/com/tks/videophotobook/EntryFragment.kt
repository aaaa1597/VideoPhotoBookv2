package com.tks.videophotobook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.tks.videophotobook.databinding.FragmentEntryBinding
import com.tks.videophotobook.settings.MARKER_VIDEO_MAP_JSON
import java.io.File

class EntryFragment : Fragment() {
    private var _binding: FragmentEntryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentEntryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /* marker_video_map.json(=マーカー/動画紐付け情報)の存在チェック */
        val file = File(requireContext().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        if ( !file.exists()) {
            /* 存在しない場合、SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.no_data)
                .setMessage(R.string.no_bind_data)
                .setPositiveButton(android.R.string.ok) { dialog, which ->
                    dialog.dismiss()
                    findNavController().navigate(R.id.action_mainFragment_to_settingFragment_slide)
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
