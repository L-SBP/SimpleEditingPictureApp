package com.example.simpleeditingpictureapp.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.LinearInterpolator

class CustomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var gradientHeightRatio = 0.2f // 渐变高度占View高度的20%
    
    // 扫光效果相关
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sweepGradient: LinearGradient? = null
    private var sweepAnimator: ValueAnimator? = null
    private var sweepPosition = 0f
    private var sweepEnabled = true
    private var sweepWidth = 100f // 扫光宽度
    private var sweepSpeed = 2000f // 扫光速度（毫秒）

    // 自定义属性
    private var customScaleType = ScaleType.FIT_CENTER // 默认使用 FIT_CENTER

    fun setCustomScaleType(scaleType: ScaleType) {
        this.customScaleType = scaleType
        super.setScaleType(scaleType)
        invalidate()
    }

    override fun setScaleType(scaleType: ScaleType?) {
        scaleType?.let {
            this.customScaleType = it
        }
        super.setScaleType(scaleType)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        // 先绘制原始图片
        super.onDraw(canvas)

        // 只在图片已经绘制完成的情况下绘制效果
        if (drawable == null) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        // 绘制底部渐变遮罩
        if (gradientHeightRatio > 0) {
            val gradientHeight = viewHeight * gradientHeightRatio

            // 创建一个图层来应用渐变遮罩
            val saved = canvas.saveLayer(0f, 0f, viewWidth, viewHeight, null, Canvas.ALL_SAVE_FLAG)

            // 先绘制原始图片
            super.onDraw(canvas)

            // 绘制渐变
            val linearGradient = LinearGradient(
                0f, viewHeight - gradientHeight,
                0f, viewHeight,
                Color.TRANSPARENT,
                Color.argb(200, 0, 0, 0), // 增加不透明度，使渐变更明显
                Shader.TileMode.CLAMP
            )

            gradientPaint.shader = linearGradient
            // 使用不同的混合模式，使渐变更明显
            gradientPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)

            canvas.drawRect(0f, viewHeight - gradientHeight, viewWidth, viewHeight, gradientPaint)

            canvas.restoreToCount(saved)

            // 清理画笔
            gradientPaint.xfermode = null
            gradientPaint.shader = null
        }
        
        // 绘制扫光效果
        if (sweepEnabled) {
            drawSweepEffect(canvas, viewWidth, viewHeight)
        }
    }
    
    private fun drawSweepEffect(canvas: Canvas, viewWidth: Float, viewHeight: Float) {
        // 初始化扫光渐变
        if (sweepGradient == null) {
            sweepGradient = LinearGradient(
                -sweepWidth, 0f,
                sweepWidth, 0f,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb(150, 255, 255, 255), // 增加不透明度，使扫光更明显
                    Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
            sweepPaint.shader = sweepGradient
            // 使用不同的混合模式
            sweepPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN) // SCREEN模式会使扫光更明显
            
            // 启动扫光动画
            startSweepAnimation(viewWidth)
        }
        
        // 创建一个图层来应用扫光效果
        val saved = canvas.saveLayer(0f, 0f, viewWidth, viewHeight, null, Canvas.ALL_SAVE_FLAG)
        
        // 先绘制原始图片
        super.onDraw(canvas)
        
        // 绘制扫光
        canvas.save()
        canvas.translate(sweepPosition, 0f)
        canvas.drawRect(0f, 0f, sweepWidth * 2, viewHeight, sweepPaint)
        canvas.restore()
        
        canvas.restoreToCount(saved)
    }
    
    private fun startSweepAnimation(viewWidth: Float) {
        if (sweepAnimator != null) return
        
        sweepAnimator = ValueAnimator.ofFloat(-sweepWidth * 2, viewWidth + sweepWidth * 2).apply {
            duration = sweepSpeed.toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                sweepPosition = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setGradientHeightRatio(ratio: Float) {
        gradientHeightRatio = ratio.coerceIn(0f, 1f)
        invalidate()
    }
    
    fun setSweepEnabled(enabled: Boolean) {
        sweepEnabled = enabled
        if (!enabled && sweepAnimator != null) {
            sweepAnimator?.cancel()
            sweepAnimator = null
        } else if (enabled && sweepAnimator == null) {
            invalidate() // 触发重绘，会重新初始化扫光动画
        }
    }
    
    fun setSweepWidth(width: Float) {
        sweepWidth = width
        sweepGradient = null // 重置渐变，下次绘制时会重新创建
        invalidate()
    }
    
    fun setSweepSpeed(speed: Float) {
        sweepSpeed = speed
        if (sweepAnimator != null) {
            sweepAnimator?.cancel()
            sweepAnimator = null
            invalidate() // 触发重绘，会重新创建动画
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理动画资源
        sweepAnimator?.cancel()
        sweepAnimator = null
    }
}