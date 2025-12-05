package com.example.simpleeditingpictureapp.model

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer

/**
 * 图片编辑器数据模型
 * 负责管理图片编辑的状态和数据
 */
class ImageEditorModel {
    private val tag = "ImageEditorModel"

    // 图片URI
    var imageUri: Uri? = null

    // 编辑状态，用来切换UI
    var isCropping = false
    var isFiltering = false

    // 裁剪框相关
    var previewCropRect: RectF = RectF(0f, 0f, 1f, 1f)

    // 滤镜默认值
    var originalUseGrayscale = false
    var originalContrastValue = 1.0f
    var originalSaturationValue = 1.0f
    var useGrayscale = false
    var contrastValue = 1.0f
    var saturationValue = 1.0f

    /**
     * 重置裁剪框
     */
    fun resetCropRect() {
        previewCropRect.set(0f, 0f, 1f, 1f)
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
     * 应用裁剪到渲染器
     */
    fun applyCropToRenderer(renderer: EditorRenderer, apply: Boolean) {
        if (apply) {
            renderer.setCropMode(true)
            renderer.setCropRect(previewCropRect)
            Log.d(tag, "applying $previewCropRect")
        } else {
            renderer.setCropMode(false)
            renderer.setCropRect(RectF(0f, 0f, 1f, 1f))
            Log.d(tag, "reverting to original")
        }
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
