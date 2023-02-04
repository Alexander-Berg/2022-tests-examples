package com.yandex.maps.testapp.car

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.yandex.maps.testapp.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class OverlayRenderer(context: Context) : GLSurfaceView.Renderer {
    private var program : TextureProgram? = null
    private var vbo : GLVertexBuffer? = null
    private var texture : GLTexture? = null
    private val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.a0)

    override fun onDrawFrame(gl: GL10?) {
        if (program == null || vbo == null || texture == null) {
            return
        }

        GLES20.glBlendFuncSeparate(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA, GLES20.GL_ZERO, GLES20.GL_ONE)
        GLES20.glEnable(GLES20.GL_BLEND)

        program!!.activate()
        texture!!.bind()
        GLES20.glEnableVertexAttribArray(program!!.posAttribLocation)
        GLES20.glEnableVertexAttribArray(program!!.texCoordAttribLocation)
        vbo!!.bind()
        GLES20.glVertexAttribPointer(program!!.posAttribLocation, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glVertexAttribPointer(program!!.texCoordAttribLocation, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        GLES20.glDisable(GLES20.GL_BLEND)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        vbo?.destroy()

        val xMul = 2.0f / width
        val yMul = 2.0f / height
        // place texture in the top right corner
        val xl = xMul * (0.5f * width - bitmap.width)
        val yb = yMul * (0.5f * height - bitmap.height)
        val data = FloatArray(4 * 6)
        writeRectToBuffer(xl, yb, xMul * bitmap.width, yMul * bitmap.height, data, 0, 4)
        writeRectToBuffer(0.0f, 1.0f, 1.0f, -1.0f, data, 2, 4)
        val memSize = data.size * 4
        val buffer = ByteBuffer.allocateDirect(memSize).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(data).rewind()
        vbo = GLVertexBuffer(buffer, memSize)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = TextureProgram()
        texture = GLTexture(bitmap)
        vbo = null
    }

    fun writeRectToBuffer(xl: Float, yb: Float, w: Float, h: Float, dst: FloatArray, offset: Int, stride: Int) {
        writePointToBuffer(xl, yb, dst, offset)
        writePointToBuffer(xl, yb + h, dst, offset + stride)
        writePointToBuffer(xl + w, yb + h, dst, offset + 2 * stride)
        writePointToBuffer(xl + w, yb + h, dst, offset + 3 * stride)
        writePointToBuffer(xl + w, yb, dst, offset + 4 * stride)
        writePointToBuffer(xl, yb, dst, offset + 5 * stride)
    }

    fun writePointToBuffer(x: Float, y: Float, dst: FloatArray, offset: Int) {
        dst[offset] = x
        dst[offset + 1] = y
    }
}
