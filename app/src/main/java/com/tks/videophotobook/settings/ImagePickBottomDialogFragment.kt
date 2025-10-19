package com.tks.videophotobook.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.scale
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.FragmentImagePickBottomDialogBinding
import kotlinx.coroutines.launch
import kotlin.getValue

class ImagePickBottomDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentImagePickBottomDialogBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: SetDialogViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentImagePickBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collect3ThumbnailFlow(binding)
        bindInfoToDialog(requireContext(), binding, _viewModel.t3Thumbnail.value)
    }

    private fun collect3ThumbnailFlow(binding: FragmentImagePickBottomDialogBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                /* mutableMarkerVideoSetを収集 */
                _viewModel.mutable3Thumbnail.collect { bitmaps ->
                    bindData(binding, bitmaps)
                }
            }
        }
    }

    private fun bindData(binding: FragmentImagePickBottomDialogBinding, bitmaps: Array<Bitmap?>) {
        binding.imvThumbnail1.setImageBitmap(bitmaps[0])
        binding.imvThumbnail2.setImageBitmap(bitmaps[1])
        binding.imvThumbnail3.setImageBitmap(bitmaps[2])
    }

    fun setMarkerAndSaveBitmap(bitmap: Bitmap) {
        lifecycleScope.launch {
            val resizedBitmap = Utils.resizeBitmapWithAspectRatio(bitmap, 1280, 720)
            /* 画像合成 */
            val set: MarkerVideoSet = _viewModel.mutableMarkerVideoSet.value
            val resizedFrame = BitmapFactory.decodeResource(resources, set.targetImageTemplateResId)
                .scale(resizedBitmap.width, resizedBitmap.height)
            val canvas = Canvas(resizedBitmap)
            canvas.drawBitmap(resizedFrame, 0f, 0f, null)
            /* キャッシュ領域にBitmapを保存 */
            val savedUri = Utils.saveBitmapToCacheAndGetUri(requireContext(), resizedBitmap, "${set.targetName}_marker.png")
            _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy(
                targetImageUri = savedUri   /* Uriだけ更新 */
            )
        }
    }

    private fun bindInfoToDialog(context: Context, binding: FragmentImagePickBottomDialogBinding, bitmaps: Array<Bitmap?>) {
        bindData(binding, bitmaps)

        binding.imvThumbnail1.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[0] != null)
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[0]!!)
        }
        binding.imvThumbnail2.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[1] != null)
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[1]!!)
        }
        binding.imvThumbnail3.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[2] != null)
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[2]!!)
        }
        binding.imvAdd.setOnClickListener {
//            val (uri,_) = pickFileAndWaitForUri("image/*", set)
//            Log.d("aaaaa", "OK!!! Maker URI: $uri")
//            /* 取得UriからBitmap生成 */
//            val selectedBitmap = Utils.decodeBitmapFromUri(requireContext(), uri)
//            val resizedBitmap = Utils.resizeBitmapWithAspectRatio(selectedBitmap!!, 1280, 720)
//            /* 画像合成 */
//            val resizedFrame = BitmapFactory.decodeResource(resources, set.targetImageTemplateResId)
//                .scale(resizedBitmap.width, resizedBitmap.height)
//            val canvas = Canvas(resizedBitmap)
//            canvas.drawBitmap(resizedFrame, 0f, 0f, null)
//            /* キャッシュ領域にBitmapを保存 */
//            val savedUri = Utils.saveBitmapToCacheAndGetUri(requireContext(), resizedBitmap, "${set.targetName}_marker.png")
//            _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy(
//                targetImageUri = savedUri   /* Uriだけ更新 */
//            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}