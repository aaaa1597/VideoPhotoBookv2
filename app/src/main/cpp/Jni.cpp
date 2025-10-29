#include <jni.h>
#include <string>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_initRendering(JNIEnv *env, jclass clazz) {
    // TODO: implement initRendering()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_setTextures(JNIEnv *env, jclass clazz, jint astronaut_width,
                                              jint astronaut_height, jobject astronaut_bytes,
                                              jint pause_width, jint pause_height,
                                              jobject pause_bytes) {
    // TODO: implement setTextures()
}


JNIEXPORT jboolean JNICALL
Java_com_tks_videophotobook_JniKt_configureRendering(JNIEnv *env, jclass clazz, jint width,
                                                     jint height, jint orientation, jint rotation) {
    // TODO: implement configureRendering()
}

JNIEXPORT jstring JNICALL
Java_com_tks_videophotobook_JniKt_renderFrame(JNIEnv *env, jclass clazz, jstring now_target_name) {
    // TODO: implement renderFrame()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_deinitRendering(JNIEnv *env, jclass clazz) {
    // TODO: implement deinitRendering()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_initAR(JNIEnv *env, jclass clazz, jobject activity) {
    // TODO: implement initAR()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_deinitAR(JNIEnv *env, jclass clazz) {
    // TODO: implement deinitAR()
}

JNIEXPORT jboolean JNICALL
Java_com_tks_videophotobook_JniKt_startAR(JNIEnv *env, jclass clazz) {
    // TODO: implement startAR()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_stopAR(JNIEnv *env, jclass clazz) {
    // TODO: implement stopAR()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_cameraPerformAutoFocus(JNIEnv *env, jclass clazz) {
    // TODO: implement cameraPerformAutoFocus()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_cameraRestoreAutoFocus(JNIEnv *env, jclass clazz) {
    // TODO: implement cameraRestoreAutoFocus()
}

JNIEXPORT jstring JNICALL
Java_com_tks_videophotobook_JniKt_checkHit(JNIEnv *env, jclass clazz, jfloat x, jfloat y,
                                           jfloat screen_w, jfloat screen_h) {
    // TODO: implement checkHit()
}

JNIEXPORT jint JNICALL
Java_com_tks_videophotobook_JniKt_initVideoTexture(JNIEnv *env, jclass clazz) {
    // TODO: implement initVideoTexture()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_nativeOnSurfaceChanged(JNIEnv *env, jclass clazz, jint width,
                                                         jint height) {
    // TODO: implement nativeOnSurfaceChanged()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_nativeSetVideoSize(JNIEnv *env, jclass clazz, jint width,
                                                     jint height) {
    // TODO: implement nativeSetVideoSize()
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_setFullScreenMode(JNIEnv *env, jclass clazz,
                                                    jboolean is_full_screen_mode) {
    // TODO: implement setFullScreenMode()
}

#ifdef __cplusplus
}
#endif
