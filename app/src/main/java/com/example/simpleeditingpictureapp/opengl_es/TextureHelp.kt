package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder


object TextureHelp {
    fun loadTexture(resource: Bitmap) : Int {
        val textureIds = IntArray(1)

        // 申请纹理ID
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]
        if (textureId == 0) {
            throw RuntimeException("Error generating texture")
        }

        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 设置纹理参数
        // 纹理过滤：缩放图片时的像素插值方式（GL_LINEAR=线性插值，缩放更平滑）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        // 纹理环绕：纹理坐标超出[0,1]时的处理（GL_CLAMP_TO_EDGE=边缘拉伸，避免纹理重复）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // 将 Bitmap 数据上传到纹理
        val bitmapBuffer = ByteBuffer.allocateDirect(resource.byteCount)
            .order(ByteOrder.nativeOrder())
        resource.copyPixelsToBuffer(bitmapBuffer)
        bitmapBuffer.position(0)

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            resource.width,
            resource.height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            bitmapBuffer
        )
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        resource.recycle()
        return textureId
    }

    fun deleteTexture(textureId: Int) {
        val textureIds = intArrayOf(textureId)
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, textureIds, 0)
        }
    }
}