package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.example.simpleeditingpictureapp.R
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EditorRenderer(
    private val context: Context,
    private val imageUri: Uri?
) : GLSurfaceView.Renderer {

    // 顶点和纹理坐标 (正方形)
    private val vertices = floatArrayOf(
        -1f, -1f,  // 左下
        1f, -1f,   // 右下
        -1f, 1f,   // 左上
        1f, 1f     // 右上
    )

    private val texCoords = floatArrayOf(
        0f, 1f,  // 左下 (OpenGL纹理坐标原点在左下)
        1f, 1f,  // 右下
        0f, 0f,  // 左上
        1f, 0f   // 右上
    )

    // 编辑模式
    enum class EditMode {
        NONE,
        CROPPING,
        FILTERING
    }

    private var currentMode = EditMode.NONE

    // 矩阵
    private val scaleMatrix = FloatArray(16)  // 顶点变换矩阵 (缩放/平移)
    private val cropMatrix = FloatArray(16)   // 裁切矩阵 (作用于顶点)
    private val texMatrix = FloatArray(16)    // 纹理坐标变换矩阵

    // 滤镜参数
    private var isFiltering = false
    private var isGrayscale = false
    private var contrast = 1.0f
    private var saturation = 1.0f

    // OpenGL 资源
    private var programId = 0
    private var textureId = 0
    private var vertexBuffer: FloatBuffer
    private var texBuffer: FloatBuffer

    // Uniform 位置
    private var uMatrixLoc = -1
    private var uTexMatrixLoc = -1
    private var uIsCroppingLoc = -1
    private var uIsFilteringLoc = -1
    private var uGrayscaleLoc = -1
    private var uContrastLoc = -1
    private var uSaturationLoc = -1

    // 属性位置
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1

    init {
        // 初始化缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices).position(0) }

        texBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(texCoords).position(0) }

        // 初始化矩阵
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.setIdentityM(cropMatrix, 0)
        Matrix.setIdentityM(texMatrix, 0)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, R.raw.vertex_shader)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_shader)

        // 创建程序
        programId = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        // 获取属性/Uniform位置
        aPositionLoc = GLES20.glGetAttribLocation(programId, "a_position")
        aTexCoordLoc = GLES20.glGetAttribLocation(programId, "a_tex_coord")
        uMatrixLoc = GLES20.glGetUniformLocation(programId, "u_matrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(programId, "u_tex_matrix")
        uIsCroppingLoc = GLES20.glGetUniformLocation(programId, "u_is_cropping")
        uIsFilteringLoc = GLES20.glGetUniformLocation(programId, "u_is_filtering")
        uGrayscaleLoc = GLES20.glGetUniformLocation(programId, "u_gray_scale")
        uContrastLoc = GLES20.glGetUniformLocation(programId, "u_contrast")  // 修正拼写
        uSaturationLoc = GLES20.glGetUniformLocation(programId, "u_saturation")

        // 加载纹理
        loadTexture()

        // 设置默认状态
        GLES20.glUseProgram(programId)
        GLES20.glUniform1i(uIsCroppingLoc, 0)
        GLES20.glUniform1i(uIsFilteringLoc, 0)
        GLES20.glUniform1i(uGrayscaleLoc, 0)
        GLES20.glUniform1f(uContrastLoc, 1.0f)
        GLES20.glUniform1f(uSaturationLoc, 1.0f)

        // 启用混合 (支持透明度)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        // 处理屏幕旋转 (保持图片比例)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.orthoM(scaleMatrix, 0, -ratio, ratio, -1f, 1f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 使用着色器程序
        GLES20.glUseProgram(programId)

        // 1. 设置顶点位置
        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(
            aPositionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )

        // 2. 设置纹理坐标
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)
        GLES20.glVertexAttribPointer(
            aTexCoordLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texBuffer
        )

        // 3. 设置变换矩阵
        GLES20.glUniformMatrix4fv(uMatrixLoc, 1, false, scaleMatrix, 0)

        // 4. 设置裁切矩阵 (仅在裁切模式下生效)
        if (currentMode == EditMode.CROPPING) {
            GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, cropMatrix, 0)
            GLES20.glUniform1i(uIsCroppingLoc, 1)
        } else {
            GLES20.glUniform1i(uIsCroppingLoc, 0)
        }

        // 5. 设置滤镜参数
        if (currentMode == EditMode.FILTERING || isFiltering) {
            GLES20.glUniform1i(uIsFilteringLoc, 1)
            GLES20.glUniform1i(uGrayscaleLoc, if (isGrayscale) 1 else 0)
            GLES20.glUniform1f(uContrastLoc, contrast)
            GLES20.glUniform1f(uSaturationLoc, saturation)
        } else {
            GLES20.glUniform1i(uIsFilteringLoc, 0)
        }

        // 6. 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // 7. 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 8. 清理
        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTexCoordLoc)
    }

    // =============== 公共编辑接口 ===============
    fun setEditMode(mode: EditMode) {
        currentMode = mode
    }

    fun getEditMode(): EditMode {
        return currentMode
    }

    fun applyScaling(scaleX: Float, scaleY: Float) {
        if (currentMode != EditMode.NONE) return

        // 重置并应用缩放
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.scaleM(scaleMatrix, 0, scaleX, scaleY, 1f)
    }

    fun applyCropping(cropRect: android.graphics.RectF) {
        if (currentMode != EditMode.CROPPING) return

        // 1. 重置裁切矩阵
        Matrix.setIdentityM(cropMatrix, 0)

        // 2. 计算裁切区域 (归一化坐标)
        val width = cropRect.width()
        val height = cropRect.height()

        // 3. 平移到裁切区域
        Matrix.translateM(cropMatrix, 0, cropRect.left * 2 - 1f, cropRect.top * 2 - 1f, 0f)

        // 4. 缩放到裁切区域大小
        Matrix.scaleM(cropMatrix, 0, width * 2f, height * 2f, 1f)
    }

    fun applyGrayscale(enable: Boolean) {
        isGrayscale = enable
        isFiltering = enable || contrast != 1.0f || saturation != 1.0f
    }

    fun applyContrast(value: Float) {
        contrast = value.coerceIn(0.1f, 3.0f)
        isFiltering = isGrayscale || contrast != 1.0f || saturation != 1.0f
    }

    fun applySaturation(value: Float) {
        saturation = value.coerceIn(0.0f, 2.0f)
        isFiltering = isGrayscale || contrast != 1.0f || saturation != 1.0f
    }

    // =============== 辅助方法 ===============
    private fun loadShader(type: Int, resourceId: Int): Int {
        val shaderCode = try {
            context.resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw RuntimeException("Failed to load shader resource", e)
        }

        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            // 检查编译状态
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val error = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                throw RuntimeException("Shader compilation failed: $error")
            }
        }
    }

    private fun loadTexture() {
        try {
            // 从URI加载Bitmap
            val bitmap = if (imageUri != null) {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } else {
                BitmapFactory.decodeResource(context.resources, R.drawable.ddefault)
            } ?: throw IOException("Failed to load bitmap")

            // 生成纹理
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]

            // 绑定并设置参数
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            // 上传纹理数据
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            // 释放资源
            bitmap.recycle()
        } catch (e: Exception) {
            throw RuntimeException("Texture loading failed", e)
        }
    }
}