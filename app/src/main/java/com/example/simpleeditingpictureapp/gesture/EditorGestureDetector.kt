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
): ScaleGestureDetector.SimpleOnScaleGestureListener (){
    private var scaleFactor = 1.0f
    private var minScaleFactor = 0.2f
    private var maxScaleFactor = 5.0f
    private var mFocusX = 0.0f
    private var mFocusY = 0.0f

    // 平移相关变量
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    var isPanningEnabled = true

    // 处理触摸事件
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPanningEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isDragging = true
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // 只有在单指触摸时才进行平移
                    if (event.pointerCount == 1) {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY

                        glSurfaceView.queueEvent {

                            editorRenderer.applyPan(dx, dy)
                        }

                        lastTouchX = event.x
                        lastTouchY = event.y
                        glSurfaceView.requestRender()
                    }
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        // 如果平移/缩放功能被禁用，则直接返回，不处理手势
        if (!isPanningEnabled) {
            return false
        }

        scaleFactor = max(minScaleFactor, min(maxScaleFactor, scaleFactor * detector.scaleFactor))
        mFocusX = detector.focusX
        mFocusY = detector.focusY

        glSurfaceView.queueEvent {
            editorRenderer.applyScaling(scaleFactor, mFocusX, mFocusY)
        }

        glSurfaceView.requestRender()
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return isPanningEnabled
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        super.onScaleEnd(detector)
    }
}