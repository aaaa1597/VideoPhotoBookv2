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
    _CameraShaderProgramID = GLESUtils::createProgram(cameraVertexShaderSrc, cameraFragmentShaderSrc);
    _CameraVertexPositionHandle = glGetAttribLocation(_CameraShaderProgramID, "cameraVertexPosition");
    _CameraTextureCoordHandle = glGetAttribLocation(_CameraShaderProgramID, "cameraVertexTextureCoord");
    _CameraMvpMatrixHandle = glGetUniformLocation(_CameraShaderProgramID, "cameraProjectionMatrix");
    _CameraTexSampler2DHandle = glGetUniformLocation(_CameraShaderProgramID, "cameraTexSampler2D");

    /* Setup for Pause.png rendering */
    _pProgram = GLESUtils::createProgram(VERTEX_SHADER_PAUSE, FRAGMENT_SHADER_PAUSE);
    _paPositionLoc = glGetAttribLocation(_pProgram, "a_Position");
    _paTexCoordLoc = glGetAttribLocation(_pProgram, "a_TexCoord");
    _puProjectionMatrixLoc = glGetUniformLocation(_pProgram, "u_ProjectionMatrix");
    _puSampler2D = glGetUniformLocation(_pProgram, "u_Sampler2D");

    return true;
}

/** pause.pngのテクスチャデータ設定 */
void GLESRenderer::setPauseTexture(int width, int height, unsigned char *bytes) {
    createTexture(width, height, bytes, _pTextureId);
}

void GLESRenderer::createTexture(int width, int height, const unsigned char *bytes, GLuint& texId) {
    if (texId != -1) {
        GLESUtils::destroyTexture(texId);
        texId = -1;
    }
    texId = GLESUtils::createTexture(width, height, bytes);
}
