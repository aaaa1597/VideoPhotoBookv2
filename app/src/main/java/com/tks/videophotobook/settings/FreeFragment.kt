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
import androidx.fragment.app.viewModels
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tks.videophotobook.databinding.DialogMarkerVideoBinding
import kotlinx.coroutines.CompletableDeferred

class FreeFragment : Fragment() {
    private lateinit var _binding: FragmentFreeBinding
    private val _settingViewModel: SettingViewModel by activityViewModels()
    private val _viewModel: FreeViewModel by viewModels()
    private lateinit var _markerVideoSetAdapter: MarkerVideoSetAdapter
    /* なんのTargetNameでどっち(image/video)のファイル選択したかを一時保持 */
    private var pendingTargetNameAndMimeType: Pair<String, String>? = null
    private var onFileUrlPicked: ((Uri, Int) -> Unit)? = null

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
            try { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (e: SecurityException) { throw RuntimeException("SecurityException!! ${e.printStackTrace()}") }

            /* Uri動画の再生チェック */
            if (mimeType.startsWith("video"))
                checkVideoCompatibilitybyPlayback(requireContext(), uri, onFileUrlPicked!!)
            else
                onFileUrlPicked!!(uri, 0)
            onFileUrlPicked = null
    }

    private fun checkVideoCompatibilitybyPlayback(context: Context, uri: Uri, onResult: (Uri, Int) -> Unit) {
        /* ExoPlayerで再生できるか試す */
        val player = ExoPlayer.Builder(context).build()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.d("aaaaa", "Error occurred!! errCode=${error.errorCode} getErrorCodeName()= ${PlaybackException.getErrorCodeName(error.errorCode)}")
                player.release()
                onResult(uri, error.errorCode)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    /* 再生成功 -> DRMなし かつ サポート形式 */
                    Log.d("aaaaa", "ok. video is available!!")
                    player.release()
                    onResult(uri, 0)
                }
            }
        })
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentFreeBinding.inflate(inflater, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val onItemDoubleTapItemProperties: (MarkerVideoSet) -> Unit = {
            markerVideoSet ->
                showMarkerVideoSetDialog( requireContext(), markerVideoSet)
        }
        _markerVideoSetAdapter = MarkerVideoSetAdapter(requireContext(), onItemDoubleTapItemProperties)
        _binding.recyclerViewMarkerVideo.adapter = _markerVideoSetAdapter
        _binding.recyclerViewMarkerVideo.layoutManager = LinearLayoutManager(context)

        /* Flow収集 */
        collectMarkerVideoSetListFlow()
    }

    private fun showMarkerVideoSetDialog(context: Context, makerVideoSet: MarkerVideoSet) {
        /* Uriの有効判定(無効なら空Uriにする) */
        if (!Utils.isUriValid(requireContext(), makerVideoSet.targetImageUri)){
            Log.d("aaaaa", "Warning!! Invalid targetImageUri: ${makerVideoSet.targetImageUri}!! Set to empty.")
            makerVideoSet.targetImageUri = "".toUri()
        }
        else if (!Utils.isUriValid(requireContext(), makerVideoSet.videoUri)) {
            Log.d("aaaaa", "Warning!! Invalid videoUri: ${makerVideoSet.videoUri}!! Set to empty.")
            makerVideoSet.videoUri = "".toUri()
        }
        val binding = DialogMarkerVideoBinding.inflate(layoutInflater)
        bindInfoToDialog(requireContext(), binding, makerVideoSet)
        collectIsEnableFlow(binding)
        _viewModel.mutableIsEnable.value = (makerVideoSet.videoUri != "".toUri())
        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        dialog.setOnDismissListener {
            binding.pyvVideoThumbnail2.releasePlayer()
        }
        dialog.show()
    }

    private fun bindInfoToDialog(context: Context, binding: DialogMarkerVideoBinding, item: MarkerVideoSet) {
        /* ARマーカーID */
        binding.txtTargetname.text = item.targetName
        /* ARマーカー画像 */
        if(Utils.isUriValid(context, item.targetImageUri))
            binding.igvMarkerpreview.setImageURI(item.targetImageUri)
        else {
            binding.igvMarkerpreview.setImageResource(item.targetImageTemplateResId)
        }
        /* 動画名/動画ファイル */
        if(Utils.isUriValid(context, item.videoUri)) {
            binding.txtVideoname.text = Utils.getFileNameFromUri(context, item.videoUri)
            binding.pyvVideoThumbnail2.setVideoUri(item.videoUri)
        }
        else {
            binding.txtVideoname.text = context.getString(R.string.video_none)
            binding.pyvVideoThumbnail2.setFileNotFoundMp4()
        }

        binding.etvComment.setText(item.comment)

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

        suspend fun pickFileAndWaitForUri(mimeType: String, item: MarkerVideoSet): Pair<Uri, Int> {
            val deferredUri = CompletableDeferred<Pair<Uri, Int>>()
            /* コールバックでUriを受け取ったらComplete */
            onFileUrlPicked = { uri, errCode ->
                deferredUri.complete(uri to errCode)
            }
            /* ファイル選択画面を起動 */
            launchFilePicker(mimeType, item)
            /* Uriが設定されるまで待つ */
            return deferredUri.await()
        }

        /* マーカー画像設定 */
       fun setMarker(set: MarkerVideoSet) {
            lifecycleScope.launch {
                val (uri,_) = pickFileAndWaitForUri("image/*", set)
                Log.d("aaaaa", "OK!!! Maker URI: $uri")
                /* 取得UriからBitmap生成 */
                val originalBitmap = Utils.decodeBitmapFromUri(requireContext(), uri)
                val resizedBitmap = Utils.resizeBitmapWithAspectRatio(originalBitmap!!, 1280, 720)
                /* 画像合成 */
                val resizedFrame = BitmapFactory.decodeResource(resources, set.targetImageTemplateResId)
                                    .scale(resizedBitmap.width, resizedBitmap.height)
                val canvas = Canvas(resizedBitmap)
                canvas.drawBitmap(resizedFrame, 0f, 0f, null)
                binding.igvMarkerpreview.setImageBitmap(resizedBitmap)
            }
        }
        val gestureDetectorForMarker = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                setMarker(item)
                return true
            }
        })
        @Suppress("ClickableViewAccessibility")
        binding.igvMarkerpreview.setOnTouchListener {
            v, event ->
                gestureDetectorForMarker.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP)
                    v.performClick()
                true
        }

        /* 再生動画設定 */
        fun setVideo(set: MarkerVideoSet) {
            lifecycleScope.launch {
                val (uri, errCode) = pickFileAndWaitForUri("video/*", set)
                if(errCode != 0) {
                    /* 再生不可 */
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.could_not_played)
                        .setMessage("${getString(R.string.error)}: ${errCode} \n${getString(R.string.select_another)}" )
                        .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
                        .show()
                }
                else {
                    /* 再生可能 */
                    binding.pyvVideoThumbnail2.setVideoUri(uri)
                    _viewModel.mutableIsEnable.value = true
                }
            }
        }

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: android.view.MotionEvent): Boolean {
                setVideo(item)
                return true
            }
        })
        binding.pyvVideoThumbnail2.setOnTouchListener {
            v, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP)
                    v.performClick()
                true
        }
        /* キャンセル */
        binding.btnCancel.setOnClickListener {
            TODO("キャンセル押下")
        }
        /* 保存 */
        binding.btnSave.setOnClickListener {
//            /* ViewModelのリスト更新 */
//            targetName.let { targetName ->
//                val currentList = _settingViewModel.markerVideoSetList.value.toMutableList()
//                val index = currentList.indexOfFirst { it.targetName == targetName }
//                if (index == -1) return@let
//
////                /* targetImageUri を更新 */ ← 保存押下ですればよくって、ここでは実行する必要がない。
////                val updatedItem = when {
////                    mimeType.startsWith("image") -> currentList[index].copy(targetImageUri = uri)
////                    mimeType.startsWith("video") -> currentList[index].copy(videoUri = uri)
////                    else -> throw RuntimeException("Unknown mimeType: $mimeType")
////                }
////                currentList[index] = updatedItem
////                _viewModel.updateMarkerVideoSetList(currentList)
//            /* Uriを返却 */
//            onFileUrlPicked?.invoke(uri)
//            onFileUrlPicked = null
        }
    }

    private fun collectMarkerVideoSetListFlow() {
        /* UIのライフサイクルがSTARTED以上(画面表示中)の間のみFlowを収集 */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ViewModelのStateFlowを収集
                _settingViewModel.markerVideoSetList.collect { list ->
                    /* Flowから新しいリストが放出されたら、Adapterにセットして画面を更新 */
                    _markerVideoSetAdapter.submitList(list)
                }
            }
        }
    }

    private fun collectIsEnableFlow(binding: DialogMarkerVideoBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ViewModelのStateFlowを収集
                _viewModel.isEnable.collect { isEnable ->
                    /* Flowから新しい値が放出されたら、UIの有効/無効を切り替え */
                    binding.txtTargetname.   isEnabled = isEnable
                    binding.igvMarkerpreview.isEnabled = isEnable
                    binding.etvComment.      isEnabled = isEnable
                    binding.btnSave.         isEnabled = isEnable
                    when(isEnable) {
                        true  -> {
                            binding.viwDisableMarker .visibility = View.GONE
                            binding.viwDisableComment.visibility = View.GONE
                        }
                        false -> {
                            binding.viwDisableMarker .visibility = View.VISIBLE
                            binding.viwDisableComment.visibility = View.VISIBLE
                        }
                    }
                    val alpha = if (isEnable) 1.0f else 0.3f
                    binding.txtTargetname.   alpha = alpha
                    binding.igvMarkerpreview.alpha = alpha
                    binding.etvComment.      alpha = alpha
                    binding.btnSave.         alpha = alpha
                }
            }
        }
    }
}


/* Adapterクラス */
class MarkerVideoSetAdapter(private val context: Context, private val onItemDoubleTap: (MarkerVideoSet) -> Unit) :
    ListAdapter<MarkerVideoSet, MarkerVideoSetAdapter.MarkerVideoSetViewHolder>(MarkerVideoSetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarkerVideoSetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_marker_video, parent, false)
        return MarkerVideoSetViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarkerVideoSetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(context, item, onItemDoubleTap)
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

        fun bind(context: Context, item: MarkerVideoSet, onItemDoubleTap: (MarkerVideoSet) -> Unit) {
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

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    onItemDoubleTap(item.copy())
                    return true
                }
            })

            /* クリックリスナーの設定 */
            @Suppress("ClickableViewAccessibility")
            topCdv.setOnTouchListener {
                v, event ->
                    gestureDetector.onTouchEvent(event)
                    if (event.action == MotionEvent.ACTION_UP)
                        v.performClick()
                true
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
