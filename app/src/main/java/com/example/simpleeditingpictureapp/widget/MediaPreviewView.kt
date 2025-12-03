//package com.example.simpleeditingpictureapp.widget
//
//import android.content.Context
//import android.util.AttributeSet
//import android.util.Log
//import androidx.appcompat.widget.AppCompatImageView
//import com.bumptech.glide.Glide
//import com.bumptech.glide.load.DataSource
//import com.bumptech.glide.load.engine.GlideException
//import com.bumptech.glide.load.resource.gif.GifDrawable
//import com.bumptech.glide.request.RequestListener
//import com.bumptech.glide.request.target.Target
//import com.example.simpleeditingpictureapp.R
//
//class MediaPreviewView @JvmOverloads constructor(
//    context: Context,
//    attrs: AttributeSet? = null,
//    defStyleAttr: Int = 0
//) : AppCompatImageView(context, attrs, defStyleAttr) {
//
//    private var isPlaying = true
//    private var isGif = false
//
//    init {
//        scaleType = ScaleType.CENTER_CROP
//    }
//
//    fun loadImageResource(imageResourceId: Int) {
//        isGif = false
//        Log.d("MediaPreviewView", "加载图片资源: $imageResourceId")
//        Glide.with(context)
//            .load(imageResourceId)
//            .into(this)
//    }
//
//    fun loadGifResource(gifResourceId: Int) {
//        Log.d("MediaPreviewView", "开始加载GIF资源: $gifResourceId")
//        isGif = true
//
//        Glide.with(context)
//            .asGif()
//            .load(gifResourceId)
//            .listener(object : RequestListener<GifDrawable> {
//                override fun onLoadFailed(
//                    e: GlideException?,
//                    model: Any?,
//                    target: Target<GifDrawable>,
//                    isFirstResource: Boolean
//                ): Boolean {
//                    Log.e("MediaPreviewView", "GIF加载失败: ${e?.message}")
//                    e?.logRootCauses("MediaPreviewView")
//                    // 加载失败时显示默认图片
//                    loadImageResource(R.drawable.img1)
//                    return false
//                }
//
//                override fun onResourceReady(
//                    resource: GifDrawable,
//                    model: Any,
//                    target: Target<GifDrawable>,
//                    dataSource: DataSource,
//                    isFirstResource: Boolean
//                ): Boolean {
//                    Log.d("MediaPreviewView", "GIF加载成功，尺寸: ${resource.intrinsicWidth}x${resource.intrinsicHeight}")
//                    return false
//                }
//            })
//            .into(this)
//        isPlaying = true
//    }
//
//    fun pauseGif() {
//        if (isGif && isPlaying) {
//            Log.d("MediaPreviewView", "暂停GIF播放")
//            Glide.with(context).pauseRequests()
//            isPlaying = false
//        }
//    }
//
//    fun resumeGif() {
//        if (isGif && !isPlaying) {
//            Log.d("MediaPreviewView", "恢复GIF播放")
//            Glide.with(context).resumeRequests()
//            isPlaying = true
//        }
//    }
//
//    fun clearMedia() {
//        Log.d("MediaPreviewView", "清除媒体内容")
//        Glide.with(context).clear(this)
//        isPlaying = false
//        isGif = false
//    }
//
//    fun isGifPlaying(): Boolean {
//        return isGif && isPlaying
//    }
//
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        clearMedia()
//    }
//}