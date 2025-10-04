package com.tks.videophotobook

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

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

    }
}