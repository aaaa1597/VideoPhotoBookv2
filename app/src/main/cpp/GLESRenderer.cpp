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

    /* Setup for PhotoBookVideo PlayBack rendering */
    _vProgram = GLESUtils::createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    _vaPosition = glGetAttribLocation(_vProgram, "a_Position");
    _vaTexCoordLoc = glGetAttribLocation(_vProgram, "a_TexCoord");
    _vuProjectionMatrixLoc = glGetUniformLocation(_vProgram, "u_ProjectionMatrix");
    _vuSamplerOES = glGetUniformLocation(_vProgram, "u_SamplerOES");

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

int32_t GLESRenderer::setVideoTexture() {
    GLESRenderer::getIns()._vTextureId = -1;
    glGenTextures(1, &GLESRenderer::getIns()._vTextureId);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, GLESRenderer::getIns()._vTextureId);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    return GLESRenderer::getIns()._vTextureId;
}

void GLESRenderer::renderBackgroundFromCameraImage(const VuMatrix44F &projectionMatrix, const float *vertices, const float *textureCoordinates,
                                                   const int numTriangles, const unsigned int *indices, int textureUnit) {
    GLboolean depthTest = GL_FALSE;
    GLboolean cullTest = GL_FALSE;

    glGetBooleanv(GL_DEPTH_TEST, &depthTest);
    glGetBooleanv(GL_CULL_FACE, &cullTest);

    glDisable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);

    /* Load the shader and upload the vertex/texcoord/index data */
    glUseProgram(_CameraShaderProgramID);
    glVertexAttribPointer(static_cast<GLuint>(_CameraVertexPositionHandle), 3, GL_FLOAT, GL_FALSE, 0, vertices);
    glVertexAttribPointer(static_cast<GLuint>(_CameraTextureCoordHandle), 2, GL_FLOAT, GL_FALSE, 0, textureCoordinates);

    glUniform1i(_CameraTexSampler2DHandle, textureUnit);

    /* Render the video background with the custom shader First, we enable the vertex arrays */
    glEnableVertexAttribArray(static_cast<GLuint>(_CameraVertexPositionHandle));
    glEnableVertexAttribArray(static_cast<GLuint>(_CameraTextureCoordHandle));

    /* Pass the projection matrix to OpenGL */
    glUniformMatrix4fv(_CameraMvpMatrixHandle, 1, GL_FALSE, projectionMatrix.data);

    /* Then, we issue the render call */
    glDrawElements(GL_TRIANGLES, numTriangles * 3, GL_UNSIGNED_INT, indices);

    /* Finally, we disable the vertex arrays */
    glDisableVertexAttribArray(static_cast<GLuint>(_CameraVertexPositionHandle));
    glDisableVertexAttribArray(static_cast<GLuint>(_CameraTextureCoordHandle));

    if (depthTest)
        glEnable(GL_DEPTH_TEST);

    if (cullTest)
        glEnable(GL_CULL_FACE);

    GLESUtils::checkGlError("Render background from camera image.");
}

void GLESRenderer::renderVideoPlayback(const VuMatrix44F &projectionMatrix, const VuMatrix44F &scaledModelViewMatrix,
                                       const VuVector2F &markerSize, const std::string &targetName) {
    VuMatrix44F scaledModelViewProjectionMatrix = vuMatrix44FMultiplyMatrix(projectionMatrix, scaledModelViewMatrix);

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glUseProgram(_vProgram);

    float scaleX = 0.5f;
    float scaleY = 0.5f;

    if(_fullscreenFlg) {
        scaleX = 1.0f;
        scaleY = 1.0f;

        float videoAspect = _vVideoWidth / _vVideoHeight;
        float screenAspect= _screenWidth / _screenHeight;

        if (screenAspect > videoAspect) /* 横長動画 → 横を1.0にして縦を縮める */
            scaleX = videoAspect / screenAspect;
        else    /* 縦長動画 → 縦を1.0にして横を縮める */
            scaleY = screenAspect / videoAspect;
    }
    else {
        /* Calculation of vertex coordinates considering the aspect ratio. */
        float markerAspect = markerSize.data[0] / markerSize.data[1];
        float videoAspect = _vVideoWidth / _vVideoHeight;

        if(markerAspect > videoAspect)  /* When the marker is wider than the video. */
            scaleX = scaleX * (videoAspect / markerAspect);
        else    /* When the marker is taller than the video or has the same aspect ratio. */
            scaleY = scaleY * (markerAspect / videoAspect);
    };

    GLfloat vertices[] = {
        -scaleX, -scaleY, 0.0f, /* 左下 */
         scaleX, -scaleY, 0.0f, /* 右下 */
        -scaleX,  scaleY, 0.0f, /* 左上 */
         scaleX,  scaleY, 0.0f  /* 右上 */
    };

    GLfloat texCoords[] = {
        0.0f, 1.0f, /* 左下 */
        1.0f, 1.0f, /* 右下 */
        0.0f, 0.0f, /* 左上 */
        1.0f, 0.0f  /* 右上 */
    };

    glVertexAttribPointer(_vaPosition, 3, GL_FLOAT, GL_FALSE, 0, vertices);
    glVertexAttribPointer(_vaTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 0, texCoords);
    glEnableVertexAttribArray(_vaPosition);
    glEnableVertexAttribArray(_vaTexCoordLoc);

    if(_fullscreenFlg) {
        const GLfloat identityMatrix[16] = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
        glUniformMatrix4fv(_vuProjectionMatrixLoc, 1, GL_FALSE, identityMatrix);
    }
    else
        glUniformMatrix4fv(_vuProjectionMatrixLoc, 1, GL_FALSE, &scaledModelViewProjectionMatrix.data[0]);

    /* 当たり判定用に板ポリ座標をNDC(正規化デバイス座標)に変換して保持 */
    std::array<glm::vec2, 4> ndcQuadPoints = {};
    int idx = 0;
    for(int lpct : {0,1,3,2/*左下→右下→右上→左上→左下の順番にする必要がある*/}) {
        glm::vec4 pos = glm::vec4(vertices[lpct*3], vertices[lpct*3+1],vertices[lpct*3+2],1.0f);
        glm::vec4 glpos = glm::make_mat4(scaledModelViewProjectionMatrix.data) * pos;
        glm::vec3 ndcpos = glm::vec3(glpos) / glpos.w;
        ndcQuadPoints[idx++] = glm::vec2(ndcpos);
    }
    _ndcQuadPoints[targetName] = std::pair(std::chrono::system_clock::now(), ndcQuadPoints);
    /* 古い(1000[ms]過ぎた)データは削除する */
    for (auto it = _ndcQuadPoints.begin(); it != _ndcQuadPoints.end(); ) {
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now() - it->second.first
        );
        if (duration.count() > 1000)
            it = _ndcQuadPoints.erase(it);  /* erase() は次のイテレータを返す */
        else
            ++it;
    }

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, _vTextureId);
    glUniform1i(_vuSamplerOES, 0);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableVertexAttribArray(_vaPosition);
    glDisableVertexAttribArray(_vaTexCoordLoc);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    glUseProgram(0);

    return;
}

void GLESRenderer::renderPause(const VuMatrix44F &projectionMatrix, const VuMatrix44F &scaledModelViewMatrix, const VuVector2F &markerSize, const std::string &targetName) {
    VuMatrix44F scaledModelViewProjectionMatrix = vuMatrix44FMultiplyMatrix(projectionMatrix, scaledModelViewMatrix);

    glEnable(GL_DEPTH_TEST);
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    glUseProgram(_pProgram);

    const GLfloat vertices[] = {
        -0.5, -0.5, 0.0f, /* 左下 */
         0.5, -0.5, 0.0f, /* 右下 */
        -0.5,  0.5, 0.0f, /* 左上 */
         0.5,  0.5, 0.0f  /* 右上 */
    };

    const GLfloat texCoords[] = {
        0.0f, 1.0f, /* 左下 */
        1.0f, 1.0f, /* 右下 */
        0.0f, 0.0f, /* 左上 */
        1.0f, 0.0f  /* 右上 */
    };

    glVertexAttribPointer(_paPositionLoc, 3, GL_FLOAT, GL_FALSE, 0, vertices);
    glVertexAttribPointer(_paTexCoordLoc, 2, GL_FLOAT, GL_FALSE, 0, texCoords);
    glEnableVertexAttribArray(_paPositionLoc);
    glEnableVertexAttribArray(_paTexCoordLoc);

    glUniformMatrix4fv(_vuProjectionMatrixLoc, 1, GL_FALSE, &scaledModelViewProjectionMatrix.data[0]);

    /* 当たり判定用に板ポリ座標をNDC(正規化デバイス座標)に変換して保持 */
    std::array<glm::vec2, 4> ndcQuadPoints = {};
    int idx = 0;
    for(int lpct : {0,1,3,2/*左下→右下→右上→左上→左下の順番にする必要がある*/}) {
        glm::vec4 pos = glm::vec4(vertices[lpct*3], vertices[lpct*3+1],vertices[lpct*3+2],1.0f);
        glm::vec4 glpos = glm::make_mat4(scaledModelViewProjectionMatrix.data) * pos;
        glm::vec3 ndcpos = glm::vec3(glpos) / glpos.w;
        ndcQuadPoints[idx++] = glm::vec2(ndcpos);
    }
    _ndcQuadPoints[targetName] = std::pair(std::chrono::system_clock::now(), ndcQuadPoints);
    /* 古い(1000[ms]過ぎた)データは削除する */
    for (auto it = _ndcQuadPoints.begin(); it != _ndcQuadPoints.end(); ) {
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::system_clock::now() - it->second.first
        );
        if (duration.count() > 1000)
            it = _ndcQuadPoints.erase(it);  /* erase() は次のイテレータを返す */
        else
            ++it;
    }

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, _pTextureId);
    glUniform1i(_puSampler2D, 0);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableVertexAttribArray(_paPositionLoc);
    glDisableVertexAttribArray(_paTexCoordLoc);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
}

void GLESRenderer::deinit() {
    if (_vTextureId != -1) {
        GLESUtils::destroyTexture(_vTextureId);
        _vTextureId = -1;
    }
    if (_pTextureId != -1) {
        GLESUtils::destroyTexture(_pTextureId);
        _pTextureId = -1;
    }
}
