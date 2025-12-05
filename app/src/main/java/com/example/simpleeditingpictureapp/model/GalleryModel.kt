package com.example.simpleeditingpictureapp.model

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.simpleeditingpictureapp.recyclerview.bean.ImageBean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图库数据模型
 * 负责从设备加载图片数据
 */
class GalleryModel {
    private val tag = "GalleryModel"

    /**
     * 从设备加载图片列表
     * @param context 应用上下文
     * @return 图片列表
     */
    suspend fun loadImageList(context: Context): List<ImageBean> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<ImageBean>()
        Log.d(tag, "开始加载图片列表")

        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用新的API
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE
                )
            } else {
                // Android 9及以下使用旧的API
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA
                )
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val cursor: Cursor? = context.contentResolver.query(
                uri, projection, null, null, sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                var count = 0

                Log.d(tag, "找到图片数量: ${c.count}")

                while (c.moveToNext()) {
                    try {
                        val id = c.getLong(idColumn)
                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        Log.d(tag, "加载图片: $contentUri")

                        val imageBean = ImageBean()
                        imageBean.imageUri = contentUri
                        imageList.add(imageBean)

                        count++
                    } catch (e: Exception) {
                        Log.e(tag, "加载图片出错", e)
                    }
                }

                Log.d(tag, "成功加载 $count 张图片")
            }
        } catch (e: Exception) {
            Log.e(tag, "加载图片列表出错", e)
        }

        return@withContext imageList
    }
}
