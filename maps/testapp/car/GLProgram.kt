package com.yandex.maps.testapp.car

import android.opengl.GLES20

open class GLProgram(vertexShaderSource: String, fragmentShaderSource: String) {
    private val id = GLES20.glCreateProgram()
    private val vertexShader = GLShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
    private val fragmentShader = GLShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)
    init {
        vertexShader.attachToProgram(id)
        fragmentShader.attachToProgram(id)
        GLES20.glLinkProgram(id)
    }

    fun locateAttribute(name: String) : Int {
        return GLES20.glGetAttribLocation(id, name)
    }

    fun activate() {
        GLES20.glUseProgram(id)
    }
}
