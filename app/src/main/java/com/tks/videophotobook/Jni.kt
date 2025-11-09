package com.tks.videophotobook

import android.app.Activity
import com.tks.videophotobook.ar.StagingViewModel
import java.nio.ByteBuffer

external fun passToNative(bridge: ViewModelBridge)
/** Initialize Vuforia. When the initialization is completed successfully the callback method initDoneCallback will be invoked. */
/** If initialization fails the error callback errorMessageCallback will be invoked. */
/** On Android the appData pointer should be a pointer to the Activity object. */
external fun initAR(activity: Activity, licensekey: String): Int
/** Start the AR session */
/** Call this method when the app resumes from paused. */
external fun startAR() : Int
external fun initRendering()
/** Configure Vuforia rendering. */
/** This method must be called after initAR and startAR are complete. */
/** This should be called from the Rendering thread. */
/** The orientation is specified as the platform-specific descriptor, hence the typeless parameter. */
external fun configureRendering(width: Int, height: Int, orientation: Int, rotation: Int) : Boolean
external fun setTextures(pauseWidth: Int, pauseHeight: Int, pauseBytes: ByteBuffer)
external fun setVideoTexture(): Int
external fun nativeSetVideoSize(width: Int, height: Int)
external fun renderFrame(nowTargetName: String) : String
external fun deinitRendering()
/** Clean up and deinitialize Vuforia. */
external fun deinitAR()
/** Stop the AR session */
/** Call this method when the app is paused. */
external fun stopAR()
/** Request that the camera refocuses in the current position */
external fun cameraPerformAutoFocus()
/** Restore the camera to continuous autofocus mode */
external fun cameraRestoreAutoFocus()
external fun checkHit(x: Float, y: Float, screenW: Float, screenH: Float): String
external fun setFullScreenMode(isFullScreenMode: Boolean)
/* C++ callback用クラス(ViewModelを保持) */
class ViewModelBridge(val stagingViewModel: StagingViewModel) {
    fun garnishLogFromNative(message: String) {
        stagingViewModel.addLogStr(message)
    }
}