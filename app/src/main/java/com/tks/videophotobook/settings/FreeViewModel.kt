package com.tks.videophotobook.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FreeViewModel: ViewModel() {
    val mutableIsEnable = MutableStateFlow(true)
    val isEnable: StateFlow<Boolean> get() = mutableIsEnable
}
