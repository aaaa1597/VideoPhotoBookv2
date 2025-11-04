package com.tks.videophotobook

import android.app.Activity
import com.tks.videophotobook.staging.StagingViewModel
import java.nio.ByteBuffer

external fun initAR(activity: Activity, licensekey: String): Int
external fun startAR() : Boolean
external fun initRendering()
external fun configureRendering(width: Int, height: Int, orientation: Int, rotation: Int) : Boolean

// ----------------------------------------
external fun setTextures(astronautWidth: Int, astronautHeight: Int, astronautBytes: ByteBuffer, pauseWidth: Int, pauseHeight: Int, pauseBytes: ByteBuffer)
external fun renderFrame(nowTargetName: String) : String
external fun deinitRendering()
external fun deinitAR()
external fun stopAR()
external fun cameraPerformAutoFocus()
external fun cameraRestoreAutoFocus()
external fun checkHit(x: Float, y: Float, screenW: Float, screenH: Float): String
external fun initVideoTexture(): Int
external fun nativeOnSurfaceChanged(width: Int, height: Int)
external fun nativeSetVideoSize(width: Int, height: Int)
external fun setFullScreenMode(isFullScreenMode: Boolean)
external fun passToNative(bridge: ViewModelBridge)
/* C++ callback用クラス(ViewModelを保持) */
class ViewModelBridge(val stagingViewModel: StagingViewModel) {
    fun garnishLogFromNative(message: String) {
        stagingViewModel.addLogStr(message)
    }
}