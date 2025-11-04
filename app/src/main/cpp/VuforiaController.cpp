/* 1.対応ヘッダ(同名ヘッダ) */
#include "VuforiaController.h"
/* 2.C++ 標準ライブラリのヘッダ */
#include <format>
#include <cassert>
/* 3.他の外部ライブラリのヘッダ */
#include <android/log.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

namespace l {
    extern void garnishLog(const std::string &logstr);
}

inline void REQUIRE_SUCCESS(VuResult result, const char* file, int line) {
    if (result != VU_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Vu call failed at %s:%d (result=%d)", file, line, result);
#ifndef NDEBUG
        assert(false);
#endif
    }
}

float VuforiaController::_vVideoWidth = 0.0f;
float VuforiaController::_vVideoHeight = 0.0f;
float VuforiaController::_screenWidth = 0.0f;
float VuforiaController::_screenHeight = 0.0f;
VuEngine* VuforiaController::mEngine{ nullptr };
VuController* VuforiaController::mRenderController{ nullptr };
VuController* VuforiaController::mPlatformController{ nullptr };
VuObserver* VuforiaController::mDevicePoseObserver{nullptr};
std::vector<VuObserver*> VuforiaController::mObjectObservers = {};

ErrorCode VuforiaController::initAR(JavaVM *pvm, jobject pjobject, const std::string &licensekey) {
    l::garnishLog(std::format("VuforiaController::initAR() start{}", "."));
    ErrorCode errcode = initVuforiaInternal(pvm, pjobject, licensekey);
    l::garnishLog(std::format("VuforiaController::initAR() end{}", "."));

    l::garnishLog(std::format("VuforiaController::createObservers() start"));
    ErrorCode errcode2= createObservers();
    l::garnishLog(std::format("VuforiaController::createObservers() end(ret={})", (int)errcode2));

    return errcode;
}

ErrorCode VuforiaController::initVuforiaInternal(JavaVM *pvm, jobject pjobject, const std::string &licensekey) {
    l::garnishLog(std::format("VuforiaController::initVuforiaInternal() start{}", "."));

using E = ErrorCode;

    l::garnishLog(std::format("check VuforiaController::mEngine instance{}", "."));
    if(mEngine != nullptr)
        return E::ERROR_INSTANCE_ALREADY_EXISTS;

    l::garnishLog(std::format("create VuEngineConfigSet{}", "."));
    /* Create engine configuration data structure */
    VuEngineConfigSet* configSet = nullptr;
    REQUIRE_SUCCESS(vuEngineConfigSetCreate(&configSet), __FILE__, __LINE__);

    l::garnishLog(std::format("create VuEngineConfigSet{}", "."));
    VuLicenseConfig licenseConfig = vuLicenseConfigDefault();
    licenseConfig.key = licensekey.c_str();
    if (vuEngineConfigSetAddLicenseConfig(configSet, &licenseConfig) != VU_SUCCESS) {
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_INVALID_KEY;
    }

    l::garnishLog(std::format("Add platform(Android) configuration{}", "."));
    /* Set Android Activity owning the Vuforia Engine in platform-specific configuration */
    VuPlatformAndroidConfig vuPlatformConfig_Android = vuPlatformAndroidConfigDefault();
    vuPlatformConfig_Android.activity = pjobject;
    vuPlatformConfig_Android.javaVM = pvm;
    /* Add platform-specific configuration to engine configuration set */
    VuResult platformConfigResult = vuEngineConfigSetAddPlatformAndroidConfig(configSet, &vuPlatformConfig_Android);
    if (platformConfigResult != VU_SUCCESS) {
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::ERROR_COULD_NOT_APPLY_PLATFORM_SPECIFIC_CONFIGURATION;
    }

    l::garnishLog(std::format("Add platform(Android) configuration(GLES3){}", "."));
    /* Create default render configuration (may be overwritten by platform-specific settings)
       The default selects the platform preferred rendering backend */
    VuRenderConfig renderConfig = vuRenderConfigDefault();
    renderConfig.vbRenderBackend = VuRenderVBBackendType::VU_RENDER_VB_BACKEND_GLES3;
    /* Add rendering-specific engine configuration */
    if (vuEngineConfigSetAddRenderConfig(configSet, &renderConfig) != VU_SUCCESS) {
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::ERROR_COULD_NOT_CONFIGURE_RENDERING;
    }

    l::garnishLog(std::format("Add asynchronous engine error handler{}", "."));
    /* Add asynchronous engine error handler */
    VuErrorHandlerConfig errorHandlerConfig = vuErrorHandlerConfigDefault();
    errorHandlerConfig.errorHandler = nullptr;
    errorHandlerConfig.clientData = nullptr;
    if (vuEngineConfigSetAddErrorHandlerConfig(configSet, &errorHandlerConfig) != VU_SUCCESS){
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::ERROR_HANDLER_DATA_COULD_NOT_BE_ADDED_TO_CONFIGURATION;
    }

    l::garnishLog(std::format("Create Engine instance{}", "."));
    /* Create Engine instance */
    VuErrorCode errorCode;
    VuResult engineCreateResult = vuEngineCreate(&mEngine, configSet, &errorCode);

    l::garnishLog(
            std::format("Destroy configuration data as we have used it for engine creation{}", "."));
    /* Destroy configuration data as we have used it for engine creation */
    REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);

    if (engineCreateResult != VU_SUCCESS) {
        return static_cast<ErrorCode>(errorCode);
    }

    l::garnishLog(std::format("Bail out if engine creation has failed{}", "."));
    /* Bail out if engine creation has failed. */
    if(mEngine == nullptr){
        return E::VU_ENGINE_CREATION_ERROR_INITIALIZATION;
    }

    l::garnishLog(std::format("Retrieve Vuforia render and platform controllers from engine and cache them (remain valid as long as the engine instance is valid){}","."));
    /* Retrieve Vuforia render and platform controllers from engine and cache them (remain valid as long as the engine instance is valid) */
    REQUIRE_SUCCESS(vuEngineGetRenderController(mEngine, &mRenderController), __FILE__, __LINE__);
    assert(mRenderController);
    REQUIRE_SUCCESS(vuEngineGetPlatformController(mEngine, &mPlatformController), __FILE__, __LINE__);
    assert(mPlatformController);

    constexpr float NEAR_PLANE = 0.01f;
    constexpr float FAR_PLANE = 5.f;
    if (vuRenderControllerSetProjectionMatrixNearFar(mRenderController, NEAR_PLANE, FAR_PLANE) != VU_SUCCESS) {
        return E::ERROR_SETTING_CLIPPING_PLANES_FOR_PROJECTION;
    }

    l::garnishLog(std::format("Successfully initialized Vuforia{}", "."));
    l::garnishLog(std::format("VuforiaController::initVuforiaInternal() end{}", "."));
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Successfully initialized Vuforia.");
    return E::None;
}

ErrorCode VuforiaController::createObservers() {
    l::garnishLog(std::format("VuforiaController::createObservers() start"));

using E = ErrorCode;

    l::garnishLog(std::format("creating device pose observer start"));
    VuDevicePoseConfig devicePoseConfig = vuDevicePoseConfigDefault();
    VuDevicePoseCreationError devicePoseCreationError;
    if (vuEngineCreateDevicePoseObserver(mEngine, &mDevicePoseObserver, &devicePoseConfig, &devicePoseCreationError) != VU_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Error creating device pose observer: 0x%02x", devicePoseCreationError);
        return E::ERROR_CREATING_DEVICE_POSE_OBSERVER;
    }
    l::garnishLog(std::format("creating device pose observer end"));

    for(int lpct = 0; lpct < 10; lpct++) {
        l::garnishLog(std::format("creating ImageTargetObserver idx={}", lpct));
        VuImageTargetConfig imageTargetConfig = vuImageTargetConfigDefault();
        imageTargetConfig.databasePath = "VideoPhotoBook.xml";
        if(lpct == 0)      imageTargetConfig.targetName = "m000";
        else if(lpct == 1) imageTargetConfig.targetName = "m001";
        else if(lpct == 2) imageTargetConfig.targetName = "m002";
        else if(lpct == 3) imageTargetConfig.targetName = "m003";
        else if(lpct == 4) imageTargetConfig.targetName = "m004";
        else if(lpct == 5) imageTargetConfig.targetName = "m005";
        else if(lpct == 6) imageTargetConfig.targetName = "m006";
        else if(lpct == 7) imageTargetConfig.targetName = "m007";
        else if(lpct == 8) imageTargetConfig.targetName = "m008";
        else if(lpct == 9) imageTargetConfig.targetName = "m009";
        imageTargetConfig.activate = VU_FALSE;
        l::garnishLog(std::format("creating ImageTargetObserver targetName={}", imageTargetConfig.targetName));

        VuObserver* observer = nullptr;
        VuImageTargetCreationError imageTargetCreationError;
        if (vuEngineCreateImageTargetObserver(mEngine, &observer, &imageTargetConfig, &imageTargetCreationError) != VU_SUCCESS) {
            __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Error creating image target observer: 0x%02x", imageTargetCreationError);
            return E::ERROR_CREATING_IMAGE_TARGET_OBSERVER;
        }

        /* 同時認識数の設定*/
        REQUIRE_SUCCESS(vuEngineSetMaximumSimultaneousTrackedImages(mEngine, 5), __FILE__, __LINE__);

        /* Observerをアクティベート */
        vuObserverActivate(observer);

        mObjectObservers.push_back(observer);
    }

    l::garnishLog(std::format("Successfully createObservers."));
    l::garnishLog(std::format("VuforiaController::createObservers() end"));
    return E::None;
}

bool VuforiaController::startAR() {
    l::garnishLog(std::format("VuforiaController::startAR() start."));
    l::garnishLog(std::format("VuforiaController::startAR() end."));
    return false;
}

/** Configure Vuforia rendering. */
/** This method must be called after initAR and startAR are complete. */
/** This should be called from the Rendering thread. */
/** The orientation is specified as the platform-specific descriptor, hence the typeless parameter. */
bool VuforiaController::configureRendering(jint width, jint height, int *pOrientation) {
    return false;
}
