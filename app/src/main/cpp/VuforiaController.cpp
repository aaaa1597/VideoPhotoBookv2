/* 1.対応ヘッダ(同名ヘッダ) */
#include "VuforiaController.h"
/* 2.C++ 標準ライブラリのヘッダ */
#include <format>
#include <cassert>
/* 3.他の外部ライブラリのヘッダ */
#include <android/log.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

extern void _garnishLog(const std::string &logstr);

inline void REQUIRE_SUCCESS(VuResult result, const char* file, int line) {
    if (result != VU_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Vu call failed at %s:%d (result=%d)", file, line, result);
#ifndef NDEBUG
        assert(false);
#endif
    }
}

ErrorCode VuforiaController::initAR(JavaVM *pvm, jobject pjobject, const std::string &licensekey) {
    _garnishLog(std::format("VuforiaController::initAR() start{}", "."));
    ErrorCode errcode = initVuforiaInternal(pvm, pjobject, licensekey);
    _garnishLog(std::format("VuforiaController::initAR() end{}", "."));
    return errcode;
}

ErrorCode VuforiaController::initVuforiaInternal(JavaVM *pvm, jobject pjobject, const std::string &licensekey) {
    _garnishLog(std::format("VuforiaController::initVuforiaInternal() start{}", "."));

using E = ErrorCode;

    _garnishLog(std::format("check VuforiaController::mEngine instance{}", "."));
    if(getIns().mEngine != nullptr)
        return E::ERROR_INSTANCE_ALREADY_EXISTS;

    _garnishLog(std::format("create VuEngineConfigSet{}", "."));
    /* Create engine configuration data structure */
    VuEngineConfigSet* configSet = nullptr;
    REQUIRE_SUCCESS(vuEngineConfigSetCreate(&configSet), __FILE__, __LINE__);

    _garnishLog(std::format("create VuEngineConfigSet{}", "."));
    VuLicenseConfig licenseConfig = vuLicenseConfigDefault();
    licenseConfig.key = licensekey.c_str();
    if (vuEngineConfigSetAddLicenseConfig(configSet, &licenseConfig) != VU_SUCCESS) {
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_INVALID_KEY;
    }

    _garnishLog(std::format("Add platform(Android) configuration{}", "."));
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

    _garnishLog(std::format("Add platform(Android) configuration(GLES3){}", "."));
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

    _garnishLog(std::format("Add asynchronous engine error handler{}", "."));
    /* Add asynchronous engine error handler */
    VuErrorHandlerConfig errorHandlerConfig = vuErrorHandlerConfigDefault();
    errorHandlerConfig.errorHandler = nullptr;
    errorHandlerConfig.clientData = &(getIns());
    if (vuEngineConfigSetAddErrorHandlerConfig(configSet, &errorHandlerConfig) != VU_SUCCESS){
        /* Clean up before exiting */
        REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);
        return E::ERROR_HANDLER_DATA_COULD_NOT_BE_ADDED_TO_CONFIGURATION;
    }

    _garnishLog(std::format("Create Engine instance{}", "."));
    /* Create Engine instance */
    VuErrorCode errorCode;
    VuResult engineCreateResult = vuEngineCreate(&(getIns().mEngine), configSet, &errorCode);

    _garnishLog(std::format("Destroy configuration data as we have used it for engine creation{}", "."));
    /* Destroy configuration data as we have used it for engine creation */
    REQUIRE_SUCCESS(vuEngineConfigSetDestroy(configSet), __FILE__, __LINE__);

    if (engineCreateResult != VU_SUCCESS) {
        return static_cast<ErrorCode>(errorCode);
    }

    _garnishLog(std::format("Bail out if engine creation has failed{}", "."));
    /* Bail out if engine creation has failed. */
    if(getIns().mEngine == nullptr){
        return E::VU_ENGINE_CREATION_ERROR_INITIALIZATION;
    }

    _garnishLog(std::format("Retrieve Vuforia render and platform controllers from engine and cache them (remain valid as long as the engine instance is valid){}", "."));
    /* Retrieve Vuforia render and platform controllers from engine and cache them (remain valid as long as the engine instance is valid) */
    REQUIRE_SUCCESS(vuEngineGetRenderController(getIns().mEngine, &getIns().mRenderController), __FILE__, __LINE__);
    assert(getIns().mRenderController);
    REQUIRE_SUCCESS(vuEngineGetPlatformController(getIns().mEngine, &getIns().mPlatformController), __FILE__, __LINE__);
    assert(getIns().mPlatformController);

    constexpr float NEAR_PLANE = 0.01f;
    constexpr float FAR_PLANE = 5.f;
    if (vuRenderControllerSetProjectionMatrixNearFar(getIns().mRenderController, NEAR_PLANE, FAR_PLANE) != VU_SUCCESS) {
        return E::ERROR_SETTING_CLIPPING_PLANES_FOR_PROJECTION;
    }

    _garnishLog(std::format("Successfully initialized Vuforia{}", "."));
    _garnishLog(std::format("VuforiaController::initVuforiaInternal() end{}", "."));
    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Successfully initialized Vuforia.");
    return E::None;
}
