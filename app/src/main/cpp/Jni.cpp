/* 1.対応ヘッダ(同名ヘッダ) */
/* 2.C++ 標準ライブラリのヘッダ */
#include <string>
#include <cassert>
#include <vector>
#include <format>
/* 3.他の外部ライブラリのヘッダ */
#include <android/log.h>
#include <jni.h>
/* 4.プロジェクト内(ローカル)ヘッダ */
#include "GLESRenderer.h"
#include "VuforiaController.h"

jobject g_pbridge = nullptr;
JavaVM *g_pvm = nullptr;
namespace l {
    void garnishLog(const std::string &logstr);
}

/* 2Dベクトルの外積(z成分)の算出関数 */
float cross2D(const glm::vec2& v1, const glm::vec2& v2) {
    return v1.x * v2.y - v1.y * v2.x;
}

/* タッチ座標と四角形頂点座標からコリジョン判定 */
/* 0 → 1 → 2 → 3 → 0 の順で判定 */
bool checkPolygonHit(const glm::vec2& targetPoint, const std::array<glm::vec2, 4>& ndcQuadPoints) {
    float z0 = cross2D(ndcQuadPoints[1] - ndcQuadPoints[0], targetPoint - ndcQuadPoints[0]);
    float z1 = cross2D(ndcQuadPoints[2] - ndcQuadPoints[1], targetPoint - ndcQuadPoints[1]);
    float z2 = cross2D(ndcQuadPoints[3] - ndcQuadPoints[2], targetPoint - ndcQuadPoints[2]);
    float z3 = cross2D(ndcQuadPoints[0] - ndcQuadPoints[3], targetPoint - ndcQuadPoints[3]);
    return (z0 >= 0 && z1 >= 0 && z2 >= 0 && z3 >= 0) || /* 全部が0以上　もしくは */
           (z0 <= 0 && z1 <= 0 && z2 <= 0 && z3 <= 0);   /* 全部が0以下 */
}

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "JNI_OnLoad");
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    0-0-1. JNI_OnLoad");

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

    g_pvm = vm;

    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Retrieved and stored JavaVM");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_passToNative(JNIEnv *env, jclass clazz, jobject bridge) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    0-2-1. Java_com_tks_videophotobook_JniKt_passToNative()");
    g_pbridge = env->NewGlobalRef(bridge);
}

JNIEXPORT jint JNICALL
Java_com_tks_videophotobook_JniKt_initAR(JNIEnv *env, jclass clazz, jobject activity, jstring jlicensekey) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    0-2-2. Java_com_tks_videophotobook_JniKt_initAR()");
    const char *license_key = env->GetStringUTFChars(jlicensekey, nullptr);
    std::string licenseKey(license_key);
    env->ReleaseStringUTFChars(jlicensekey, license_key);

    l::garnishLog("Java_com_tks_videophotobook_JniKt_initAR() start");
    ErrorCode ret = VuforiaController::initAR(g_pvm, activity, licenseKey);
    l::garnishLog(std::format("Java_com_tks_videophotobook_JniKt_initAR() end(err={})", (int)ret));
    return static_cast<jint>(ret);
}

JNIEXPORT jint JNICALL
Java_com_tks_videophotobook_JniKt_startAR(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-5-1. Java_com_tks_videophotobook_JniKt_startAR()");
    l::garnishLog("Java_com_tks_videophotobook_JniKt_startAR() start");
    ErrorCode ret = VuforiaController::startAR();
    if (ret != ErrorCode::None)
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Error startAR()!");

    l::garnishLog("Java_com_tks_videophotobook_JniKt_startAR() end(JNI_TRUE)");
    return (jint)ret;
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_initRendering(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-6-1. Java_com_tks_videophotobook_JniKt_initRendering()");
    bool ret = GLESRenderer::getIns().init();
    if(!ret)
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Error initialising renderer");
}

JNIEXPORT jboolean JNICALL
Java_com_tks_videophotobook_JniKt_configureRendering(JNIEnv *env, jclass clazz, jint width, jint height, jint orientation, jint rotation) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-7-1. Java_com_tks_videophotobook_JniKt_configureRendering()");
    glViewport(0, 0, width, height);
using VuC = VuforiaController;
    GLESRenderer::getIns()._screenWidth = static_cast<float>(width);
    GLESRenderer::getIns()._screenHeight= static_cast<float>(height);
    std::vector<int> androidOrientation{ orientation, rotation };
    return VuC::configureRendering(width, height, androidOrientation.data()) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_setTextures(JNIEnv *env, jclass clazz,
                               jint pause_width, jint pause_height, jobject pause_bytes) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-7-2. Java_com_tks_videophotobook_JniKt_setTextures()");
    auto pauseBytes = static_cast<unsigned char*>(env->GetDirectBufferAddress(pause_bytes));
    GLESRenderer::getIns().setPauseTexture(pause_width, pause_height, pauseBytes);
}

JNIEXPORT jint JNICALL
Java_com_tks_videophotobook_JniKt_setVideoTexture(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-7-3. Java_com_tks_videophotobook_JniKt_setVideoTexture()");
    return GLESRenderer::getIns().setVideoTexture();
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_nativeSetVideoSize(JNIEnv *env, jclass clazz, jint width, jint height) {
    GLESRenderer::getIns()._vVideoWidth = static_cast<float>(width);
    GLESRenderer::getIns()._vVideoHeight= static_cast<float>(height);
}

JNIEXPORT jstring JNICALL
Java_com_tks_videophotobook_JniKt_renderFrame(JNIEnv *env, jclass clazz, jstring now_playing_target) {
    /* 戻り値定義 */
    std::string retDetectedTarget = "";
    /* 引数jstring を std::string に変換 */
    const char* nativeStr = env->GetStringUTFChars(now_playing_target, nullptr);
    std::string nowPlayingTarget(nativeStr);
    env->ReleaseStringUTFChars(now_playing_target, nativeStr);

    /* Clear colour and depth buffers */
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    int vbTextureUnit = 0;
    VuRenderVideoBackgroundData renderVideoBackgroundData;
    renderVideoBackgroundData.renderData = nullptr;
    renderVideoBackgroundData.textureData = nullptr;
    renderVideoBackgroundData.textureUnitData = &vbTextureUnit;
    double viewport[6];
using VuC = VuforiaController;
    if (VuC::prepareToRender(viewport, &renderVideoBackgroundData)) {
        /* Set viewport for current view */
        glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        auto renderState = VuC::getRenderState();
        GLESRenderer::getIns().renderBackgroundFromCameraImage(renderState.vbProjectionMatrix, renderState.vbMesh->pos, renderState.vbMesh->tex,
                                                               renderState.vbMesh->numFaces, renderState.vbMesh->faceIndices, vbTextureUnit);

        auto [imageTargetList, CNT] = VuC::createImageTargetList();
        for (int idx = 0; idx < CNT; idx++) {
            VuObservation* observation = nullptr;
            if (vuObservationListGetElement(imageTargetList.get(), idx, &observation) != VU_SUCCESS)
                continue;

            assert(observation);
            assert(vuObservationIsType(observation, VU_OBSERVATION_IMAGE_TARGET_TYPE) == VU_TRUE);
            assert(vuObservationHasPoseInfo(observation) == VU_TRUE);

            VuMatrix44F trackableProjection;
            VuMatrix44F trackableModelView;
            VuMatrix44F trackableModelViewScaled;

            VuImageTargetObservationTargetInfo imageTargetInfo;
            VuResult vuret = vuImageTargetObservationGetTargetInfo(observation, &imageTargetInfo);
            assert(vuret == VU_SUCCESS);
            VuVector2F markerSize{.data{imageTargetInfo.size.data[0], imageTargetInfo.size.data[1]}};

            if (VuC::getImageTargetResult(observation, markerSize, trackableProjection, trackableModelView, trackableModelViewScaled))
            {
                if(nowPlayingTarget.empty()) {
                    nowPlayingTarget = imageTargetInfo.name;
                    retDetectedTarget = imageTargetInfo.name;
                    GLESRenderer::getIns().renderVideoPlayback(trackableProjection, trackableModelViewScaled, markerSize, imageTargetInfo.name);
                }
                else if(nowPlayingTarget != imageTargetInfo.name)
                    GLESRenderer::getIns().renderPause(trackableProjection, trackableModelViewScaled, markerSize, imageTargetInfo.name);
                else if(nowPlayingTarget == imageTargetInfo.name) {
                    retDetectedTarget = imageTargetInfo.name;
                    GLESRenderer::getIns().renderVideoPlayback(trackableProjection, trackableModelViewScaled, markerSize, imageTargetInfo.name);
                }
            }
        }
        imageTargetList.reset();
    }

    VuC::finishRender();

    return env->NewStringUTF(retDetectedTarget.c_str());
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_deinitRendering(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-9-1. Java_com_tks_videophotobook_JniKt_deinitRendering()");
    GLESRenderer::getIns().deinit();
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_stopAR(JNIEnv *env, jclass clazz) {
    __android_log_print(ANDROID_LOG_VERBOSE, "aaaaa", "    1-10-1. Java_com_tks_videophotobook_JniKt_stopAR()");
using VuC = VuforiaController;
    VuC::stopAR();
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_deinitAR(JNIEnv *env, jclass clazz) {
using VuC = VuforiaController;
    VuC::deinitAR();
    env->DeleteGlobalRef(g_pbridge);
    g_pbridge = nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_tks_videophotobook_JniKt_checkHit(JNIEnv *env, jclass clazz, jfloat x, jfloat y, jfloat screen_w, jfloat screen_h) {
    for (const auto& [targetName, timeAndQuad] : GLESRenderer::getIns()._ndcQuadPoints) {
        const auto& [lastUpdate, ndcQuadPoints] = timeAndQuad;
        /* 最終更新時刻が1秒以上ならそのデータは無効 */
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now() - lastUpdate);
        if(duration.count() > 1000) continue;

        /* タッチ座標を スクリーン座標 → NDC(正規化デバイス座標-1～1)に変換 */
        float ndcX = (2.0f * x / screen_w) - 1.0f;
        float ndcY = 1.0f - (2.0f * y / screen_h); /* Y軸は反転してOpenGL系に合わせる */

        /* タッチ座標と板ポリ座標でコリジョン判定 */
        glm::vec2 touchPoint = glm::vec2(ndcX, ndcY);
        bool ret = checkPolygonHit(touchPoint, ndcQuadPoints);
        /* 見つかったらreturn */
        if(ret) return env->NewStringUTF(targetName.c_str());;
    }
    return env->NewStringUTF(std::string("").c_str());
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_setFullScreenMode(JNIEnv *env, jclass clazz, jboolean is_full_screen_mode) {
    GLESRenderer::getIns()._fullscreenFlg = (is_full_screen_mode == JNI_TRUE);
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "_fullscreenFlg=%d", GLESRenderer::getIns()._fullscreenFlg);
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_cameraPerformAutoFocus(JNIEnv *env, jclass clazz) {
using VuC = VuforiaController;
    VuC::cameraPerformAutoFocus();
}

JNIEXPORT void JNICALL
Java_com_tks_videophotobook_JniKt_cameraRestoreAutoFocus(JNIEnv *env, jclass clazz) {
using VuC = VuforiaController;
    VuC::cameraRestoreAutoFocus();
}

#ifdef __cplusplus
}
#endif

namespace l {
    void garnishLog(const std::string &logstr) {
        /* detach要求フラグ */
        bool needDetach = false;
        /* JNIEnvのインスタンス取得(C++側でのスレッド跨ぎを考慮して毎回取得する) */
        JNIEnv* env = nullptr;
        int getEnvStat = g_pvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
        if (getEnvStat == JNI_EDETACHED) {
            /* Kotlin側スレッドにアタッチ */
            if (g_pvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
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
        jclass bridgeClass = env->GetObjectClass(g_pbridge);
        /* ViewModelBridgeクラスの関数を取得 */
        jmethodID methodId = env->GetMethodID(bridgeClass, "garnishLogFromNative", "(Ljava/lang/String;)V");
        /* 引数のstringをkotlinのString型に変換 */
        jstring jLogStr = env->NewStringUTF(logstr.c_str());
        /* やっとkotlin関数(ViewModelBridge::garnishLogFromNative)呼び出し */
        env->CallVoidMethod(g_pbridge, methodId, jLogStr);
        /* jLogStrを解放 */
        env->DeleteLocalRef(jLogStr);
        env->DeleteLocalRef(bridgeClass);
        /* スレッドデタッチ(必要な時だけ) */
        if (needDetach)
            g_pvm->DetachCurrentThread();

        return;
    }
}
