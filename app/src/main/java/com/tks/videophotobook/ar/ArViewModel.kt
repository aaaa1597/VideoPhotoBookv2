package com.tks.videophotobook.ar

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class ArViewModel : ViewModel() {
    var detectedTarget: MutableStateFlow<String> = MutableStateFlow("")

}
