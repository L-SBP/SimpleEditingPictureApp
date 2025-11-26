package com.example.simpleeditingpictureapp.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class AnimatedImageView : AppCompatImageView {
    private enum class EffectType {
        LIGHT_SWEEP,
        GRADIENT_CROP
    }

    private var currentEffect = EffectType.LIGHT_SWEEP
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lightSweepShader: LinearGradient? = null
    private var sweepPosition = 0f //0->1
    private val clipPath = Path()
    private var cropProgress = 0f //0->1

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        resetAnim()
    }

    private fun resetAnim() {
        cropProgress = 0f
        initAnim()
    }

    private fun initAnim() {
        val sweepAnim = ValueAnimator.ofFloat(0f, 1.5f)
        sweepAnim.duration = 3000
        sweepAnim.repeatCount = ValueAnimator.INFINITE
        sweepAnim.addUpdateListener { animation ->
            sweepPosition = animation.animatedValue as Float
            invalidate()
        }

        val cropAnim = ValueAnimator.ofFloat(0f, 1f)
        cropAnim.duration = 1500
        cropAnim.addUpdateListener { animation ->
            cropProgress = animation.animatedValue as Float
            invalidate()
        }

        if (currentEffect == EffectType.LIGHT_SWEEP) {
            sweepAnim.start()
        } else {
            cropAnim.start()
        }
    }

    private fun drawLightSweep(canvas: Canvas, width: Int, height: Int) {
        val lightWith = width / 2f
        val lightLeft = sweepPosition * width - lightWith
        val lightRect = RectF(lightLeft, 0f, lightLeft + lightWith, height.toFloat())

        lightSweepShader = LinearGradient(
            lightRect.left, 0f, lightRect.right, 0f,
            intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = lightSweepShader
        paint.alpha = 80

        canvas.drawRect(lightRect, paint)
    }

    private fun setCropPath(width: Int, height: Int) {
        clipPath.reset()

        clipPath.moveTo(0f, 0f)
        clipPath.lineTo(width * cropProgress, 0f)
        clipPath.lineTo(width * cropProgress, height.toFloat())
        clipPath.lineTo(0f, height.toFloat())
        clipPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        val width = width
        val height = height

        when (currentEffect) {
            EffectType.LIGHT_SWEEP -> {
                super.onDraw(canvas)
                drawLightSweep(canvas, width, height)
            }
            EffectType.GRADIENT_CROP -> {
                setCropPath(width, height)
                canvas.clipPath(clipPath)
                super.onDraw(canvas)
            }
        }
    }
}