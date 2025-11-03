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

private: // data members
    /* For camera background rendering */
    GLuint mCameraShaderProgramID       = 0;
    GLint mCameraVertexPositionHandle   = 0;
    GLint mCameraTextureCoordHandle     = 0;
    GLint mCameraMvpMatrixHandle        = 0;
    GLint mCameraTexSampler2DHandle     = 0;
};

#endif //VIDEOPHOTOBOOKV2_GLESRENDERER_H
