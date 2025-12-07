package com.example.simpleeditingpictureapp.opengl_es

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.max
import kotlin.math.min

fun interface OnCropRectChangeListener {
    fun onCropRectChanged(rect: RectF)
}

class CropFrameGLSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {
    private val cropFrameRenderer = CropFrameRenderer(context)
    private var cropRectChangeListener: OnCropRectChangeListener? = null

    // 裁剪框参数(px)
    private var cropLeft = 0f
    private var cropTop = 0f
    private var cropRight = 0f
    private var cropBottom = 0f

    // 最小裁剪框尺寸(px)
    private val minCropSize = 100f

    // 四个角的控制点尺寸(px)
    private val controlSize = 40f

    // 拖拽状态
    private enum class DragState {
        NONE,
        LEFT,
        TOP,
        RIGHT,
        BOTTOM,
        LEFT_TOP,
        RIGHT_TOP,
        LEFT_BOTTOM,
        RIGHT_BOTTOM
    }

    private var dragState = DragState.NONE

    // 裁剪比例锁定
    private var aspectRatioLock: Float? = null

    // 预设比例
    enum class AspectRatio(val displayName: String, val ratio: Float?) {
        FREE("自由", null),
        SQUARE("1:1", 1.0f),
        RATIO_3_4("3:4", 3f/4f),
        RATIO_4_3("4:3", 4f/3f),
        RATIO_9_16("9:16", 9f/16f),
        RATIO_16_9("16:9", 16f/9f)
    }

    private var currentAspectRatio = AspectRatio.FREE

    init {
        // 配置透明背景
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)   //
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSPARENT)

        setEGLContextClientVersion(2)
        setRenderer(cropFrameRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetCropFrameRect()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragState = getDragState(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragState != DragState.NONE) {
                    updateCropFrameRect(event.x, event.y)
                    updateRenderCropFrameRect()
                    cropRectChangeListener?.onCropRectChanged(getNormalizedCropFrameRect())
                    // 确保请求渲染，即使是在快速移动时
                    requestRender()
                }
            }

            MotionEvent.ACTION_UP -> {
                dragState = DragState.NONE
            }
        }
        return true
    }

    private fun updateCropFrameRect(x: Float, y: Float) {
        var newLeft = cropLeft
        var newTop = cropTop
        var newRight = cropRight
        var newBottom = cropBottom

        when(dragState) {
            DragState.LEFT -> newLeft = x
            DragState.RIGHT -> newRight = x
            DragState.TOP -> newTop = y
            DragState.BOTTOM -> newBottom = y
            DragState.LEFT_TOP -> {
                newLeft = x
                newTop = y
            }
            DragState.RIGHT_TOP -> {
                newRight = x
                newTop = y
            }
            DragState.LEFT_BOTTOM -> {
                newLeft = x
                newBottom = y
            }
            DragState.RIGHT_BOTTOM -> {
                newRight = x
                newBottom = y
            }
            else -> return
        }
        newLeft = max(0f, newLeft)
        newRight = min(width.toFloat(), newRight)
        newTop = max(0f, newTop)                    // 现在是触摸的坐标，以左上角为原点，之后才进行y轴反转
        newBottom = min(height.toFloat(), newBottom)

        // 如果有锁定比例，调整裁剪框以保持比例
        aspectRatioLock?.let { ratio ->
            when(dragState) {
                DragState.LEFT, DragState.RIGHT -> {
                    // 调整高度以保持比例
                    val newWidth = newRight - newLeft
                    val newHeight = newWidth / ratio
                    val centerY = (cropTop + cropBottom) / 2
                    newTop = centerY - newHeight / 2
                    newBottom = centerY + newHeight / 2
                }
                DragState.TOP, DragState.BOTTOM -> {
                    // 调整宽度以保持比例
                    val newHeight = newBottom - newTop
                    val newWidth = newHeight * ratio
                    val centerX = (cropLeft + cropRight) / 2
                    newLeft = centerX - newWidth / 2
                    newRight = centerX + newWidth / 2
                }
                DragState.LEFT_TOP -> {
                    // 根据拖动方向调整
                    val newWidth = newRight - newLeft
                    val newHeight = newWidth / ratio
                    newBottom = cropBottom
                    newTop = newBottom - newHeight
                }
                DragState.RIGHT_TOP -> {
                    // 根据拖动方向调整
                    val newWidth = newRight - newLeft
                    val newHeight = newWidth / ratio
                    newBottom = cropBottom
                    newTop = newBottom - newHeight
                }
                DragState.LEFT_BOTTOM -> {
                    // 根据拖动方向调整
                    val newWidth = newRight - newLeft
                    val newHeight = newWidth / ratio
                    newTop = cropTop
                    newBottom = newTop + newHeight
                }
                DragState.RIGHT_BOTTOM -> {
                    // 根据拖动方向调整
                    val newWidth = newRight - newLeft
                    val newHeight = newWidth / ratio
                    newTop = cropTop
                    newBottom = newTop + newHeight
                }
                else -> {}
            }

            // 确保不超出边界
            if (newLeft < 0) {
                newLeft = 0f
                newRight = (newBottom - newTop) * ratio
            }
            if (newRight > width) {
                newRight = width.toFloat()
                newLeft = newRight - (newBottom - newTop) * ratio
            }
            if (newTop < 0) {
                newTop = 0f
                newBottom = (newRight - newLeft) / ratio
            }
            if (newBottom > height) {
                newBottom = height.toFloat()
                newTop = newBottom - (newRight - newLeft) / ratio
            }
        }

        // 宽高最小限制
        if (newRight - newLeft < minCropSize) {
            newLeft = if (dragState in listOf(DragState.LEFT, DragState.LEFT_TOP, DragState.LEFT_BOTTOM)) {
                newRight - minCropSize
            } else {
                newLeft
            }
            newRight = newLeft + minCropSize

            // 如果有比例锁定，调整高度
            aspectRatioLock?.let { ratio ->
                val newHeight = minCropSize / ratio
                val centerY = (newTop + newBottom) / 2
                newTop = centerY - newHeight / 2
                newBottom = centerY + newHeight / 2
            }
        }
        if (newBottom - newTop < minCropSize) {
            newTop = if (dragState in listOf(DragState.TOP, DragState.LEFT_TOP, DragState.RIGHT_TOP)) {
                newBottom - minCropSize
            } else {
                newTop
            }
            newBottom = newTop + minCropSize

            // 如果有比例锁定，调整宽度
            aspectRatioLock?.let { ratio ->
                val newWidth = minCropSize * ratio
                val centerX = (newLeft + newRight) / 2
                newLeft = centerX - newWidth / 2
                newRight = centerX + newWidth / 2
            }
        }

        // 更新坐标
        cropLeft = newLeft
        cropTop = newTop
        cropRight = newRight
        cropBottom = newBottom
    }

    private fun getDragState(x: Float, y: Float): DragState {
        val halfControl = controlSize / 2
        val onLeft = x in (cropLeft - halfControl)..(cropLeft + halfControl)
        val onRight = x in (cropRight - halfControl)..(cropRight + halfControl)
        val onTop = y in (cropTop - halfControl)..(cropTop + halfControl)
        val onBottom = y in (cropBottom - halfControl)..(cropBottom + halfControl)

        return when {
            onLeft && onTop -> DragState.LEFT_TOP
            onRight && onTop -> DragState.RIGHT_TOP
            onLeft && onBottom -> DragState.LEFT_BOTTOM
            onRight && onBottom -> DragState.RIGHT_BOTTOM
            onLeft -> DragState.LEFT
            onRight -> DragState.RIGHT
            onTop -> DragState.TOP
            onBottom -> DragState.BOTTOM
            else -> DragState.NONE
        }
    }

    fun setOnCropRectChangeListener(listener: OnCropRectChangeListener?) {
        cropRectChangeListener = listener
    }

    fun resetCropFrameRect() {
        val padding = min(width, height) * 0.1f

        cropLeft = padding
        cropTop = padding
        cropRight = width - padding
        cropBottom = height - padding
        updateRenderCropFrameRect()
        requestRender()
    }

    fun releaseRenderer() {
        queueEvent {
            cropFrameRenderer.release()
        }
    }


    private fun updateRenderCropFrameRect() {
        queueEvent {
            val normalizedRect = getNormalizedCropFrameRect()
            cropFrameRenderer.updateCropFrameRect(normalizedRect)
            // 确保在更新后立即请求渲染
            requestRender()
        }
    }

    private fun getNormalizedCropFrameRect(): RectF {
        val rect = RectF()
        rect.left = cropLeft / width
        rect.top = cropTop / height
        rect.right = cropRight / width
        rect.bottom = cropBottom / height
        return rect
    }

    /**
     * 设置裁剪比例
     * @param aspectRatio 裁剪比例枚举
     */
    fun setAspectRatio(aspectRatio: AspectRatio) {
        currentAspectRatio = aspectRatio
        aspectRatioLock = aspectRatio.ratio

        // 如果设置了固定比例，调整当前裁剪框
        aspectRatioLock?.let { ratio ->
            val centerX = (cropLeft + cropRight) / 2
            val centerY = (cropTop + cropBottom) / 2

            // 计算当前裁剪框的宽高
            val currentWidth = cropRight - cropLeft
            val currentHeight = cropBottom - cropTop

            // 根据比例调整尺寸，保持中心点不变
            if (currentWidth / currentHeight > ratio) {
                // 当前裁剪框太宽，以高度为基准调整宽度
                val newWidth = currentHeight * ratio
                cropLeft = centerX - newWidth / 2
                cropRight = centerX + newWidth / 2
            } else {
                // 当前裁剪框太高，以宽度为基准调整高度
                val newHeight = currentWidth / ratio
                cropTop = centerY - newHeight / 2
                cropBottom = centerY + newHeight / 2
            }

            // 确保不超出边界
            if (cropLeft < 0) {
                cropLeft = 0f
                cropRight = (cropBottom - cropTop) * ratio
            }
            if (cropRight > width) {
                cropRight = width.toFloat()
                cropLeft = cropRight - (cropBottom - cropTop) * ratio
            }
            if (cropTop < 0) {
                cropTop = 0f
                cropBottom = (cropRight - cropLeft) / ratio
            }
            if (cropBottom > height) {
                cropBottom = height.toFloat()
                cropTop = cropBottom - (cropRight - cropLeft) / ratio
            }
        }

        updateRenderCropFrameRect()
        cropRectChangeListener?.onCropRectChanged(getNormalizedCropFrameRect())
        requestRender()
    }

    /**
     * 获取当前裁剪比例
     */
    fun getCurrentAspectRatio(): AspectRatio {
        return currentAspectRatio
    }

}
