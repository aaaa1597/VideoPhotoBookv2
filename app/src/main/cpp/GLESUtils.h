#ifndef VIDEOPHOTOBOOKV2_GLESUTILS_H
#define VIDEOPHOTOBOOKV2_GLESUTILS_H

/* 1.対応ヘッダ(同名ヘッダ) */
/* 2.C++ 標準ライブラリのヘッダ */
/* 3.他の外部ライブラリのヘッダ */
#include <GLES3/gl31.h>
/* 4.プロジェクト内(ローカル)ヘッダ */

/** A utility class used by the Vuforia Engine samples. */
class GLESUtils {
private:
    /** Enable this flag to debug OpenGL errors */
    static const bool DEBUG_GL = false;

public:
    /** Prints GL error information. */
    static void checkGlError(const char* operation);
    /** Initialize a shader. */
    static GLuint initShader(GLenum shaderType, const char* source);
    /** Create a shader program. */
    static GLuint createProgram(const char* vertexShaderBuffer, const char* fragmentShaderBuffer);
    /** Create a texture from a byte vector */
    static unsigned int createTexture(int width, int height, const unsigned char* data, GLenum format = GL_RGBA);
    /** Clean up texture */
    static bool destroyTexture(GLuint textureId);
};
#endif //VIDEOPHOTOBOOKV2_GLESUTILS_H
