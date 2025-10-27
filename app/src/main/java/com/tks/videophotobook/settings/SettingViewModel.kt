package com.tks.videophotobook.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import com.tks.videophotobook.R
import com.tks.videophotobook.settings.MarkerVideoSet.Companion.loadImageTargetNamesFromAssets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

const val MARKER_VIDEO_MAP_JSON = "marker_video_map.json"
class SettingViewModel(application: Application) : AndroidViewModel(application) {
    val mutableMarkerVideoSetList = MutableStateFlow<List<MarkerVideoSet>>(emptyList())
    val markerVideoSetList: StateFlow<List<MarkerVideoSet>> = mutableMarkerVideoSetList
    fun initMarkerVideoSetList() {
        val file = File(getApplication<Application>().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        var list = MarkerVideoSet.loadFromJsonFile(file)
        if(list.isEmpty()) {
            /* assets配下のVideoPhotoBook.xmlのImageTargetタグのname属性一覧を取得 */
            val targetNames = loadImageTargetNamesFromAssets(application)
            val plesechoose = ""
            list = listOf(
                MarkerVideoSet(targetNames[0], R.drawable.m000_star4, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[1], R.drawable.m001_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[2], R.drawable.m002_star4, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[3], R.drawable.m003_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[4], R.drawable.m004_star4, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[5], R.drawable.m005_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[6], R.drawable.m006_star4, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[7], R.drawable.m007_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[8], R.drawable.m008_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
                MarkerVideoSet(targetNames[9], R.drawable.m009_star5, Uri.EMPTY, Uri.EMPTY, plesechoose),
            )
        }
        mutableMarkerVideoSetList.value = list
    }

    fun getMakerUriArrayList(): ArrayList<Uri> {
        return mutableMarkerVideoSetList.value.filter { it.targetImageUri != Uri.EMPTY }.map{ it.targetImageUri }.toCollection(ArrayList())
    }

    fun saveMarkerVideoSetListToCacheJsonFile() {
        val file = File(getApplication<Application>().externalCacheDir, MARKER_VIDEO_MAP_JSON)
        val jsonList = mutableMarkerVideoSetList.value.joinToString(prefix="[", postfix="]") { it.toJson() }
        file.writeText(jsonList)
        Log.d("aaaaa", "Saved jsonList= $jsonList")
    }

    fun getVideoUri(target: String): Uri? {
        return mutableMarkerVideoSetList.value.find { it.targetName == target }?.videoUri
    }

    val isVisibleDoubleTapGuideView: StateFlow<Boolean> = mutableMarkerVideoSetList
        .map { list -> list.all { it.videoUri == Uri.EMPTY } }
        .stateIn(
            scope = viewModelScope,             /* ScopはviewModelScopeで */
            started = SharingStarted.Eagerly,   /* すぐに開始 */
            initialValue = mutableMarkerVideoSetList.value.all { it.videoUri == Uri.EMPTY } /* 初期値 */
        )

}