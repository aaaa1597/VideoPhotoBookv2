package com.tks.videophotobook

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tks.videophotobook.settings.MarkerVideoSet
import com.tks.videophotobook.settings.SetDialogViewModel
import kotlinx.coroutines.CompletableDeferred

class Utils {
    companion object {
        /* Uriパス有効チェック */
        fun isUriValid(context: Context, uri: Uri): Boolean {
            return try {
                if (uri.scheme == "file") {
                    /* file:// の場合は File に変換して確認 */
                    File(uri.path!!).exists()
                } else {
                    /* content:// の場合は InputStream を開けるか確認 */
                    context.contentResolver.openInputStream(uri)?.close()
                    true
                }
            } catch (_: Exception) {
                false
            }
        }

        /* URIからファイル名取得 */
        fun getFileNameFromUri(context: Context, uri: Uri): String {
            return when (uri.scheme) {
                "content" -> {
                    val cursor = context.contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (it.moveToFirst() && nameIndex != -1) {
                            it.getString(nameIndex)
                        } else ""
                    } ?: ""
                }
                "file" -> {
                    File(uri.path ?: "").name ?: ""
                }
                else -> ""
            }
        }

        /* UriからBitmap生成 */
        fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                inputStream?.use {
                    BitmapFactory.decodeStream(it)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        /* Bitmapリサイズ生成 */
        fun resizeBitmapWithAspectRatio(src: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
            val scale = minOf(
                targetWidth.toFloat() / src.width,
                targetHeight.toFloat() / src.height
            )
            val newWidth = (src.width * scale).toInt()
            val newHeight = (src.height * scale).toInt()
            /* リサイズしたbitmapを作成 */
            val scaledBitmap = src.scale(newWidth, newHeight)
            /* 最終的な指定サイズのBitmapを作成し中央に配置 */
            val outputBitmap = createBitmap(targetWidth, targetHeight)
            val canvas = Canvas(outputBitmap)
            /* 背景を白で塗りつぶす */
            canvas.drawColor(android.graphics.Color.WHITE)
            /* 中央に配置 */
            val left = (targetWidth - newWidth) / 2f
            val top = (targetHeight - newHeight) / 2f
            canvas.drawBitmap(scaledBitmap, left, top, null)
            return outputBitmap
        }

        fun checkVideoCompatibilitybyPlayback(context: Context, uri: Uri, onResult: (Uri, Int) -> Unit) {
            /* ExoPlayerで再生できるか試す */
            val player = ExoPlayer.Builder(context).build()
            player.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.e("aaaaa", "Error occurred!! errCode=${error.errorCode} getErrorCodeName()= ${PlaybackException.getErrorCodeName(error.errorCode)}")
                    player.release()
                    onResult(uri, error.errorCode)
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        /* 再生成功 -> DRMなし かつ サポート形式 */
                        Log.i("aaaaa", "ok. video is available!!")
                        player.release()
                        onResult(uri, 0)
                    }
                }
            })
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()
        }

        /* 動画からサムネイル取得 */
        fun getThumbnail(context: Context, uri: Uri, timeUs: Long): Bitmap? {
            /* Uri から一時ファイルにコピー */
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_video_000", ".mp4", context.cacheDir)
            inputStream!!.copyTo(tempFile.outputStream())
            inputStream.close()

            val retriever = MediaMetadataRetriever()
            var retbitmap: Bitmap? = null
            try {
                retriever.setDataSource(tempFile.absolutePath)
                retbitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
            finally {
                retriever.release()
            }
            tempFile.delete()
            return retbitmap
        }

        /* 動画再生時間取得 */
        fun getDurationMsVideo(context: Context, uri: Uri): Long {
            /* mp4の再生時間を取得 */
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(context, uri)
            }
            val durationMs = retriever.let {
                val durStr = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                return@let durStr?.toLong() ?: 0L
            }
            return durationMs
        }

        fun get2ThumbnailMidAndEnd(context: Context, uri: Uri): Array<Bitmap?> {
            /* mp4の再生時間を取得 */
            val durationMs = getDurationMsVideo(context, uri)
            val targetUs50: Long =  (durationMs * 1000) / 2
            val targetUs90: Long = ((durationMs * 1000) * 0.9 ).toLong()

            val bitmapArray = Array(2) { idx ->
                val timeUs: Long = when(idx) {0 -> targetUs50 ; else -> targetUs90}
                val retbitmap: Bitmap? = getThumbnail(context, uri, timeUs)
                retbitmap
            }
            return bitmapArray
        }

        fun deleteRecursively(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    deleteRecursively(child)
                }
            }
            file.delete()
        }

        fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap, fileName: String): Uri {
            val externalCacheDir = context.externalCacheDir
            val file = File(externalCacheDir, fileName)
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            /* content://始まりのUriを返却 */
            return FileProvider.getUriForFile(
                context,"${context.packageName}.fileprovider",file )

        }

        /* なんのTargetNameでどっち(image/video)のファイル選択したかを一時保持 */
        var pendingTargetNameAndMimeType: Pair<String, String>? = null
        var onFileUrlPicked: ((Uri, Int) -> Unit)? = null
        val launchFilePicker: (String, MarkerVideoSet, ActivityResultLauncher<Intent>) -> Unit = {
            mimeType, item, pickFileLauncher ->
                pendingTargetNameAndMimeType = item.targetName to mimeType
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = mimeType
                    /* 初期表示ディレクトリ指定 */
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                }
                /* 生成Intentで起動 */
                pickFileLauncher.launch(intent)
        }

        suspend fun pickFileAndWaitForUri(mimeType: String, item: MarkerVideoSet, pickFileLauncher: ActivityResultLauncher<Intent>): Pair<Uri, Int> {
            val deferredUri = CompletableDeferred<Pair<Uri, Int>>()
            /* コールバックでUriを受け取ったらComplete */
            onFileUrlPicked = { uri, errCode ->
                deferredUri.complete(uri to errCode)
            }
            /* ファイル選択画面を起動 */
            launchFilePicker(mimeType, item, pickFileLauncher)
            /* Uriが設定されるまで待つ */
            return deferredUri.await()
        }

        fun setTargetImageAndSaveBitmap(context: Context, thumbnailBitmap: Bitmap, viewModel: SetDialogViewModel) {
            val resizedBitmap = resizeBitmapWithAspectRatio(thumbnailBitmap, 1280, 720)
            /* 画像合成 */
            val resizedFrame = BitmapFactory.decodeResource(context.resources, viewModel.mutableMarkerVideoSet.value.targetImageTemplateResId)
                .scale(resizedBitmap.width, resizedBitmap.height)
            val canvas = Canvas(resizedBitmap)
            canvas.drawBitmap(resizedFrame, 0f, 0f, null)
            /* キャッシュ領域にBitmapを保存 */
            val savedUri = saveBitmapToCacheAndGetUri(context, resizedBitmap, "${viewModel.mutableMarkerVideoSet.value.targetName}_marker.png")
            /* マーカー表示更新 */
            viewModel.mutableMarkerVideoSet.value = viewModel.mutableMarkerVideoSet.value.copy(
                targetImageUri = savedUri   /* Uriだけ更新 */
            )
        }

        fun getErrorMessage(context: Context, errcode: Int): String {
            return when(errcode) {
                0     -> context.getString(R.string.no_error)
                1     -> context.getString(R.string.vu_engine_creation_error_device_not_supported)
                2     -> context.getString(R.string.vu_engine_creation_error_permission_error)
                3     -> context.getString(R.string.vu_engine_creation_error_license_error)
                4     -> context.getString(R.string.vu_engine_creation_error_initialization)
                0x100 -> context.getString(R.string.vu_engine_creation_error_driver_config_load_error)
                0x101 -> context.getString(R.string.vu_engine_creation_error_device_not_supported)
                0x200 -> context.getString(R.string.vu_engine_creation_error_license_config_missing_key)
                0x201 -> context.getString(R.string.vu_engine_creation_error_license_config_invalid_key)
                0x202 -> context.getString(R.string.vu_engine_creation_error_license_config_no_network_permanent)
                0x203 -> context.getString(R.string.vu_engine_creation_error_license_config_no_network_transient)
                0x204 -> context.getString(R.string.vu_engine_creation_error_license_config_bad_request)
                0x205 -> context.getString(R.string.vu_engine_creation_error_license_config_key_canceled)
                0x206 -> context.getString(R.string.vu_engine_creation_error_license_config_product_type_mismatch)
                0x207 -> context.getString(R.string.vu_engine_creation_error_license_config_unknown)
                0x300 -> context.getString(R.string.vu_engine_creation_error_render_config_unsupported_backend)
                0x301 -> context.getString(R.string.vu_engine_creation_error_render_config_failed_to_set_video_bg_viewport)
                0x510 -> context.getString(R.string.vu_engine_creation_error_platform_android_config_initialization_error)
                0x511 -> context.getString(R.string.vu_engine_creation_error_platform_android_config_invalid_activity)
                0x512 -> context.getString(R.string.vu_engine_creation_error_platform_android_config_invalid_java_vm)
                0x600 -> context.getString(R.string.vu_engine_error_invalid_license)
                0x601 -> context.getString(R.string.vu_engine_error_camera_device_lost)
                0x602 -> context.getString(R.string.vu_engine_error_platform_fusion_provider_info_invalidated)
                0x1001-> context.getString(R.string.error_instance_already_exists)
                0x1002-> context.getString(R.string.could_not_apply_platform_specific_configuration)
                0x1003-> context.getString(R.string.could_not_configure_rendering)
                0x1004-> context.getString(R.string.error_handler_data_could_not_be_added_to_configuration)
                0x1005-> context.getString(R.string.error_setting_clipping_planes_for_projection)
                0x1006-> context.getString(R.string.error_creating_device_pose_observer)
                0x1007-> context.getString(R.string.error_creating_image_target_observer)
                else  -> context.getString(R.string.vu_engine_creation_error_initialization)
            }
        }
    }
}