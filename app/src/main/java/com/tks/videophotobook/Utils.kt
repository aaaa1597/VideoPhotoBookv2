package com.tks.videophotobook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.number.IntegerWidth
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

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
            } catch (e: Exception) {
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
            val canvas = android.graphics.Canvas(outputBitmap)
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

        /* 要素数3のBitmap配列(冒頭,中間,終盤)を返却 */
        fun get3Thumbnail(context: Context, uri: Uri): Array<Bitmap?> {
            /* Uri から一時ファイルにコピー */
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_video_000", ".mp4", context.cacheDir)
            inputStream!!.copyTo(tempFile.outputStream())
            inputStream.close()

            /* mp4の再生時間を取得 */
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(tempFile.absolutePath)
            }
            val durationMs = retriever.let {
                val durStr = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                return@let durStr?.toLong() ?: 0L
            }
            val targetUs3 : Long = ((durationMs * 1000) * 0.03).toLong()
            val targetUs50: Long =  (durationMs * 1000) / 2
            val targetUs90: Long = ((durationMs * 1000) * 0.9 ).toLong()

            val bitmapArray = Array<Bitmap?>(3) { idx ->
                val retriever = MediaMetadataRetriever()
                val timeUs: Long = when(idx) {0 -> targetUs3 ; 1 -> targetUs50 ; else -> targetUs90}
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
                retbitmap
            }
            return bitmapArray
        }
    }
}