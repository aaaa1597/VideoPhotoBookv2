package com.tks.videophotobook.settings

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SetDialogViewModel: ViewModel() {
    val mutableIsBlockedInput = MutableStateFlow(true)
    val isBlockedInput: StateFlow<Boolean> get() = mutableIsBlockedInput

    val mutableIsVisibilityMarker = MutableStateFlow(false)
    val isVisibilityMarker: StateFlow<Boolean> get() = mutableIsVisibilityMarker

    val mutableIsVisibilitySave = MutableStateFlow(false)
    val isVisibilitySave: StateFlow<Boolean> get() = mutableIsVisibilitySave

    lateinit var mutableMarkerVideoSet: MutableStateFlow<MarkerVideoSet>
    val markerVideoSet: StateFlow<MarkerVideoSet> get() = mutableMarkerVideoSet

    var mutable3Thumbnail: MutableStateFlow<Array<Bitmap?>> = MutableStateFlow(arrayOfNulls(3))
    val t3Thumbnail: StateFlow<Array<Bitmap?>> get() = mutable3Thumbnail
}
