package com.tks.videophotobook.settings

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SetDialogViewModel: ViewModel() {
    val mutableIsEnable = MutableStateFlow(true)
    val isEnable: StateFlow<Boolean> get() = mutableIsEnable

    val mutableIsVisibilityMarker = MutableStateFlow(false)
    val isVisibilityMarker: StateFlow<Boolean> get() = mutableIsVisibilityMarker

    val mutableIsVisibilitySave = MutableStateFlow(false)
    val isVisibilitySave: StateFlow<Boolean> get() = mutableIsVisibilitySave

    lateinit var mutableMarkerVideoSet: MutableStateFlow<MarkerVideoSet>
    val markerVideoSet: StateFlow<MarkerVideoSet> get() = mutableMarkerVideoSet

    var mutable3Thumbnail: MutableStateFlow<Array<Bitmap?>> = MutableStateFlow(arrayOfNulls<Bitmap>(3))
    val t3Thumbnail: StateFlow<Array<Bitmap?>> get() = mutable3Thumbnail
}
