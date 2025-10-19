package com.tks.videophotobook.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SetDialogViewModel: ViewModel() {
    val mutableIsEnable = MutableStateFlow(true)
    val isEnable: StateFlow<Boolean> get() = mutableIsEnable

    lateinit var mutableMarkerVideoSet: MutableStateFlow<MarkerVideoSet>
    val markerVideoSet: StateFlow<MarkerVideoSet> get() = mutableMarkerVideoSet
}
