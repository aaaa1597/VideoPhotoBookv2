package com.tks.videophotobook.settings

import android.content.Context
import android.net.Uri
import android.util.Xml
import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import kotlinx.serialization.json.Json
import com.tks.videophotobook.R
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class MarkerVideoSet(
    /* ARマーカー系定義 */
    var targetName: String = "",
    @DrawableRes var targetImageTemplateResId: Int,
    @Serializable(with = UriSerializer::class) var targetImageUri: Uri,
    /* Video系定義 */
    @Serializable(with = UriSerializer::class) var videoUri: Uri,
    var comment: String
) {
    companion object {
        fun loadFromJsonFile(context: Context, file: File): List<MarkerVideoSet> {
            /* ファイルが存在しない場合、空リストを返す */
            if (!file.exists())
                return emptyList()

            return try {
                /* 2. ファイルの内容を文字列として全て読み込む */
                val jsonString = file.readText()

                /* 3. Kotlinx.serializationのJsonパーサーを使用してデシリアライズする */
                /*    List<MarkerVideoSet> 型へ変換する */
                Json.decodeFromString<List<MarkerVideoSet>>(jsonString)
            }
            catch (e: Exception) {
                // ファイル読み込みやJSONパースに失敗した場合のエラー処理
                e.printStackTrace()
                // エラーが発生した場合は空のリストを返す
                emptyList()
            }
        }

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
                    if (eventType == XmlPullParser.START_TAG) {
                        /* 開始タグの名前が "ImageTarget" の場合 */
                        if (parser.name == "ImageTarget") {
                            /* 属性を走査して "name" 属性の値を取得 */
                            val name = parser.getAttributeValue(null, "name")
                            if (name != null) {
                                targetNames.add(name)
                            }
                        }
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

object UriSerializer : KSerializer<Uri> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uri", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }
}