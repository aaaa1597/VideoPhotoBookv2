/* 1.対応ヘッダ(同名ヘッダ) */
#include "GLESUtils.h"
/* 2.C++ 標準ライブラリのヘッダ */
/* 3.他の外部ライブラリのヘッダ */
#include <android/log.h>
#include <malloc.h>
/* 4.プロジェクト内(ローカル)ヘッダ */


void GLESUtils::checkGlError(const char *operation) {
    if (DEBUG_GL) {
        for(GLuint error = glGetError(); error; error = glGetError())
            __android_log_print(ANDROID_LOG_INFO, "aaaaa", "after %s() glError (0x%x)", operation, error);
    }
}

GLuint GLESUtils::initShader(GLenum shaderType, const char *source) {
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
