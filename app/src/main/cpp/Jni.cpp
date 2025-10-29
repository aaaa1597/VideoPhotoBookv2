#include <jni.h>
#include <string>
#include <android/log.h>
#include <assert.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "JNI_OnLoad");

    if (vm == nullptr) {
        __android_log_print(ANDROID_LOG_FATAL, "JNI", "JavaVM pointer is null in JNI_OnLoad");
        assert(false); // もしくは return -1;
    }

    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Failed to get JNI environment from JavaVM");
        return -1;
    }

    // Cache Java VM
//    javaVM = vm;
//    gWrapperData.vm = vm;

    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Retrieved and stored JavaVM");
    return JNI_VERSION_1_6;
}

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
