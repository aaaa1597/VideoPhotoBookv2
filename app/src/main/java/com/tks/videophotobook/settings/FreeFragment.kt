package com.tks.videophotobook.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.graphics.scale
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
import kotlinx.coroutines.CompletableDeferred

class FreeFragment : Fragment() {
    private lateinit var _binding: FragmentFreeBinding
    private val _viewModel: SettingViewModel by activityViewModels()
    private lateinit var _markerVideoSetAdapter: MarkerVideoSetAdapter
    /* なんのTargetNameでどっち(image/video)のファイル選択したかを一時保持 */
    private var pendingTargetNameAndMimeType: Pair<String, String>? = null
    private var onfileUrlPicked: ((Uri) -> Unit)? = null

    /* ファイル選択ランチャー */
    private val _pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* ファイルリストの戻り */
        result ->
            val (targetName, mimeType) = pendingTargetNameAndMimeType ?: throw RuntimeException("No way!! pendingTargetNameAndMimeType is null")
            pendingTargetNameAndMimeType = null
            if (result.resultCode != Activity.RESULT_OK) throw RuntimeException("No way!! resultCode isn't Activity.RESULT_OK")
            if (result.data==null)                       throw RuntimeException("No way!! result.data is null")
            if (result.data!!.data == null)              throw RuntimeException("No way!! result.data!!.data is null")

            /* 単一のファイルが選択された */
            val uri = result.data!!.data!!
            Log.d("aaaaa", "file URI: $uri")
            /* URIに対する永続権限を取得 */
            try { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) { throw RuntimeException("SecurityException!! ${e.printStackTrace()}") }

            /* ViewModelのリスト更新 */
            targetName.let { targetName ->
                val currentList = _viewModel.markerVideoSetList.value.toMutableList()
                val index = currentList.indexOfFirst { it.targetName == targetName }
                if (index == -1) return@let

//                /* targetImageUri を更新 */ ← 保存押下ですればよくって、ここでは実行する必要がない。
//                val updatedItem = when {
//                    mimeType.startsWith("image") -> currentList[index].copy(targetImageUri = uri)
//                    mimeType.startsWith("video") -> currentList[index].copy(videoUri = uri)
//                    else -> throw RuntimeException("Unknown mimeType: $mimeType")
//                }
//                currentList[index] = updatedItem
//                _viewModel.updateMarkerVideoSetList(currentList)
                /* Uriを返却 */
                onfileUrlPicked?.invoke(uri)
                onfileUrlPicked = null
            }
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
                setInfoToDialogView(requireContext(), dialogView, markerVideoSet)
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                dialog.setOnDismissListener {
                    val playerView = dialogView.findViewById<VideoThumbnailPlayerView>(R.id.pyv_video_thumbnail2)
                    playerView.releasePlayer()
                }
                dialog.show()
        }
        _markerVideoSetAdapter = MarkerVideoSetAdapter(requireContext(), onItemClickedItemProperties)
        _binding.recyclerViewMarkerVideo.adapter = _markerVideoSetAdapter
        _binding.recyclerViewMarkerVideo.layoutManager = LinearLayoutManager(context)

        /* Flow収集 */
        collectMarkerVideoSetListFlow()
    }

    private fun setInfoToDialogView(context: Context, dialogView: View, item: MarkerVideoSet) {
        val playerView = dialogView.findViewById<VideoThumbnailPlayerView>(R.id.pyv_video_thumbnail2)
        /* ARマーカーID */
        dialogView.findViewById<TextView>(R.id.txt_targetname).text = item.targetName
        /* ARマーカー画像 */
        if(Utils.isUriValid(context, item.targetImageUri))
            dialogView.findViewById<ImageView>(R.id.igv_markerpreview).setImageURI(item.targetImageUri)
        else {
            item.targetImageUri = "".toUri()
            dialogView.findViewById<ImageView>(R.id.igv_markerpreview).setImageResource(item.targetImageTemplateResId)
        }
        /* 動画名/動画ファイル */
        if(Utils.isUriValid(context, item.videoUri)) {
            dialogView.findViewById<TextView>(R.id.txt_videoname).text = Utils.getFileNameFromUri(context, item.videoUri)
            playerView.setVideoUri(item.videoUri)
        }
        else {
            item.videoUri = "".toUri()
            dialogView.findViewById<TextView>(R.id.txt_videoname).text = context.getString(R.string.video_none)
            playerView.setFileNotFoundMp4()
        }

        dialogView.findViewById<TextView>(R.id.etv_comment).text = item.comment

        val launchFilePicker: (String, MarkerVideoSet) -> Unit = {
                mimeType, item ->
                    pendingTargetNameAndMimeType = item.targetName to mimeType
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = mimeType
                        /* 初期表示ディレクトリ指定 */
                        putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    }
                    /* 生成Intentで起動 */
                    _pickFileLauncher.launch(intent)
        }

        suspend fun pickFileAndWaitForUri(mimeType: String, item: MarkerVideoSet): Uri {
            val deferredUri = CompletableDeferred<Uri>()
            /* コールバックでUriを受け取ったらComplete */
            onfileUrlPicked = { uri ->
                deferredUri.complete(uri)
            }
            /* ファイル選択画面を起動 */
            launchFilePicker(mimeType, item)
            /* Uriが設定されるまで待つ */
            return deferredUri.await()
        }

        /* マーカー画像設定 */
        dialogView.findViewById<ImageView>(R.id.igv_markerpreview).setOnClickListener {
            lifecycleScope.launch {
                val uri = pickFileAndWaitForUri("image/*", item)
                /* 取得UriからBitmap生成 */
                val originalBitmap = Utils.decodeBitmapFromUri(requireContext(), uri)
                val resizedBitmap = Utils.resizeBitmapWithAspectRatio(originalBitmap!!, 1280, 720)
                /* 画像合成 */
                val resizedFrame = BitmapFactory.decodeResource(resources, item.targetImageTemplateResId)
                                    .scale(resizedBitmap.width, resizedBitmap.height)
                val canvas = Canvas(resizedBitmap)
                canvas.drawBitmap(resizedFrame, 0f, 0f, null)
                dialogView.findViewById<ImageView>(R.id.igv_markerpreview).setImageBitmap(resizedBitmap)
            }
        }

        /* 再生動画設定 */
        fun setVideo(set: MarkerVideoSet) {
            lifecycleScope.launch {
                val uri = pickFileAndWaitForUri("video/*", set)
                val playerView = dialogView.findViewById<VideoThumbnailPlayerView>(R.id.pyv_video_thumbnail2)
                playerView.setVideoUri(uri)
//              playerView.setOnClickListener { playerView.togglePlayPause() }
            }
        }

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                setVideo(item)
                return true
            }
        })
        dialogView.findViewById<VideoThumbnailPlayerView>(R.id.pyv_video_thumbnail2).setOnTouchListener {
            v, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP)
                    v.performClick()
                true
        }
        /* キャンセル */
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            TODO("キャンセル押下")
        }
        /* 保存 */
        dialogView.findViewById<Button>(R.id.btnSave).setOnClickListener {
            TODO("保存押下")
        }
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
}

/* Adapterクラス */
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
