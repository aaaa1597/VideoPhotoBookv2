#ifndef VIDEOPHOTOBOOKV2_GLESRENDERER_H
#define VIDEOPHOTOBOOKV2_GLESRENDERER_H

/* 1.対応ヘッダ(同名ヘッダ) */
/* 2.C++ 標準ライブラリのヘッダ */
/* 3.他の外部ライブラリのヘッダ */
#include <GLES3/gl31.h>
#include <GLES2/gl2ext.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

class GLESRenderer {
public:
    static GLESRenderer &getIns() {
        static GLESRenderer instance;
        return instance;
    }
public:
    ~GLESRenderer() = default;
    /** Initialize the renderer ready for use */
    bool init();
    /** pause.pngのテクスチャデータ設定 */
    void setPauseTexture(int width, int height, unsigned char* bytes);
    void createTexture(int width, int height, const unsigned char *bytes, GLuint& texId);

public:
    /* Screen size and video size */
    float _vVideoWidth = 0.0f;
    float _vVideoHeight = 0.0f;
    float _screenWidth  = 0.0f;
    float _screenHeight = 0.0f;

    /* Fullscreen mode flag */
    bool  _fullscreenFlg = false;

private: // data members
    /* For camera background rendering */
    GLuint _CameraShaderProgramID       = 0;
    GLint _CameraVertexPositionHandle   = 0;
    GLint _CameraTextureCoordHandle     = 0;
    GLint _CameraMvpMatrixHandle        = 0;
    GLint _CameraTexSampler2DHandle     = 0;
    /* For pause.png rendering */
    GLuint _pTextureId          = 0;
    GLuint _pProgram            = 0;
    GLint _paPositionLoc        = -1;
    GLint _paTexCoordLoc        = -1;
    GLint _puProjectionMatrixLoc= -1;
    GLint _puSampler2D          = -1;
    /* For video playback rendering */
public:
    GLuint _vTextureId = 0;
private: // data members
    GLuint _vProgram = 0;
    GLint _vaPosition = -1;
    GLint _vaTexCoordLoc = -1;
    GLint _vuProjectionMatrixLoc = -1;
    GLint _vuSamplerOES = -1;
};

#endif //VIDEOPHOTOBOOKV2_GLESRENDERER_H
