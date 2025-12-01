package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.simpleeditingpictureapp.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.util.Log


class CropFrameRenderer(
    private val context: Context
) : GLSurfaceView.Renderer{
    private val tag = "CropFrameRenderer"
    private var program = 0
    private var vertexBuffer: FloatBuffer? = null

    // 着色器参数
    private var aPositionLoc = -1
    private var uMatrixLoc = -1
    private var uColorLoc = -1
    private var uDrawTypeLoc = -1
    private var uCircleCenterLoc = -1
    private var uCircleRadiusLoc = -1

    // 投影矩阵
    private val mProjectMatrix = FloatArray(16)


    // 裁剪框
    private var cropFrameRect: RectF = RectF(0.1f, 0.1f, 0.9f, 0.9f)

    // 控件大小
    private val controlBigRadius = 0.02f
    private val controlSmallRadius = 0.01f


    override fun onDrawFrame(gl: GL10?) {
        // 清屏, 透明背景
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 使用程序
        GLES20.glUseProgram(program)
        // 传递投影矩阵
        GLES20.glUniformMatrix4fv(uMatrixLoc, 1, false, mProjectMatrix, 0)

        // 绘制裁剪框
        drawMask()
        drawCropFrame()
        drawGrid()
        drawControlPoints()
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES20.glViewport(0, 0, width, height)

        Matrix.orthoM(
            mProjectMatrix,0,
            0.0f, 1.0f,
            1.0f, 0.0f,
            -1.0f, 1.0f
        )
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        try {
            Log.d(tag, "CropFrameRenderer onSurfaceCreated started")

            // 设置透明清屏颜色（叠加在图片上）
            Log.d(tag, "Setting clear color and blend mode")
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            GLES20.glEnable(GLES20.GL_BLEND) // 开启混合（处理透明）
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

            // 创建程序
            Log.d(tag, "Creating shader program")
            createProgram()
            Log.d(tag, "Shader program created")

            // 获取着色器参数位置
            Log.d(tag, "Finding shader locations")
            findShaderLocation()
            Log.d(tag, "Shader locations found")

            // 创建VBO
            Log.d(tag, "Initializing vertex buffer")
            initVertexBuffer()
            Log.d(tag, "Vertex buffer initialized")

            Log.d(tag, "CropFrameRenderer onSurfaceCreated completed")
        } catch (e: Exception) {
            Log.e(tag, "Error in CropFrameRenderer onSurfaceCreated", e)
            throw e
        }
    }

    fun updateCropFrameRect(rect: RectF) {
        cropFrameRect = rect
    }

    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
        }
    }

    private fun createProgram() {
        try {
            Log.d(tag, "Creating shader program")

            Log.d(tag, "Reading vertex shader")
            val vertexShaderCode = ShaderHelper.readShaderFileFromResource(R.raw.crop_vertex_shader, context)
            Log.d(tag, "Reading fragment shader")
            val fragmentShaderCode = ShaderHelper.readShaderFileFromResource(R.raw.crop_fragment_shader, context)

            Log.d(tag, "Compiling vertex shader")
            val vertexShaderId = ShaderHelper.compileVertexShader(vertexShaderCode)
            if (vertexShaderId == 0) {
                Log.e(tag, "Failed to compile vertex shader")
                throw RuntimeException("Vertex shader compilation failed")
            }

            Log.d(tag, "Compiling fragment shader")
            val fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentShaderCode)
            if (fragmentShaderId == 0) {
                Log.e(tag, "Failed to compile fragment shader")
                throw RuntimeException("Fragment shader compilation failed")
            }

            Log.d(tag, "Linking shader program")
            program = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId)
            if (program == 0){
                Log.e(tag, "Failed to link shader program")
                throw RuntimeException("Shader program linking failed")
            }

            Log.d(tag, "Using shader program")
            GLES20.glUseProgram(program)
            Log.d(tag, "Shader program created and used successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in createProgram", e)
            throw e
        }
    }

    private fun findShaderLocation() {
        aPositionLoc = GLES20.glGetAttribLocation(program, "a_position")
        uMatrixLoc = GLES20.glGetUniformLocation(program, "u_matrix")
        uColorLoc = GLES20.glGetUniformLocation(program, "u_color")
        uDrawTypeLoc = GLES20.glGetUniformLocation(program, "u_draw_type")
        uCircleCenterLoc = GLES20.glGetUniformLocation(program, "u_circle_center")
        uCircleRadiusLoc = GLES20.glGetUniformLocation(program, "u_circle_radius")

        if (aPositionLoc == -1 || uMatrixLoc == -1 || uColorLoc == -1) {
            throw RuntimeException("Failed to get shader location: aPosition=$aPositionLoc, uMatrix=$uMatrixLoc, uColor=$uColorLoc")
        }

    }

    private fun initVertexBuffer() {
        vertexBuffer = ByteBuffer.allocateDirect(4 * 2 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    }

    private fun updateVertexBuffer(vertices: FloatArray) {
        vertexBuffer?.clear()
        vertexBuffer?.put(vertices)
        vertexBuffer?.position(0)

        // 绑定顶点属性
        GLES20.glVertexAttribPointer(
            aPositionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * Float.SIZE_BYTES,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(aPositionLoc)
    }

    private fun drawMask() {
        val maskColor = floatArrayOf(0.0f, 0.0f, 0.0f, 0.5f)
        // 左遮罩
        drawRect(RectF(0f, 0f, cropFrameRect.left, 1f), maskColor)
        // 右遮罩
        drawRect(RectF(cropFrameRect.right, 0f, 1f, 1f), maskColor)
        // 上遮罩
        drawRect(RectF(cropFrameRect.left, 0f, cropFrameRect.right, cropFrameRect.top), maskColor)
        // 下遮罩
        drawRect(RectF(cropFrameRect.left, cropFrameRect.bottom, cropFrameRect.right, 1f), maskColor)

    }
    private fun drawCropFrame() {
        val frameColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        drawRect(cropFrameRect, frameColor, false)
    }
    private fun drawGrid() {
        val gridColor = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val cropFrameWith = cropFrameRect.width()
        val cropFrameHeight = cropFrameRect.height()

        // 竖线：1/3、2/3 位置
        drawLine(
            cropFrameRect.left + cropFrameWith / 3, cropFrameRect.top,
            cropFrameRect.left + cropFrameWith / 3 , cropFrameRect.bottom,
            gridColor
        )
        drawLine(
            cropFrameRect.left + cropFrameWith*2/3, cropFrameRect.top,
            cropFrameRect.left + cropFrameWith*2/3, cropFrameRect.bottom,
            gridColor
        )

        // 横线：1/3、2/3 位置
        drawLine(
            cropFrameRect.left, cropFrameRect.top + cropFrameHeight/3,
            cropFrameRect.right, cropFrameRect.top + cropFrameHeight/3,
            gridColor
        )
        drawLine(
            cropFrameRect.left, cropFrameRect.top + cropFrameHeight*2/3,
            cropFrameRect.right, cropFrameRect.top + cropFrameHeight*2/3,
            gridColor
        )
    }
    private fun drawControlPoints() {
        // 黑色
        val controlColor = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

        // 四角控制点（大）
        drawCircle(cropFrameRect.left, cropFrameRect.top, controlBigRadius, controlColor)
        drawCircle(cropFrameRect.right, cropFrameRect.top, controlBigRadius, controlColor)
        drawCircle(cropFrameRect.left, cropFrameRect.bottom, controlBigRadius, controlColor)
        drawCircle(cropFrameRect.right, cropFrameRect.bottom, controlBigRadius, controlColor)

        // 四边中点控制点（小）
        drawCircle((cropFrameRect.left + cropFrameRect.right)/2, cropFrameRect.top, controlSmallRadius, controlColor)
        drawCircle(cropFrameRect.left, (cropFrameRect.top + cropFrameRect.bottom)/2, controlSmallRadius, controlColor)
        drawCircle((cropFrameRect.left + cropFrameRect.right)/2, cropFrameRect.bottom, controlSmallRadius, controlColor)
        drawCircle(cropFrameRect.right, (cropFrameRect.top + cropFrameRect.bottom)/2, controlSmallRadius, controlColor)
    }

    private fun drawRect(rect: RectF, color: FloatArray, isFill: Boolean = true) {
        val vertices = floatArrayOf(
            rect.left, rect.top,
            rect.left, rect.bottom,
            rect.right, rect.top,
            rect.right, rect.bottom
        )
        updateVertexBuffer(vertices)

        GLES20.glUniform4fv(uColorLoc, 1, color, 0)
        GLES20.glUniform1i(uDrawTypeLoc, 0)

        val drawMode = if (isFill) GLES20.GL_TRIANGLE_STRIP else GLES20.GL_LINE_LOOP
        GLES20.glDrawArrays(drawMode, 0, 4)
    }

    private fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: FloatArray) {
        val vertices = floatArrayOf(
            x1, y1,
            x2, y2
        )
        updateVertexBuffer(vertices)

        GLES20.glUniform4fv(uColorLoc, 1, color, 0)
        GLES20.glUniform1i(uDrawTypeLoc, 0)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
    }

    private fun drawCircle(centerX: Float, centerY: Float, radius: Float, color: FloatArray) {
        val vertices = floatArrayOf(
            centerX - radius, centerY - radius,
            centerX + radius, centerY - radius,
            centerX - radius, centerY + radius,
            centerX + radius, centerY + radius
        )
        updateVertexBuffer(vertices)

        GLES20.glUniform4fv(uColorLoc, 1, color, 0)
        GLES20.glUniform1f(uDrawTypeLoc, 1.0f)
        GLES20.glUniform2f(uCircleCenterLoc, centerX, centerY)
        GLES20.glUniform1f(uCircleRadiusLoc, radius)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }
}