package com.tks.videophotobook

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            GuidedDialog().show(parentFragmentManager, "GuidedDialog")
        }

        binding.imvMenu.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingFragment_slide)
        }

//        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//        val currentidxstr = prefs.getString(CURRENT_INDEX, "0000")
//        /* CURRENT_INDEXデータ存在チェック */
//        val dir = requireContext().getExternalFilesDir(currentidxstr)
//        if (dir != null && !dir.exists()) {
//            /* CURRENT_INDEXフォルダが存在しない場合は、"0000"フォルダに遷移 */
//            val currentidxstr0000 = "0000"
//            prefs.edit { putString(CURRENT_INDEX, currentidxstr0000) }
//            val dir0000 = requireContext().getExternalFilesDir(currentidxstr0000)
//            /* "0000"フォルダが存在しない場合は、システム不整合なので全データ削除 */
//            if (dir0000 != null && !dir0000.exists()) {
//                val targetDir = requireContext().getExternalFilesDir(null)
//                targetDir?.listFiles()?.forEach { child ->
//                    Utils.deleteRecursively(child)
//                }
//            }
//            /*  */
//            dir?.mkdirs()
//            /* 存在しない場合、SettingFragmentを表示(マーカー/動画紐付け情報を生成する)*/
//            GuidedDialog().show(parentFragmentManager, "GuidedDialog")
//        }
//        val file = File(requireContext().getExternalFilesDir(currentidxstr), MARKER_VIDEO_MAP_JSON)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
