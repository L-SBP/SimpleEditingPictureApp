package com.example.simpleeditingpictureapp.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlin.math.max

/**
 * 图片编辑器数据模型
 * 负责管理图片编辑的状态和数据
 */
class ImageEditorModel {
    private val tag = "ImageEditorModel"

    // 图片URI
    var imageUri: Uri? = null

    // 图片Bitmap
    var originalBitmap: Bitmap? = null

    // 图片尺寸
    var originalImageWidth = 0
    var originalImageHeight = 0
    var currentCropWidth = 0
    var currentCropHeight = 0

    // 编辑状态，用来切换UI
    var isCropping = false
    var isFiltering = false

    // 裁剪框相关
    var cropFrameRect: RectF = RectF(0f, 0f, 1f, 1f)

    // 裁剪纹理矩阵
    private var cropTexMatrix: FloatArray = FloatArray(16)

    // 滤镜默认值
    var originalUseGrayscale = false
    var originalContrastValue = 1.0f
    var originalSaturationValue = 1.0f
    var useGrayscale = false
    var contrastValue = 1.0f
    var saturationValue = 1.0f

    /**
     * 通过URI从设备加载Bitmap
     */
    suspend fun loadBitmapFromUri(context: Context, uri: Uri) : Bitmap?  = withContext(
        Dispatchers.IO) {
        Log.d(tag, "Loading bitmap from URI: $uri")

        return@withContext suspendCancellableCoroutine { continuation ->

            Glide.with(context)
                .asBitmap()
                .load(uri)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(tag, "加载图片失败: $e")
                        continuation.resumeWith(Result.success(null))
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        originalImageWidth = resource.width
                        originalImageHeight = resource.height
                        originalBitmap = resource

                        currentCropWidth = originalImageWidth
                        currentCropHeight = originalImageHeight

                        Log.d(tag, "图片加载成功")
                        // 创建一个副本，避免资源被回收
                        val bitmapCopy = resource.copy(resource.config ?: Bitmap.Config.ARGB_8888, false)
                        continuation.resumeWith(Result.success(bitmapCopy))
                        return true
                    }
                })
                .submit()
        }
    }

    /**
     * 保存原始滤镜值
     */
    fun saveOriginalFilterValues() {
        originalUseGrayscale = useGrayscale
        originalContrastValue = contrastValue
        originalSaturationValue = saturationValue
    }

    /**
     * 恢复原始滤镜值
     */
    fun restoreOriginalFilterValues(): FilterValues {
        return FilterValues(
            grayscale = originalUseGrayscale,
            contrast = originalContrastValue,
            saturation = originalSaturationValue
        )
    }

    /**
     * 获取当前滤镜值
     */
    fun getCurrentFilterValues(): FilterValues {
        return FilterValues(
            grayscale = useGrayscale,
            contrast = contrastValue,
            saturation = saturationValue
        )
    }

    /**
     * 更新滤镜值
     */
    fun updateFilterValues(values: FilterValues) {
        useGrayscale = values.grayscale
        contrastValue = values.contrast
        saturationValue = values.saturation
    }

    /**
     * 应用滤镜到渲染器
     */
    fun applyFiltersToRenderer(renderer: EditorRenderer) {
        renderer.setGrayscaleEnabled(useGrayscale)
        renderer.setContrast(contrastValue)
        renderer.setSaturation(saturationValue)
    }

    /**
     * 保存裁剪纹理矩阵
     */
    fun setCropTexMatrix(matrix: FloatArray) {
        cropTexMatrix = matrix.copyOf()
    }

    /**
     * 获取裁剪纹理矩阵
     */
    fun getCropTexMatrix(): FloatArray {
        return cropTexMatrix.copyOf()
    }

    fun applyCropTexMatrixToRenderer(renderer: EditorRenderer, cropTexMatrix: FloatArray) {
        renderer.setCropTexMatrix(cropTexMatrix)
    }

    /**
     * 应用裁剪到渲染器
     */
    fun applyCropToRenderer(renderer: EditorRenderer, apply: Boolean) {
        if (apply) {
            renderer.setCropMode(true)
            renderer.setCropRect(cropFrameRect)
            Log.d(tag, "applying $cropFrameRect")

            // 计算裁剪后的图片尺寸
            var cropWidth = (originalImageWidth * cropFrameRect.width()).toInt()
            var cropHeight = (originalImageHeight * cropFrameRect.height()).toInt()

            // 确保裁剪后的图片至少有1像素的高度和宽度
            cropWidth = max(1, cropWidth)
            cropHeight = max(1, cropHeight)

            // 更新图片尺寸
            currentCropWidth = cropWidth
            currentCropHeight = cropHeight

            // 更新渲染器的图片尺寸
            renderer.updateImageDimensions(currentCropWidth, currentCropHeight)

            Log.d(tag, "裁剪后图片尺寸更新为: ${cropWidth}x${cropHeight}")
        } else {
            renderer.setCropMode(false)
            renderer.setCropRect(RectF(0f, 0f, 1f, 1f))
            Log.d(tag, "reverting to original")
        }
    }

    fun applyImageDimensions(renderer: EditorRenderer, imageWidth: Int, imageHeight: Int) {
        renderer.updateImageDimensions(imageWidth, imageHeight)
    }

    /**
     * 从渲染器获取处理后的图片
     */
    fun getEditedBitmap(renderer: EditorRenderer, width: Int, height: Int): Bitmap? {
        return try {
            renderer.getFullBitmap(width, height)
        } catch (e: Exception) {
            Log.e(tag, "获取处理后的图片失败", e)
            null
        }
    }

    /**
     * 滤镜值数据类
     */
    data class FilterValues(
        val grayscale: Boolean,
        val contrast: Float,
        val saturation: Float
    )
}
