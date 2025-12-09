package com.example.simpleeditingpictureapp.gesture

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import kotlin.math.max
import kotlin.math.min

class EditorGestureDetector(
    private val editorRenderer: EditorRenderer,
    private val glSurfaceView: GLSurfaceView
): ScaleGestureDetector.SimpleOnScaleGestureListener () {
    private var scaleFactor = 1.0f
    private var minScaleFactor = 0.5f
    private var maxScaleFactor = 10.0f
    private var mFocusX = 0.0f
    private var mFocusY = 0.0f

    // 平移相关变量
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var isScaling = false

    var isPanningEnabled = true

    // 处理触摸事件
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPanningEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 只有在没有缩放时才处理单指按下
                if (!isScaling && event.pointerCount == 1) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    isDragging = true
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !isScaling && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    glSurfaceView.queueEvent {
                        editorRenderer.applyPan(dx, dy)
                    }

                    lastTouchX = event.x
                    lastTouchY = event.y
                    glSurfaceView.requestRender()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (event.pointerCount == 1) {
                    isDragging = false
                    return true
                }
            }
        }
        return false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // 如果平移/缩放功能被禁用，则直接返回，不处理手势
        if (!isPanningEnabled) {
            return false
        }

        isScaling = true

        // 使用更平滑的缩放因子计算
        val newScaleFactor = scaleFactor * detector.scaleFactor
        scaleFactor = max(minScaleFactor, min(maxScaleFactor, newScaleFactor))

        // 使用缩放中心作为焦点
        mFocusX = detector.focusX
        mFocusY = detector.focusY

        glSurfaceView.queueEvent {
            editorRenderer.applyScaling(scaleFactor, mFocusX, mFocusY)
        }

        glSurfaceView.requestRender()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        isScaling = true
        // 当开始缩放时，停止当前拖拽
        isDragging = false
        return isPanningEnabled
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        isScaling = false

        // 可选：重置缩放因子到合理范围，避免抖动
        if (scaleFactor < minScaleFactor + 0.1f) {
            scaleFactor = minScaleFactor
        } else if (scaleFactor > maxScaleFactor - 0.1f) {
            scaleFactor = maxScaleFactor
        }

        super.onScaleEnd(detector)
    }

    /**
     * 重置手势状态
     */
    fun reset() {
        isScaling = false
        isDragging = false
    }
}