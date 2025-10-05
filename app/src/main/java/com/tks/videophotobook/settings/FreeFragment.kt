package com.tks.videophotobook.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tks.videophotobook.databinding.FragmentFreeBinding
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import kotlinx.coroutines.launch
import kotlin.getValue
import androidx.core.net.toUri

class FreeFragment : Fragment() {
    private lateinit var _binding: FragmentFreeBinding
    private val _viewModel: SettingViewModel by activityViewModels()
    private lateinit var _markerVideoSetAdapter: MarkerVideoSetAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentFreeBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onItemClickedItemProperties: (MarkerVideoSet) -> Unit = {
            markerVideoSet ->
            val dialogView = layoutInflater.inflate(R.layout.dialog_marker_video, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()
            dialog.show()
        }
        _markerVideoSetAdapter = MarkerVideoSetAdapter(requireContext(), onItemClickedItemProperties)
        _binding.recyclerViewMarkerVideo.adapter = _markerVideoSetAdapter
        _binding.recyclerViewMarkerVideo.layoutManager = LinearLayoutManager(context)

        /* Flow収集 */
        collectMarkerVideoSetListFlow()
    }

    private fun collectMarkerVideoSetListFlow() {
        /* UIのライフサイクルがSTARTED以上(画面表示中)の間のみFlowを収集 */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ViewModelのStateFlowを収集
                _viewModel.markerVideoSetList.collect { list ->
                    /* Flowから新しいリストが放出されたら、Adapterにセットして画面を更新 */
                    _markerVideoSetAdapter.submitList(list)
                }
            }
        }
    }

    class MarkerVideoSetAdapter(private val context: Context, private val onItemClicked: (MarkerVideoSet) -> Unit) :
        ListAdapter<MarkerVideoSet, MarkerVideoSetAdapter.MarkerVideoSetViewHolder>(MarkerVideoSetDiffCallback()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerVideoSetViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_marker_video, parent, false)
            return MarkerVideoSetViewHolder(view)
        }

        override fun onBindViewHolder(holder: MarkerVideoSetViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(context, item, onItemClicked)
        }

        class MarkerVideoSetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val topCdv: CardView = itemView.findViewById(R.id.cdv_top)
            private val targetInfoFly: FrameLayout = itemView.findViewById(R.id.fly_targetinfo)
            private val targetNameTxt: TextView = itemView.findViewById(R.id.txt_targetname)
            private val targetImageImv: ImageView = itemView.findViewById(R.id.imv_targetImage)
            private val videoInfoFly: FrameLayout = itemView.findViewById(R.id.fly_videoInfo)
            private val videoName: TextView = itemView.findViewById(R.id.txt_videoname)
            private val videothumbnailPyv: VideoThumbnailPlayerView = itemView.findViewById(R.id.pyv_video_thumbnail)
            private val videothumbnailImv: ImageView = itemView.findViewById(R.id.imv_video_thumbnail)
            private val commentTxt: TextView = itemView.findViewById(R.id.txt_comment)

            fun bind(context: Context, item: MarkerVideoSet, onItemClicked: (MarkerVideoSet) -> Unit) {
                /* ARマーカーID */
                targetNameTxt.text = item.targetName
                /* ARマーカー画像 */
                if(Utils.isUriValid(context, item.targetImageUri))
                    targetImageImv.setImageURI(item.targetImageUri)
                else {
                    item.targetImageUri = "".toUri()
                    targetImageImv.setImageResource(item.targetImageTemplateResId)
                }
                /* 動画名/動画ファイル */
                if(Utils.isUriValid(context, item.videoUri)) {
                    videoName.text = Utils.getFileNameFromUri(context, item.videoUri)
                    videothumbnailImv.visibility = View.GONE
                    videothumbnailPyv.visibility = View.VISIBLE
                    videothumbnailPyv.setVideoUri(item.videoUri)
                }
                else {
                    item.videoUri = "".toUri()
                    videoName.text = context.getString(R.string.video_none)
                    videothumbnailPyv.visibility = View.GONE
                    videothumbnailImv.visibility = View.VISIBLE
                    videothumbnailImv.setImageResource(R.drawable.videofilenotfound)
                }

                commentTxt.text = item.comment

                /* クリックリスナーの設定 */
                topCdv.setOnClickListener {
                    onItemClicked(item)
                }
            }
        }
    }

    /* リストの変更を検出するためのコールバック */
    class MarkerVideoSetDiffCallback : DiffUtil.ItemCallback<MarkerVideoSet>() {
        override fun areItemsTheSame(oldItem: MarkerVideoSet, newItem: MarkerVideoSet): Boolean {
            /* IDが同じなら同じアイテムと見なす */
            return oldItem.targetName == newItem.targetName
        }

        override fun areContentsTheSame(oldItem: MarkerVideoSet, newItem: MarkerVideoSet): Boolean {
            /* 内容が完全に一致するかどうかを確認 */
            return oldItem == newItem
        }
    }
}