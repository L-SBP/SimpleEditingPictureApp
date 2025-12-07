package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.bumptech.glide.Glide
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.activity.EditorActivity
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import androidx.core.graphics.createBitmap
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.simpleeditingpictureapp.viewmodel.EditorViewModel
import kotlin.math.max
import kotlin.math.min

class EditorRenderer (
    private val context: Context,
) : GLSurfaceView.Renderer {
    private val tag = "EditorRenderer"

    // 裁剪矩阵变化监听器
    interface CropMatrixChangeListener {
        fun onCropMatrixChanged(matrix: FloatArray)
    }

    private var cropMatrixChangeListener: CropMatrixChangeListener? = null
    private var viewModel: EditorViewModel? = null

    // 图片宽高比和显示模式
    private var imageAspectRatio = 1.0f
    private var displayMode = DisplayMode.FIT_CENTER // 默认居中适配

    // 显示区域计算
    private var displayRect = RectF(0f, 0f, 1f, 1f)

    // 显示模式枚举
    enum class DisplayMode {
        FIT_CENTER,     // 保持比例，居中显示（可能有黑边）
        CROP_FILL,      // 裁剪模式：填满整个视图
        ORIGINAL        // 原始比例，不缩放
    }

    // --- GL Program & Data ---
    private var program: Int = -1
    private var textureId = -1
    private var vertexVboId = -1
    private val vertexData = floatArrayOf(
        0.0f, 0.0f, // 屏幕左上角
        0.0f, 1.0f, // 屏幕左下角
        1.0f, 0.0f, // 屏幕右上角
        1.0f, 1.0f, // 屏幕右下角
    )
    private val textureData = floatArrayOf(
        0.0f, 0.0f, // 纹理左上角
        0.0f, 1.0f, // 纹理左下角
        1.0f, 0.0f, // 纹理右上角
        1.0f, 1.0f, // 纹理右下角
    )
    // 加载图片的参数
    private var imageWidth = 0
    private var imageHeight = 0
    // 缩放 (Scaling)
    private var mScale = 1.0f
    private var mFocusXInGl = 0.0f
    private var mFocusYInGl = 0.0f

    // 平移 (Panning)
    private var mPanX = 0.0f
    private var mPanY = 0.0f

    // 裁剪 (Cropping)
    private var isCropping = false
    private val cropTexMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) } // 裁剪纹理矩阵
    private val cropRect = RectF(0.0f, 0.0f, 1.0f, 1.0f) // 归一化裁剪区域

    // 滤镜 (Filtering)
    private var useGrayscale = false // 灰度滤镜开关
    private var contrast = 1.0f      // 对比度 (1.0为原始值)
    private var saturation = 1.0f  // 饱和度 (1.0为原始值)

    // ===================================================================

    // --- Matrices & Dimensions ---
    private var mViewWidth = 0
    private var mViewHeight = 0
    private val projectionMatrix = FloatArray(16)       // 投影矩阵
    private val mModelMatrix = FloatArray(16)           // 模型矩阵
    private val mvpMatrix = FloatArray(16)              // 投影矩阵 * 模型矩阵
    private val texMatrix = FloatArray(16)              // 纹理矩阵

    // --- Shader Locations ---
    private var aPositionLoc = -1
    private var aTexCoordLoc = -1
    private var uMatrixLoc = -1
    private var uTexMatrixLoc = -1
    private var uIsCroppingLoc = -1
    private var uTextureLoc = -1
    private var uGrayScaleLoc = -1
    private var uContrastLoc = -1
    private var uSaturationLoc = -1

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        Log.d(tag, "onSurfaceCreated 开始")

        // 创建程序对象
        createProgram()
        if (program == 0) {
            Log.e(tag, "着色器程序创建失败")
            return
        }
        Log.d(tag, "着色器程序创建成功，ID: $program")

        // 获取着色器参数位置
        findShaderLocation()

        // 初始化Vbo
        initVertexBuffer()

        // 如果已有bitmap，重新加载纹理
        if (imageWidth > 0 && imageHeight > 0) {
            // 从ViewModel获取ImageEditorModel中的bitmap
            val bitmap = viewModel?.model?.originalBitmap
            if (bitmap != null) {
                textureId = TextureHelp.loadTexture(context, bitmap)
                Log.d(tag, "重新加载纹理，textureId: $textureId")
            }
        }

        // 触发渲染
        (context as EditorActivity).getGLSurfaceView().requestRender()
        Log.d(tag, "onSurfaceCreated 完成")
    }


    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Log.d(tag, "onDrawFrame - textureId: $textureId, isCropping: $isCropping, cropRect: $cropRect")

        // 检查纹理是否已加载
        if (textureId == -1) {
            Log.w(tag, "纹理尚未加载，跳过渲染")
            return
        }

        // 确保着色器程序已创建并正在使用
        GLES20.glUseProgram(program)

        // 1. MVP Matrix for scaling and positioning
        Matrix.setIdentityM(mModelMatrix, 0)
        // 应用缩放（以焦点为中心）
        Matrix.translateM(mModelMatrix, 0, mFocusXInGl, mFocusYInGl, 0.0f)
        Matrix.scaleM(mModelMatrix, 0, mScale, mScale, 1.0f)
        Matrix.translateM(mModelMatrix, 0, -mFocusXInGl, -mFocusYInGl, 0.0f)
        // 应用平移
        Matrix.translateM(mModelMatrix, 0, mPanX, mPanY, 0.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mModelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMatrixLoc, 1, false, mvpMatrix, 0)

        // 2. Texture Matrix for cropping
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, cropTexMatrix, 0)
        Log.d(tag, "应用裁剪矩阵: ${cropTexMatrix.contentToString()}")

        // 3. Uniforms for shader logic (cropping and filtering)
        GLES20.glUniform1i(uIsCroppingLoc, if (isCropping) 1 else 0)

        GLES20.glUniform1i(uGrayScaleLoc, if (useGrayscale) 1 else 0)
        GLES20.glUniform1f(uContrastLoc, contrast)
        GLES20.glUniform1f(uSaturationLoc, saturation)

        // 4. Bind texture and draw
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureLoc, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // 检查OpenGL错误
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(tag, "OpenGL错误: $error")
        }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        // 设置视图宽高
        mViewWidth = width
        mViewHeight = height
        GLES20.glViewport(0, 0, mViewWidth, mViewHeight)

        // 初始化正交投影矩阵 (bottom > top, Y轴翻转)
        Matrix.orthoM(
            projectionMatrix, 0,
            0.0f, 1.0f, // left, right
            1.0f, 0.0f, // bottom, top
            -1.0f, 1.0f // near, far
        )
    }


    fun applyScaling(scale: Float, focusX: Float, focusY: Float){
        mScale = scale
        mFocusXInGl = focusX / mViewWidth
        mFocusYInGl = (mViewHeight - focusY) / mViewHeight
    }

    fun applyPan(dx: Float, dy: Float) {
        // 将屏幕坐标转换为OpenGL坐标
        val dxGl = dx / mViewWidth
        val dyGl = dy / mViewHeight

        // 应用平移，考虑缩放因子
        mPanX += dxGl / mScale
        mPanY += dyGl / mScale

        // 限制平移范围
        val maxPanX = 0.5f * (mScale - 1.0f)
        val maxPanY = 0.5f * (mScale - 1.0f)

        mPanX = max(-maxPanX, min(maxPanX, mPanX))
        mPanY = max(-maxPanY, min(maxPanY, mPanY))
    }


    fun setGrayscaleEnabled(enabled: Boolean) {
        useGrayscale = enabled
    }

    fun setContrast(value: Float) {
        contrast = value
    }

    fun setSaturation(value: Float) {
        saturation = value
    }
    fun setCropMode(enabled: Boolean) {
        isCropping = enabled
        Log.d(tag, "setCropMode: $enabled")
    }

    fun isCropModeEnabled(): Boolean {
        return isCropping
    }

    fun setCropRect(normalizedCropRect: RectF) {
        // 保存裁剪矩形
        cropRect.set(normalizedCropRect)

        // 计算裁剪矩阵：将裁剪区域映射到整个纹理
        Matrix.setIdentityM(cropTexMatrix, 0)

        // 步骤1: 平移 - 将裁剪区域的左上角移动到原点
        Matrix.translateM(cropTexMatrix, 0, cropRect.left, cropRect.top, 0.0f)

        // 步骤2: 缩放 - 将裁剪区域缩放到填满整个纹理空间
        Matrix.scaleM(cropTexMatrix, 0, cropRect.width(), cropRect.height(), 1.0f)

        Log.d(tag, "setCropRect: $normalizedCropRect")

        // 通知监听器裁剪矩阵已更改
        cropMatrixChangeListener?.onCropMatrixChanged(cropTexMatrix)
    }

    /**
     * 设置裁剪矩阵变化监听器
     */
    fun setCropMatrixChangeListener(listener: CropMatrixChangeListener?) {
        cropMatrixChangeListener = listener
    }

    /**
     * 设置ViewModel引用，用于在OpenGL上下文重建时重新加载纹理
     */
    fun setViewModel(viewModel: EditorViewModel) {
        this.viewModel = viewModel
    }

    fun setCropTexMatrix(matrix: FloatArray) {
        System.arraycopy(matrix, 0, cropTexMatrix, 0, matrix.size)
    }

    /**
     * 根据显示模式计算显示区域
     */
    private fun updateDisplayRect() {
        val viewAspectRatio = 1.0f // 因为是正方形

        when (displayMode) {
            DisplayMode.FIT_CENTER -> {
                // 保持比例，居中适配
                if (imageAspectRatio > viewAspectRatio) {
                    // 图片更宽，按宽度适配
                    val scale = 1.0f / imageAspectRatio
                    val offsetY = (1.0f - scale) / 2.0f
                    displayRect.set(0f, offsetY, 1f, offsetY + scale)
                } else {
                    // 图片更高，按高度适配
                    val scale = imageAspectRatio
                    val offsetX = (1.0f - scale) / 2.0f
                    displayRect.set(offsetX, 0f, offsetX + scale, 1.0f)
                }
            }
            DisplayMode.CROP_FILL -> {
                // 填满整个视图（可能变形）
                displayRect.set(0f, 0f, 1f, 1f)
            }
            DisplayMode.ORIGINAL -> {
                // 原始比例，不缩放（从左上角开始）
                displayRect.set(0f, 0f, 
                    min(1.0f, imageAspectRatio), 
                    min(1.0f, 1.0f / imageAspectRatio))
            }
        }

        // 更新顶点数据
        updateVertexData()
    }

    /**
     * 更新顶点数据
     */
    private fun updateVertexData() {
        // 根据displayRect更新vertexData
        vertexData[0] = displayRect.left
        vertexData[1] = displayRect.top
        vertexData[2] = displayRect.left
        vertexData[3] = displayRect.bottom
        vertexData[4] = displayRect.right
        vertexData[5] = displayRect.top
        vertexData[6] = displayRect.right
        vertexData[7] = displayRect.bottom

        // 更新VBO
        updateVertexBuffer()
    }

    /**
     * 更新VBO
     */
    private fun updateVertexBuffer() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexVboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexData.size * Float.SIZE_BYTES,
            FloatBuffer.wrap(vertexData),
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    /**
     * 设置显示模式
     */
    fun setDisplayMode(mode: DisplayMode) {
        displayMode = mode
        updateDisplayRect()
        (context as EditorActivity).getGLSurfaceView().requestRender()
    }

    /**
     * 更新图片尺寸和宽高比
     */
    fun updateImageDimensions(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
        imageAspectRatio = width.toFloat() / height.toFloat()
        Log.d(tag, "更新图片尺寸: ${width}x${height}, 宽高比: $imageAspectRatio")

        // 根据新的宽高比更新显示区域
        updateDisplayRect()

        // 请求渲染
        (context as EditorActivity).getGLSurfaceView().requestRender()
    }

    private fun createProgram() {
        Log.d(tag, "开始创建着色器程序")

        val vertexShaderCode = ShaderHelper.readShaderFileFromResource(R.raw.editor_vertex_shader, context)
        val fragmentShaderCode = ShaderHelper.readShaderFileFromResource(R.raw.editor_fragment_shader, context)

        if (vertexShaderCode == null || fragmentShaderCode == null) {
            Log.e(tag, "无法读取着色器代码")
            return
        }

        val vertexShaderId = ShaderHelper.compileVertexShader(vertexShaderCode)
        if (vertexShaderId == 0) {
            Log.e(tag, "顶点着色器编译失败")
            return
        }

        val fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentShaderCode)
        if (fragmentShaderId == 0) {
            Log.e(tag, "片元着色器编译失败")
            return
        }

        program = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId)
        if (program == 0) {
            Log.e(tag, "着色器程序链接失败")
            return
        }

        GLES20.glUseProgram(program)
        Log.d(tag, "着色器程序创建并使用成功")
    }

    private fun findShaderLocation(){
        aPositionLoc = GLES20.glGetAttribLocation(program, "a_position")
        aTexCoordLoc = GLES20.glGetAttribLocation(program, "a_tex_coord")
        uMatrixLoc = GLES20.glGetUniformLocation(program, "u_matrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(program, "u_tex_matrix")
        uIsCroppingLoc = GLES20.glGetUniformLocation(program, "u_is_cropping")

        uTextureLoc = GLES20.glGetUniformLocation(program, "u_texture")
        uGrayScaleLoc = GLES20.glGetUniformLocation(program, "u_gray_scale")
        uContrastLoc = GLES20.glGetUniformLocation(program, "u_contrast")
        uSaturationLoc = GLES20.glGetUniformLocation(program, "u_saturation")
    }

    private fun initVertexBuffer() {
        // 申请vertex Vbo

        val vertexVbo = IntArray(1)
        // 申请Vbo Id
        GLES20.glGenBuffers(1, vertexVbo, 0)
        vertexVboId = vertexVbo[0]
        // 绑定Vbo
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexVboId)
        // 将vertexData 传递给VBO
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            vertexData.size * Float.SIZE_BYTES,
            FloatBuffer.wrap(vertexData),
            GLES20.GL_STATIC_DRAW
        )
        // 设置顶点属性指针
        GLES20.glVertexAttribPointer(
            aPositionLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * Float.SIZE_BYTES,
            0
        )
        // 激活顶点属性
        GLES20.glEnableVertexAttribArray(aPositionLoc)

        // 申请 texture Vbo
        val textureVbo = IntArray(1)
        GLES20.glGenBuffers(1, textureVbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureVbo[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            textureData.size * Float.SIZE_BYTES,
            FloatBuffer.wrap(textureData),
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glVertexAttribPointer(
            aTexCoordLoc,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * Float.SIZE_BYTES,
            0
        )
        GLES20.glEnableVertexAttribArray(aTexCoordLoc)

        // 解绑VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    fun setBitmap(bitmap: Bitmap) {
        if (textureId != -1) {
            TextureHelp.deleteTexture(textureId)
        }

        imageWidth = bitmap.width
        imageHeight = bitmap.height

        // 计算图片宽高比
        imageAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        Log.d(tag, "图片宽高比: $imageAspectRatio")

        // 根据显示模式计算显示区域
        updateDisplayRect()

        textureId = TextureHelp.loadTexture(context, bitmap)
        Log.d(tag, "textureId: $textureId")

        (context as EditorActivity).getGLSurfaceView().requestRender()
    }

    fun release() {
        TextureHelp.deleteTexture(textureId)
        if (vertexVboId != -1) {
            GLES20.glDeleteBuffers(1, intArrayOf(vertexVboId), 0)
            vertexVboId = -1
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
        }
    }

    fun getFullBitmap(viewWidth: Int, viewHeight: Int) : Bitmap? {
        if (textureId == -1) return null

        // 创建帧缓冲对象
        val fboIds = IntArray(1)
        GLES20.glGenFramebuffers(1, fboIds, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboIds[0])

        // 创建纹理作为FBO的颜色附件
        val texId = IntArray(1)
        GLES20.glGenTextures(1, texId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId[0])

        // 设置纹理尺寸和参数
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            viewWidth, viewHeight, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )

        // 将纹理附加到帧缓冲
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, texId[0], 0
        )

        // 设置视口并渲染
        GLES20.glViewport(0, 0, viewWidth, viewHeight)
        onDrawFrame(null)

        // 创建位图并读取像素数据
        val bitmap = createBitmap(viewWidth, viewHeight)
        val buffer = ByteBuffer.allocateDirect(viewWidth * viewHeight * 4)
        GLES20.glReadPixels(
            0, 0, viewWidth, viewHeight,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // 清理资源
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glDeleteFramebuffers(1, fboIds, 0)
        GLES20.glDeleteTextures(1, texId, 0)

        // 翻转图像（OpenGL的Y轴与Android的Y轴相反）
        val flipMatrix = android.graphics.Matrix().apply { postScale(1f, -1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, viewWidth, viewHeight, flipMatrix, true)
    }
}
