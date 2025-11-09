#ifndef VIDEOPHOTOBOOKV2_SHADERS_H
#define VIDEOPHOTOBOOKV2_SHADERS_H

/////////////////////////////////////////////////////////////////////////////////////////
// Camera映像用shader: vertexTexCoord in vertex shader, texture2D sample
/////////////////////////////////////////////////////////////////////////////////////////
static const char* cameraVertexShaderSrc = R"(
    attribute vec4 cameraVertexPosition;
    attribute vec2 cameraVertexTextureCoord;
    uniform mat4 cameraProjectionMatrix;
    varying vec2 texCoord;
    void main()
    {
        gl_Position = cameraProjectionMatrix * cameraVertexPosition;
        texCoord = cameraVertexTextureCoord;
    }
)";


static const char* cameraFragmentShaderSrc = R"(
    precision mediump float;
    uniform sampler2D cameraTexSampler2D;
    varying vec2 texCoord;
    void main()
    {
        gl_FragColor = texture2D(cameraTexSampler2D, texCoord);
    }
)";

/////////////////////////////////////////////////////////////////////////////////////////
// pause.png用shader: vertexTexCoord in vertex shader, texture2D sample
/////////////////////////////////////////////////////////////////////////////////////////
static const char* VERTEX_SHADER_PAUSE =
        "attribute vec4 a_Position;\n"
        "attribute vec2 a_TexCoord;\n"
        "uniform mat4 u_ProjectionMatrix;\n"
        "varying vec2 v_TexCoord;\n"
        "void main() {\n"
        "  gl_Position = u_ProjectionMatrix * a_Position;\n"
        "  v_TexCoord = a_TexCoord;\n"
        "}\n";

static const char* FRAGMENT_SHADER_PAUSE =
        "precision mediump float;\n"
        "varying vec2 v_TexCoord;\n"
        "uniform sampler2D u_Sampler2D;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(u_Sampler2D, v_TexCoord);\n"
        "}\n";

/////////////////////////////////////////////////////////////////////////////////////////
// VideoPhotoBook用shader: vertexTexCoord in vertex shader, texture2D sample
/////////////////////////////////////////////////////////////////////////////////////////
static const char* VERTEX_SHADER =
        "attribute vec4 a_Position;\n"
        "attribute vec2 a_TexCoord;\n"
        "uniform mat4 u_ProjectionMatrix;\n"
        "varying vec2 v_TexCoord;\n"
        "void main() {\n"
        "  gl_Position = u_ProjectionMatrix * a_Position;\n"
        "  v_TexCoord = a_TexCoord;\n"
        "}\n";

static const char* FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n"
        "precision mediump float;\n"
        "varying vec2 v_TexCoord;\n"
        "uniform samplerExternalOES u_SamplerOES;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(u_SamplerOES, v_TexCoord);\n"
        "}\n";

#endif //VIDEOPHOTOBOOKV2_SHADERS_H
