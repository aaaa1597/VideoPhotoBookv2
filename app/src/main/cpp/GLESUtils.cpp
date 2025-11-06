/* 1.対応ヘッダ(同名ヘッダ) */
#include "GLESUtils.h"
/* 2.C++ 標準ライブラリのヘッダ */
#include <string>
/* 3.他の外部ライブラリのヘッダ */
#include <android/log.h>
#include <malloc.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

namespace l {
    extern void garnishLog(const std::string &logstr);
}

void GLESUtils::checkGlError(const char *operation) {
    if (DEBUG_GL) {
        for(GLuint error = glGetError(); error; error = glGetError())
            __android_log_print(ANDROID_LOG_INFO, "aaaaa", "after %s() glError (0x%x)", operation, error);
    }
}

GLuint GLESUtils::initShader(GLenum shaderType, const char *source) {
    l::garnishLog("GLESUtils::initShader start{}");
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &source, nullptr);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);

        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*)malloc(static_cast<size_t>(infoLen));
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, nullptr, buf);
                    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Could not compile shader %d: %s", shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

GLuint GLESUtils::createProgram(const char *vertexShaderBuffer, const char *fragmentShaderBuffer) {
    GLuint vertexShader = initShader(GL_VERTEX_SHADER, vertexShaderBuffer);
    if (!vertexShader)
        return 0;

    GLuint fragmentShader = initShader(GL_FRAGMENT_SHADER, fragmentShaderBuffer);
    if (!fragmentShader)
        return 0;

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");

        glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader");

        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);

        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(program, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*)malloc(static_cast<size_t>(bufLength));
                if (buf) {
                    glGetProgramInfoLog(program, bufLength, nullptr, buf);
                    __android_log_print(ANDROID_LOG_INFO, "aaaaa", "Could not link program: %s", buf);
                    free(buf);
                }
            }
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

unsigned int GLESUtils::createTexture(int width, int height, const unsigned char *data, GLenum format) {
    GLuint gl_TextureID = -1;
    if (data == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, "aaaaa", "Error: Cannot create a texture from null data");
        return gl_TextureID;
    }

    glGenTextures(1, &gl_TextureID);

    glBindTexture(GL_TEXTURE_2D, gl_TextureID);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, data);

    glBindTexture(GL_TEXTURE_2D, 0);

    GLESUtils::checkGlError("Creating texture from image");

    return gl_TextureID;
}

bool GLESUtils::destroyTexture(GLuint textureId) {
    glDeleteTextures(1, &textureId);
    GLESUtils::checkGlError("After glDeleteTextures");
    return true;
}
