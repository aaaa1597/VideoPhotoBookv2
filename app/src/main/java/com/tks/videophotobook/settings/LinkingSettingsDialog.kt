package com.tks.videophotobook.settings

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.DialogMarkerVideoBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlin.getValue

const val ARG_SET = "arg_set"
class LinkingSettingsDialog: DialogFragment() {
    private var _binding: DialogMarkerVideoBinding? = null
    private val binding get() = _binding!!
    private lateinit var set: MarkerVideoSet
    private val _viewModel: SetDialogViewModel by activityViewModels()
    /* なんのTargetNameでどっち(image/video)のファイル選択したかを一時保持 */
    private var pendingTargetNameAndMimeType: Pair<String, String>? = null
    private var onFileUrlPicked: ((Uri, Int) -> Unit)? = null
    /* ファイル選択ランチャー */
    private val _pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* ファイルリストの戻り */
        result ->
            val (targetName, mimeType) = pendingTargetNameAndMimeType ?: throw RuntimeException("No way!! pendingTargetNameAndMimeType is null")
            pendingTargetNameAndMimeType = null
            if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
                onFileUrlPicked = null
                return@registerForActivityResult
            }
            /* 単一のファイルが選択された */
            val uri = result.data!!.data!!
            Log.d("aaaaa", "file URI: $uri")
            /* URIに対する永続権限を取得 */
            try { requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (e: SecurityException) { throw RuntimeException("SecurityException!! ${e.printStackTrace()}") }

            /* Uri動画の再生チェック */
            if (mimeType.startsWith("video"))
                Utils.checkVideoCompatibilitybyPlayback(requireContext(), uri, onFileUrlPicked!!)
            else
                onFileUrlPicked!!(uri, 0)
            onFileUrlPicked = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        set = requireArguments().getParcelable(ARG_SET, MarkerVideoSet::class.java)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogMarkerVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindInfoToDialog(requireContext(), binding, set)
        collectIsEnableFlow(binding)
        _viewModel.mutableIsEnable.value = (set.videoUri != Uri.EMPTY)
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
            binding.pyvVideoThumbnail2.setVideoUri(item.videoUri, true, false, true)
        }
        else {
            binding.txtVideoname.text = context.getString(R.string.video_none)
            val uri = "android.resource://${context.packageName}/${R.raw.double_tap_to_choose_a_video}".toUri()
            binding.pyvVideoThumbnail2.setVideoUri(uri, false, false, true)
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
        fun setVideoAndGet3Thumbnail(set: MarkerVideoSet) {
            lifecycleScope.launch {
                val (uri, errCode) = pickFileAndWaitForUri("video/*", set)
                if(errCode != 0) {
                    /* 再生不可 */
                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.could_not_played)
                        .setMessage("${getString(R.string.error)}: $errCode \n${getString(R.string.select_another)}" )
                        .setPositiveButton(android.R.string.ok) { dialog, which -> dialog.dismiss() }
                        .show()
                }
                else {
                    /* 再生可能 */
                    binding.pyvVideoThumbnail2.setVideoUri(uri,true, false, true)
                    _viewModel.mutableIsEnable.value = true
                    /* 動画から3つのサムネイルを取得 */
                    val thumbnails3 = Utils.get3Thumbnail(requireContext(), uri)
                    /* アニメーション開始 */
                    showFlashAnimation(binding, thumbnails3[0])
                }
            }
        }

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                setVideoAndGet3Thumbnail(item)
                return true
            }
        })
        /* PlayerViewをダブルタップ */
        binding.pyvVideoThumbnail2.setOnTouchListener {
            v, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP)
                    v.performClick()
                true
        }
        /* キャンセル */
        binding.btnCancel.setOnClickListener {
            dismiss()
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

    /* Flashアニメ → Image縮小 → BottomSheetDialogFragment表示 */
    private fun showFlashAnimation(binding: DialogMarkerVideoBinding, thumbnail: android.graphics.Bitmap?) {
        val container = binding.topView
//        /* すでに"FlashView" が存在していれば何も */
//        if ((0 until container.childCount).any {
//                container.getChildAt(it).tag == "FlashView"}) {
//            return
//        }

        /* pyv_video_thumbnail2と同じ制約を作成 */
        val params = ConstraintLayout.LayoutParams(
                binding.pyvVideoThumbnail2.width,
                binding.pyvVideoThumbnail2.height).apply {
            topToBottom = R.id.txt_videotitle
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        }

        val blackView = View(container.context).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = params
        }

        val imageView = ImageView(container.context).apply {
            setImageBitmap(thumbnail)
            layoutParams = params
        }

        val flashView = View(container.context).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 1f
            tag = "FlashView"
            layoutParams = params
        }

        val touchBlocker = View(container.context).apply {
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = params
            isClickable = true
            isFocusable = true
        }

        container.addView(blackView)
        container.addView(imageView)
        container.addView(flashView)
        container.addView(touchBlocker)

        /* 一旦真っ白になって徐々に消える */
        flashView.animate()
            .alpha(0f)
            .setDuration(600)
            .withEndAction {
                container.removeView(flashView)
            }
            .start()

        val path = Path().apply {
            val startX = binding.pyvVideoThumbnail2.left.toFloat()
            val startY = binding.pyvVideoThumbnail2.top.toFloat()
            moveTo(startX, startY)
            val endX = binding.igvMarkerpreview.left.toFloat()
            val endY = binding.igvMarkerpreview.top.toFloat()
            cubicTo(0f, -300f, -200f, -300f , endX, endY)
        }

        val animator = ObjectAnimator.ofFloat(imageView, View.X, View.Y, path).apply {
            duration = 1500
            interpolator = AccelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    container.removeView(blackView)
                    container.removeView(imageView)
                    container.removeView(touchBlocker)
                }
            })
            start()
        }
    }

    /* ViewModelのisEnableを収集してUIの有効/無効を切り替え */
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
                            binding.viwDoubleTapGuide.visibility = View.GONE
                        }
                        false -> {
                            binding.viwDisableMarker .visibility = View.VISIBLE
                            binding.viwDisableComment.visibility = View.VISIBLE
                            binding.viwDoubleTapGuide.visibility = View.VISIBLE
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // TODO ViewModel 更新はここで。
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.pyvVideoThumbnail2.releasePlayer()
        _binding = null
    }
    companion object {
        fun newInstance(set: MarkerVideoSet): LinkingSettingsDialog {
            return LinkingSettingsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SET, set)
                }
            }
        }
    }
}