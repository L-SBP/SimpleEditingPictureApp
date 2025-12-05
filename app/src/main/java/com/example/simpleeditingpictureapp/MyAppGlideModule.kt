package com.example.simpleeditingpictureapp

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import java.util.concurrent.TimeUnit

@GlideModule
class MyAppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        super.applyOptions(context, builder)

        // 设置默认请求选项
        val requestOptions = RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .signature(ObjectKey(System.currentTimeMillis().toShort()))

        builder.setDefaultRequestOptions(requestOptions)
    }

    // 禁用清单解析以提升性能
    override fun isManifestParsingEnabled(): Boolean = false
}