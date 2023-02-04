package com.yandex.maps.testapp.car

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

class GLTexture(bitmap: Bitmap) {
    private val id : Int

    init {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        id = ids[0]
        bind()
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    fun bind() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
    }

    fun destroy() {
        GLES20.glDeleteTextures(1, IntArray(1) {id}, 0)
    }
}
