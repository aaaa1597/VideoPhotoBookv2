package com.tks.videophotobook.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.tks.videophotobook.R
import com.tks.videophotobook.settings.MarkerVideoSet.Companion.loadImageTargetNamesFromAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

val MARKER_VIDEO_MAP_JSON = "marker_video_map.json"
class SettingViewModel(application: Application) : AndroidViewModel(application) {
    private val _markerVideoSetList = MutableStateFlow<List<MarkerVideoSet>>(emptyList())
    val markerVideoSetList: StateFlow<List<MarkerVideoSet>> = _markerVideoSetList

    fun initMarkerVideoSetList() {
        val file = File(getApplication<Application>().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        var list = MarkerVideoSet.loadFromJsonFile(application, file)
        if(list.isEmpty()) {
            /* assets配下のVideoPhotoBook.xmlのImageTargetタグのname属性一覧を取得 */
            val targetNames = loadImageTargetNamesFromAssets(application)
            val plesechoose = application.getString(R.string.please_choose)
            list = listOf(
                MarkerVideoSet(targetNames[0], R.drawable.m000_star4,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[1], R.drawable.m001_star5,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[2], R.drawable.m002_star4,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[3], R.drawable.m003_star5,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[4], R.drawable.m004_star4,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[5], R.drawable.m005_star5,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[6], R.drawable.m006_star4,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[7], R.drawable.m007_star5,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[8], R.drawable.m008_star5,Uri.parse(""), Uri.parse(""), plesechoose),
                MarkerVideoSet(targetNames[9], R.drawable.m009_star5,Uri.parse(""), Uri.parse(""), plesechoose),
            )
        }
        _markerVideoSetList.value = list
    }
}