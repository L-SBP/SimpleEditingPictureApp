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

        // 宽高最小限制
        if (newRight - newLeft < minCropSize) {
            newLeft = if (dragState in listOf(DragState.LEFT, DragState.LEFT_TOP, DragState.LEFT_BOTTOM)) {
                newRight - minCropSize
            } else {
                newLeft
            }
            newRight = newLeft + minCropSize
        }
        if (newBottom - newTop < minCropSize) {
            newTop = if (dragState in listOf(DragState.TOP, DragState.LEFT_TOP, DragState.RIGHT_TOP)) {
                newBottom - minCropSize
            } else {
                newTop
            }
            newBottom = newTop + minCropSize
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

}
