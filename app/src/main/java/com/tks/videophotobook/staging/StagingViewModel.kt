package com.tks.videophotobook.staging

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MAX_LOG_COUNT = 500
class StagingViewModel : ViewModel() {
    private val _logListFlow = MutableStateFlow<List<String>>(emptyList())
    val logListFlow = _logListFlow.asStateFlow()

    fun addLogStr(item: String) {
        val mutableList = _logListFlow.value.toMutableList()
        mutableList.add(item)
        if(mutableList.size > MAX_LOG_COUNT) {
            val over = mutableList.size - MAX_LOG_COUNT
            mutableList.subList(0, over).clear()
        }
        _logListFlow.value = mutableList
    }

    fun removeLogStr(position: Int) {
        val list = _logListFlow.value.toMutableList()
        if (position in list.indices) {
            list.removeAt(position)
            _logListFlow.value = list
        }
    }

    fun clearLogs() {
        _logListFlow.value = emptyList()
    }
}