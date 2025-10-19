package com.tks.videophotobook.settings

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tks.videophotobook.R
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.DialogMarkerVideoBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.getValue

const val ARG_SET = "arg_set"
class LinkingSettingsDialog: DialogFragment() {
    private var _binding: DialogMarkerVideoBinding? = null
    private val binding get() = _binding!!
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
        if(savedInstanceState == null) {
            val set = requireArguments().getParcelable(ARG_SET, MarkerVideoSet::class.java)!!
            _viewModel.mutableMarkerVideoSet = MutableStateFlow(set)
            _viewModel.mutableIsEnable.value = (set.videoUri != Uri.EMPTY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogMarkerVideoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindInfoToDialog(requireContext(), binding, _viewModel.mutableMarkerVideoSet.value)
        collectMarkerVideoSetFlow(binding)
        collectIsEnableFlow(binding)

        view.doOnLayout {
            Log.d("aaaaa", "     top               X=${it.x}, Y=${it.y}, W=${it.width}, H=${it.height}")
            Log.d("aaaaa", "     txt_toptitle      X=${binding.txtVideotitle.x}, Y=${binding.txtVideotitle.y}, W=${binding.txtVideotitle.width}, H=${binding.txtVideotitle.height}")
            Log.d("aaaaa", "     txt_targettitle   X=${binding.txtTargettitle.x}, Y=${binding.txtTargettitle.y}, W=${binding.txtTargettitle.width}, H=${binding.txtTargettitle.height}")
            Log.d("aaaaa", "     igv_markerpreview X=${binding.igvMarkerpreview.x}, Y=${binding.igvMarkerpreview.y}, W=${binding.igvMarkerpreview.width}, H=${binding.igvMarkerpreview.height}")
        }
    }

    private fun bindData(context: Context, binding: DialogMarkerVideoBinding, item: MarkerVideoSet) {
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
    }

    private fun bindInfoToDialog(context: Context, binding: DialogMarkerVideoBinding, item: MarkerVideoSet) {
        /* データ紐付け */
        bindData(context, binding, item)

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
                    val thumbnails3 = arrayOfNulls<Bitmap?>(3)
                    lifecycleScope.launch {
                        thumbnails3[0] = Utils.getThumbnail(requireContext(), uri, 3_000_000) /* 3秒 */
                        /* アニメーション開始 */
                        showFlashAnimation(binding, thumbnails3[0])
                    }
                    /* Videoセット */
                    lifecycleScope.launch {
                        binding.pyvVideoThumbnail2.setVideoUri(uri,true, false, true)
                        _viewModel.mutableIsEnable.value = true
                    }
                    /* 動画から中盤/終盤のサムネイルを取得 */
                    lifecycleScope.launch {
                        val thumbnails2 = Utils.get2ThumbnailMidAndEnd(requireContext(), uri)
                        thumbnails3[1] = thumbnails2[0]
                        thumbnails3[2] = thumbnails2[1]
                    }
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
        val container = binding.dialogTopView
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

        /* 拡縮比（縦横比を維持しつつ、はみ出さない最大スケール） */
        val fromWidth = binding.pyvVideoThumbnail2.width.toFloat()
        val fromHeight = binding.pyvVideoThumbnail2.height.toFloat()
        val toWidth = binding.igvMarkerpreview.width.toFloat()
        val toHeight = binding.igvMarkerpreview.height.toFloat()
        val scale = minOf(toWidth / fromWidth, toHeight / fromHeight)

        val path = Path().apply {
            val imageViewStartX = binding.pyvVideoThumbnail2.left.toFloat()
            val imageViewStartY = binding.pyvVideoThumbnail2.top.toFloat()
            val targetX = binding.igvMarkerpreview.left.toFloat()
            val targetY = binding.igvMarkerpreview.top.toFloat()
            val dx = targetX - imageViewStartX
            val dy = targetY - imageViewStartY - (binding.igvMarkerpreview.height*(1-scale))
            Log.d("aaaaa", "dx=$dx, dy=$dy imageViewStartX=$imageViewStartX, imageViewStartY=$imageViewStartY, targetX=$targetX, targetY=$targetY")
            moveTo(0f, 0f)
            cubicTo(300f, 0f, 300f, dy, dx, dy)
        }

        val pathAnim = ObjectAnimator.ofFloat(imageView, View.TRANSLATION_X, View.TRANSLATION_Y, path).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
        }

        val scaleXAnim = ObjectAnimator.ofFloat(imageView, View.SCALE_X, 1f, 1.3f, scale).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
        }
        val scaleYAnim = ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 1f, 1.3f, scale).apply {
            duration = 1500
            interpolator = DecelerateInterpolator()
        }

        /* アニメーションを同時に実行 */
        AnimatorSet().apply {
            playTogether(pathAnim, scaleXAnim, scaleYAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lifecycleScope.launch {
                        /* 200ms待ってから */
                        delay(2000)
                        container.removeView(blackView)
                        container.removeView(imageView)
                        container.removeView(touchBlocker)
                    }
                }
            })
            start()
        }
    }

    /* ViewModelのisEnableを収集してUIの有効/無効を切り替え */
    private fun collectMarkerVideoSetFlow(binding: DialogMarkerVideoBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                /* mutableMarkerVideoSetを収集 */
                _viewModel.mutableMarkerVideoSet.collect { set ->
                    bindData(requireContext(), binding, _viewModel.markerVideoSet.value)
                }
            }
        }
    }

    private fun collectIsEnableFlow(binding: DialogMarkerVideoBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                /* ViewModelのStateFlowを収集 */
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