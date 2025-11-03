/* 1.対応ヘッダ(同名ヘッダ) */
#include "GLESRenderer.h"
/* 2.C++ 標準ライブラリのヘッダ */
/* 3.他の外部ライブラリのヘッダ */
/* 4.プロジェクト内(ローカル)ヘッダ */
#include "GLESUtils.h"
#include "Shaders.h"

bool GLESRenderer::init() {
    /* Define clear color */
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

    /* Setup for Camera映像 Background rendering */
    mCameraShaderProgramID = GLESUtils::createProgram(cameraVertexShaderSrc, cameraFragmentShaderSrc);
    mCameraVertexPositionHandle = glGetAttribLocation(mCameraShaderProgramID, "cameraVertexPosition");
    mCameraTextureCoordHandle = glGetAttribLocation(mCameraShaderProgramID, "cameraVertexTextureCoord");
    mCameraMvpMatrixHandle = glGetUniformLocation(mCameraShaderProgramID, "cameraProjectionMatrix");
    mCameraTexSampler2DHandle = glGetUniformLocation(mCameraShaderProgramID, "cameraTexSampler2D");

    return true;
}
