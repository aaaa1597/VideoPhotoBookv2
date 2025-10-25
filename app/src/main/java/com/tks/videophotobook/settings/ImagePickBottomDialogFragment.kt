package com.tks.videophotobook.settings

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tks.videophotobook.Utils
import com.tks.videophotobook.databinding.FragmentImagePickBottomDialogBinding
import kotlinx.coroutines.launch

class ImagePickBottomDialogFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentImagePickBottomDialogBinding? = null
    private val binding get() = _binding!!
    private val _viewModel: SetDialogViewModel by activityViewModels()
    /* ファイル選択ランチャー */
    private val _pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /* ファイルリストの戻り */
        result ->
            val (targetName, mimeType) = Utils.pendingTargetNameAndMimeType ?: throw RuntimeException("No way!! pendingTargetNameAndMimeType is null")
            Utils.pendingTargetNameAndMimeType = null
            if (result.resultCode != Activity.RESULT_OK || result.data?.data == null) {
                Utils.onFileUrlPicked = null
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
                Utils.checkVideoCompatibilitybyPlayback(requireContext(), uri, Utils.onFileUrlPicked!!)
            else
                Utils.onFileUrlPicked!!(uri, 0)
            Utils.onFileUrlPicked = null
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        _binding = FragmentImagePickBottomDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        collect3ThumbnailFlow(binding)
        bindInfoToDialog(binding, _viewModel.t3Thumbnail.value)
    }

    private fun collect3ThumbnailFlow(binding: FragmentImagePickBottomDialogBinding) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                /* 3Thumbnailを収集 */
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

    fun setMarkerAndSaveBitmap(thumbnailBitmap: Bitmap) {
        lifecycleScope.launch {
            Utils.setTargetImageAndSaveBitmap(requireContext(), thumbnailBitmap, _viewModel)
        }
    }

    private fun bindInfoToDialog(binding: FragmentImagePickBottomDialogBinding, bitmaps: Array<Bitmap?>) {
        bindData(binding, bitmaps)

        binding.imvThumbnail1.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[0] != null) {
                _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy( targetImageUri = Uri.EMPTY )
                /* bitmapをマーカー設定と保存 */
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[0]!!)
                _viewModel.mutableIsVisibilityMarker.value = false
                _viewModel.mutableIsVisibilitySave.value = true
                dismissAllowingStateLoss()
            }
        }
        binding.imvThumbnail2.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[1] != null) {
                _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy( targetImageUri = Uri.EMPTY )
                /* bitmapをマーカー設定と保存 */
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[1]!!)
                _viewModel.mutableIsVisibilityMarker.value = false
                _viewModel.mutableIsVisibilitySave.value = true
                dismissAllowingStateLoss()
            }
        }
        binding.imvThumbnail3.setOnClickListener {
            if(_viewModel.t3Thumbnail.value[2] != null) {
                _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy( targetImageUri = Uri.EMPTY )
                /* bitmapをマーカー設定と保存 */
                setMarkerAndSaveBitmap(_viewModel.t3Thumbnail.value[2]!!)
                _viewModel.mutableIsVisibilityMarker.value = false
                _viewModel.mutableIsVisibilitySave.value = true
                dismissAllowingStateLoss()
            }
        }
        binding.imvAdd.setOnClickListener {
            lifecycleScope.launch {
                val set: MarkerVideoSet = _viewModel.mutableMarkerVideoSet.value
                val (uri,_) = Utils.pickFileAndWaitForUri("image/*", set, _pickFileLauncher)
                Log.d("aaaaa", "OK!!! Maker URI: $uri")
                /* 取得UriからBitmap生成 */
                val selectedBitmap = Utils.decodeBitmapFromUri(requireContext(), uri)
                _viewModel.mutableMarkerVideoSet.value = _viewModel.mutableMarkerVideoSet.value.copy( targetImageUri = Uri.EMPTY )
                /* bitmapをマーカー設定と保存 */
                setMarkerAndSaveBitmap(selectedBitmap!!)
                _viewModel.mutableIsVisibilityMarker.value = false
                _viewModel.mutableIsVisibilitySave.value = true
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}