#ifndef VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H
#define VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H

/* 1.対応ヘッダ(同名ヘッダ) */
/* 2.C++ 標準ライブラリのヘッダ */
#include <string>
/* 3.他の外部ライブラリのヘッダ */
#include <VuforiaEngine/VuforiaEngine.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

enum class ErrorCode : int32_t {
    None = 0,
    VU_ENGINE_CREATION_ERROR_DEVICE_NOT_SUPPORTED   = 0x1,///< The device is not supported
    VU_ENGINE_CREATION_ERROR_PERMISSION_ERROR       = 0x2,///< One or more permissions required by Vuforia Engine are missing or not granted by user (e.g. the user may have denied camera access to the App)
    VU_ENGINE_CREATION_ERROR_LICENSE_ERROR          = 0x3,///< A valid license configuration is required
    VU_ENGINE_CREATION_ERROR_INITIALIZATION         = 0x4,///< An error occurred during initialization of the Vuforia Engine instance (e.g. an instance already exists)
    VU_ENGINE_CREATION_ERROR_DRIVER_CONFIG_LOAD_ERROR           = 0x100,///< An error occurred while loading the driver (library not found or could not be loaded due to missing entry points, incompatible ABI format, etc.)
    VU_ENGINE_CREATION_ERROR_DRIVER_CONFIG_FEATURE_NOT_SUPPORTED= 0x101,///< Vuforia Driver is not supported by the current license
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_MISSING_KEY             = 0x200,///< License key is missing
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_INVALID_KEY             = 0x201,///< Invalid license key passed to SDK
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_NO_NETWORK_PERMANENT    = 0x202,///< Unable to verify license key due to network (Permanent error)
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_NO_NETWORK_TRANSIENT    = 0x203,///< Unable to verify license key due to network (Transient error)
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_BAD_REQUEST             = 0x204,///< Malformed request sent to license server. Please ensure your app has valid name and version fields
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_KEY_CANCELED            = 0x205,///< Provided key is no longer valid
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_PRODUCT_TYPE_MISMATCH   = 0x206,///< Provided key is not valid for current product
    VU_ENGINE_CREATION_ERROR_LICENSE_CONFIG_UNKNOWN                 = 0x207,///< Unknown error
    VU_ENGINE_CREATION_ERROR_RENDER_CONFIG_UNSUPPORTED_BACKEND              = 0x300,///< Unsupported render backend
    VU_ENGINE_CREATION_ERROR_RENDER_CONFIG_FAILED_TO_SET_VIDEO_BG_VIEWPORT  = 0x301,///< Failed to set video background viewport.
    VU_ENGINE_CREATION_ERROR_PLATFORM_ANDROID_CONFIG_INITIALIZATION_ERROR   = 0x510,///< An error occurred during initialization of the platform
    VU_ENGINE_CREATION_ERROR_PLATFORM_ANDROID_CONFIG_INVALID_ACTIVITY       = 0x511,///< Invalid Android Activity jobject passed to the configuration
    VU_ENGINE_CREATION_ERROR_PLATFORM_ANDROID_CONFIG_INVALID_JAVA_VM        = 0x512,///< Invalid Java VM (JavaVM*) passed to the configuration This is currently never reported.
    VU_ENGINE_ERROR_INVALID_LICENSE                             = 0x600,///< License key validation has failed, Engine has stopped
    VU_ENGINE_ERROR_CAMERA_DEVICE_LOST                          = 0x601,///< The operating system has reported that the camera device
                                                                        ///< has become unavailable to Vuforia and therefore Engine has
                                                                        ///< stopped. This may be because of a device error or another App or
                                                                        ///< user action has caused the operating system to close Engine's
                                                                        ///< connection to the camera.
    VU_ENGINE_ERROR_PLATFORM_FUSION_PROVIDER_INFO_INVALIDATED   = 0x602,///< This error can only happen on Android when using ARCore.
                                                                        ///< <p> If ARCore Fusion Provider info was retrieved previously via
                                                                        ///< \ref vuPlatformControllerGetARCoreInfo, this will be invalidated
                                                                        ///< (including all platform pointers) after the callback has
                                                                        ///< returned and should not be accessed any more after that (as this
                                                                        ///< might lead to undefined behavior). The info must be retrieved again.
                                                                        ///< \note All the ARCore entities previously created by using these
                                                                        ///< pointers directly (such as anchors, planes) will also become
                                                                        ///< invalid and so must be created again.
    ERROR_INSTANCE_ALREADY_EXISTS                           = 0x1001,/** Failed to initialize Vuforia as a valid engine instance already exists */
    ERROR_COULD_NOT_APPLY_PLATFORM_SPECIFIC_CONFIGURATION   = 0x1002,/** Vuforia failed to initialize, could not apply platform-specific configuration. */
    ERROR_COULD_NOT_CONFIGURE_RENDERING                     = 0x1003,/** Failed to init Vuforia, could not configure rendering. */
    ERROR_HANDLER_DATA_COULD_NOT_BE_ADDED_TO_CONFIGURATION  = 0x1004,/** Failed to init Vuforia, error handler data could not be added to configuration */
    ERROR_SETTING_CLIPPING_PLANES_FOR_PROJECTION            = 0x1005,/** Error setting clipping planes for projection */
};

class VuforiaController {
public:
    static VuforiaController &getIns() {
        static VuforiaController instance;
        return instance;
    }
    static ErrorCode initAR(const std::string &licensekey);

private:
    /** Vuforia Engine instance */
    VuEngine* mEngine{ nullptr };
    /** Vuforia render controller object */
    VuController* mRenderController{ nullptr };
    /** Vuforia platform controller object */
    VuController* mPlatformController{ nullptr };
    static ErrorCode initVuforiaInternal(const std::string &licensekey);
};

#endif //VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H
