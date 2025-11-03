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


#endif //VIDEOPHOTOBOOKV2_SHADERS_H
