package com.tks.videophotobook.settings

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.util.Xml
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import androidx.core.net.toUri
import org.json.JSONArray

@Parcelize
data class MarkerVideoSet(
    /* ARマーカー系定義 */
    var targetName: String = "",
    @DrawableRes var targetImageTemplateResId: Int,
    var targetImageUri: Uri,
    /* Video系定義 */
    var videoUri: Uri,
    var comment: String
): Parcelable {

    fun toJson(): String {
        return """
        {
            "targetName": "$targetName",
            "targetImageTemplateResId": $targetImageTemplateResId,
            "targetImageUri": "$targetImageUri",
            "videoUri": "$videoUri",
            "comment": "$comment"
        }
        """.trimIndent()
    }


    companion object {
        fun loadFromJsonFile(file: File): List<MarkerVideoSet> {
            /* ファイルが存在しない場合、空リストを返す */
            if (!file.exists())
                return emptyList()

            return try {
                val jsonString = file.readText()
                val jsonArray = JSONArray(jsonString)
                val result = mutableListOf<MarkerVideoSet>()

                for (idx in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(idx)
                    result.add(
                        MarkerVideoSet(
                            targetName = obj.optString("targetName", ""),
                            targetImageTemplateResId = obj.optInt("targetImageTemplateResId", 0),
                            targetImageUri = obj.optString("targetImageUri", "").toUri(),
                            videoUri = obj.optString("videoUri", "").toUri(),
                            comment = obj.optString("comment", "")
                        )
                    )
                }
                result            }
            catch (e: Exception) {
                // ファイル読み込みやJSONパースに失敗した場合のエラー処理
                e.printStackTrace()
                // エラーが発生した場合は空のリストを返す
                emptyList()
            }
        }

        /** assets/VideoPhotoBook.xml から ImageTarget name を読み込む */
        fun loadImageTargetNamesFromAssets(context: Context): List<String> {
            val targetNames = mutableListOf<String>()

            /* 1. assetsからファイルを開く */
            var inputStream: InputStream? = null
            try {
                inputStream = context.assets.open("VideoPhotoBook.xml")

                /* 2. XmlPullParserを初期化 */
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)

                var eventType = parser.eventType

                /* 3. XMLをイベント駆動でパース */
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    /* 開始タグの名前が "ImageTarget" の場合 */
                    if (eventType == XmlPullParser.START_TAG && parser.name == "ImageTarget") {
                        /* 属性を走査して "name" 属性の値を取得 */
                        val name = parser.getAttributeValue(null, "name")
                        if (name != null)
                            targetNames.add(name)
                    }
                    /* 次のイベントへ */
                    eventType = parser.next()
                }
            }
            catch (e: Exception) {
                /* 読み込みやパースのエラー処理 */
                e.printStackTrace()
            }
            finally {
                /* 4. ストリームを閉じる */
                inputStream?.close()
            }

            return targetNames
        }
    }
}
