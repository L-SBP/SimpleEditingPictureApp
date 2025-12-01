package com.example.simpleeditingpictureapp.gesture

import android.opengl.GLSurfaceView
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

    var isPanningEnabled = true

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