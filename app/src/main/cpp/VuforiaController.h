#ifndef VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H
#define VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H

/* 1.対応ヘッダ(同名ヘッダ) */
/* 2.C++ 標準ライブラリのヘッダ */
#include <string>
/* 3.他の外部ライブラリのヘッダ */
#include <VuforiaEngine/VuforiaEngine.h>
#include <jni.h>

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
    ERROR_HANDLER_DATA_COULD_NOT_BE_ADDED_TO_CONFIGURATION  = 0x1004,/** Failed to init Vuforia, error handler data could not be added to configuration. */
    ERROR_SETTING_CLIPPING_PLANES_FOR_PROJECTION            = 0x1005,/** Error setting clipping planes for projection. */
    ERROR_CREATING_DEVICE_POSE_OBSERVER                     = 0x1006,/** Error creating device pose observer. */
    ERROR_CREATING_IMAGE_TARGET_OBSERVER                    = 0x1007,/** Error creating image target observer. */
    ERROR_ENGINE_INSTANCE_HAS_NOT_BEEN_CREATED_YET          = 0x1008,/** engine instance has not been created yet. */
    ERROR_ENGINE_HAS_ALREADY_BEEN_STARTED                   = 0x1009,/** engine has already been started. */
    ERROR_FAILED_TO_START_VUFORIA                           = 0x100A,/** failed to start Vuforia. */
};

class VuforiaController {
public:
    /** Initialize Vuforia. When the initialization is completed successfully return 0. If initialization fails return the error code.*/
    static ErrorCode initAR(JavaVM *pvm, jobject pjobject, const std::string &licensekey);
    /** Start the AR session. Call this method when the app resumes from paused. */
    static ErrorCode startAR();
    /** Configure Vuforia rendering. */
    static bool configureRendering(jint width, jint height, int *pOrientation);
    /* Screen size and video size */
    static float _vVideoWidth;
    static float _vVideoHeight;
    static float _screenWidth;
    static float _screenHeight;

private:
    /** Vuforia Engine instance */
    static VuEngine* mEngine;
    /** Vuforia render controller object */
    static VuController* mRenderController;
    /** Vuforia platform controller object */
    static VuController* mPlatformController;
    /** The observer for device poses */
    static VuObserver* mDevicePoseObserver;
    /** The observer for either the Image or Model target depending on which target was specified */
    static std::vector<VuObserver*> mObjectObservers;
    /** The Vuforia camera video mode to use, either DEFAULT, SPEED or QUALITY. */
    static VuCameraVideoModePreset mCameraVideoMode;
    /** Used by initAR to prepare and invoke Vuforia initialization. */
    static ErrorCode initVuforiaInternal(JavaVM *pvm, jobject pjobject, const std::string &licensekey);
    /** Create the set of Vuforia Observers needed in the application */
    static ErrorCode createObservers();
};

#endif //VIDEOPHOTOBOOKV2_VUFORIACONTROLLER_H
