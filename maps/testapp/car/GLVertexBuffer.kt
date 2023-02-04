package com.yandex.maps.testapp.car

import android.opengl.GLES20
import java.nio.Buffer

class GLVertexBuffer(buffer: Buffer, memSize: Int) {
    private val id : Int
    init {
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        id = ids[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, memSize, buffer, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun bind() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, id)
    }

    fun destroy() {
        GLES20.glDeleteBuffers(1, IntArray(1) {id}, 0)
    }
}
