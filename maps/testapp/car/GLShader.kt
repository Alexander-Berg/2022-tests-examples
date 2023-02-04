package com.yandex.maps.testapp.car

import android.opengl.GLES20

class GLShader(type: Int, shaderSource: String) {
    private val id = GLES20.glCreateShader(type)

    init {
        GLES20.glShaderSource(id, shaderSource)
        GLES20.glCompileShader(id)
    }

    fun destroy() {
        GLES20.glDeleteShader(id)
    }

    fun attachToProgram(programId: Int) {
        GLES20.glAttachShader(programId, id)
    }
}
