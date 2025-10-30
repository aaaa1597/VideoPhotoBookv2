#include <jni.h>
#include <string>
#include <android/log.h>
#include <cassert>

jobject g_bridge = nullptr;
JavaVM *g_vm = nullptr;
void garnishLog(const std::string &logstr);

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_initAR(JNIEnv *env, jclass clazz, jobject activity) {
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() start");
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() start");
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() start");
    // TODO: implement initAR()
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() end");
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() end");
    garnishLog("Java_com_tks_videophotobook_JniKt_initAR() end");
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "JNI_OnLoad");

    if (vm == nullptr) {
        __android_log_print(ANDROID_LOG_FATAL, "JNI", "JavaVM pointer is null in JNI_OnLoad");
        assert(false);
    }

    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Failed to get JNI environment from JavaVM");
        return -1;
    }

    g_vm = vm;

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

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_passToNative(JNIEnv *env, jclass clazz, jobject bridge) {
    g_bridge = env->NewGlobalRef(bridge);
}

#ifdef __cplusplus
}
#endif

void garnishLog(const std::string &logstr) {
    /* detach要求フラグ */
    bool needDetach = false;
    /* JNIEnvのインスタンス取得(C++側でのスレッド跨ぎを考慮して毎回取得する) */
    JNIEnv* env = nullptr;
    int getEnvStat = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    if (getEnvStat == JNI_EDETACHED) {
        /* Kotlin側スレッドにアタッチ */
        if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Failed to attach current thread");
            return;
        }
        needDetach = true;
    }
    else if (getEnvStat == JNI_OK) {
        /* すでにアタッチ済 */
        needDetach = false;
    }
    else {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Failed to get the environment");
        return;
    }
    /* ViewModelBridgeクラスのインスタンス取得 */
    jclass bridgeClass = env->GetObjectClass(g_bridge);
    /* ViewModelBridgeクラスの関数を取得 */
    jmethodID methodId = env->GetMethodID(bridgeClass, "garnishLogFromNative", "(Ljava/lang/String;)V");
    /* 引数のstringをkotlinのString型に変換 */
    jstring jLogStr = env->NewStringUTF(logstr.c_str());
    /* やっとkotlin関数(ViewModelBridge::garnishLogFromNative)呼び出し */
    env->CallVoidMethod(g_bridge, methodId, jLogStr);
    /* jLogStrを解放 */
    env->DeleteLocalRef(jLogStr);
    /* スレッドデタッチ(必要な時だけ) */
    if (needDetach)
        g_vm->DetachCurrentThread();

    return;
}