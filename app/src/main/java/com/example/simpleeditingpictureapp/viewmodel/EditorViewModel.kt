package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.simpleeditingpictureapp.activity.MainActivity
import com.example.simpleeditingpictureapp.model.ImageEditorModel
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 编辑器ViewModel
 * 负责处理编辑器的业务逻辑和UI状态
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "EditorViewModel"
    private val context = application.applicationContext

    // 数据模型
    private val model = ImageEditorModel()

    // 渲染器
    private var renderer: EditorRenderer? = null

    // UI状态LiveData
    private val _uiState = MutableLiveData<EditorUiState>()
    val uiState: LiveData<EditorUiState> = _uiState

    // 滤镜值LiveData
    private val _filterValues = MutableLiveData<ImageEditorModel.FilterValues>()
    val filterValues: LiveData<ImageEditorModel.FilterValues> = _filterValues

    // 裁剪框LiveData
    private val _cropRect = MutableLiveData<RectF>()
    val cropRect: LiveData<RectF> = _cropRect

    // 消息LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // 保存结果LiveData
    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    init {
        // 初始化UI状态
        _uiState.value = EditorUiState()

        // 初始化滤镜值
        _filterValues.value = model.getCurrentFilterValues()
    }

    /**
     * 设置图片URI
     */
    fun setImageUri(uri: Uri?) {
        model.imageUri = uri
    }

    /**
     * 设置渲染器
     */
    fun setRenderer(renderer: EditorRenderer) {
        this.renderer = renderer
    }

    /**
     * 进入裁剪模式
     */
    fun enterCropMode() {
        model.isCropping = true
        model.isFiltering = false
        model.resetCropRect()

        _cropRect.value = model.previewCropRect
        _uiState.value = _uiState.value?.copy(
            isEditing = true,
            isCropping = true,
            isFiltering = false,
            showFilterControls = false
        )

        renderer?.setCropMode(false)
    }

    /**
     * 退出裁剪模式
     */
    fun exitCropMode(applyChanges: Boolean) {
        model.isCropping = false

        model.applyCropToRenderer(renderer ?: return, applyChanges)

        _uiState.value = _uiState.value?.copy(
            isEditing = false,
            isCropping = false,
            isFiltering = false,
            showFilterControls = false
        )
    }

    /**
     * 进入滤镜模式
     */
    fun enterFilterMode() {
        model.isFiltering = true
        model.isCropping = false

        // 保存原始滤镜值
        model.saveOriginalFilterValues()

        // 更新滤镜值LiveData
        _filterValues.value = model.getCurrentFilterValues()

        _uiState.value = _uiState.value?.copy(
            isEditing = true,
            isCropping = false,
            isFiltering = true,
            showFilterControls = true
        )
    }

    /**
     * 退出滤镜模式
     */
    fun exitFilterMode(applyChanges: Boolean) {
        model.isFiltering = false

        if (!applyChanges) {
            // 恢复原始滤镜值
            val originalValues = model.restoreOriginalFilterValues()
            model.updateFilterValues(originalValues)
            _filterValues.value = originalValues

            // 应用到渲染器
            model.applyFiltersToRenderer(renderer ?: return)
        }

        _uiState.value = _uiState.value?.copy(
            isEditing = false,
            isCropping = false,
            isFiltering = false,
            showFilterControls = false
        )
    }

    /**
     * 更新裁剪框
     */
    fun updateCropRect(rect: RectF) {
        if (model.isCropping) {
            model.previewCropRect.set(rect)
            _cropRect.value = rect
        }
    }

    /**
     * 更新滤镜
     */
    fun updateFilter(values: ImageEditorModel.FilterValues) {
        model.updateFilterValues(values)
        _filterValues.value = values

        // 应用到渲染器
        model.applyFiltersToRenderer(renderer ?: return)
    }

    /**
     * 保存编辑后的图片
     */
    fun saveEditedImage(glSurfaceView: GLSurfaceView) {
        glSurfaceView.queueEvent {
            try {
                // 获取处理后的图像
                val editedBitmap = model.getEditedBitmap(renderer ?: return@queueEvent, 
                    glSurfaceView.width, glSurfaceView.height)

                // 回到主线程保存图像
                if (editedBitmap != null) {
                    saveBitmapToDevice(editedBitmap)
                } else {
                    _message.postValue("保存失败，请重试")
                    _saveResult.postValue(false)
                }
            } catch (e: Exception) {
                Log.e(tag, "保存图片时出错", e)
                _message.postValue("保存失败: ${e.message}")
                _saveResult.postValue(false)
            }
        }
    }

    /**
     * 保存Bitmap到设备
     */
    private fun saveBitmapToDevice(bitmap: Bitmap) {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri?

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Android 10及以上版本使用MediaStore保存
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, 
                        android.os.Environment.DIRECTORY_PICTURES)
                }

                imageUri = context.contentResolver.insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = context.contentResolver.openOutputStream(imageUri!!)
            } else {
                // Android 10以下版本使用传统文件保存方式
                val picturesDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES), 
                    "SimpleEditingPictureApp")
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }

                val imageFile = File(picturesDir, filename)
                fos = FileOutputStream(imageFile)
                imageUri = Uri.fromFile(imageFile)
            }

            // 保存图像
            fos?.use{ outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            // 通知媒体扫描器更新图库
            imageUri?.let { uri ->
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scanIntent.data = uri
                context.sendBroadcast(scanIntent)
            }

            // 显示成功消息
            _message.postValue("图片已保存")
            _saveResult.postValue(true)

        } catch (e: Exception) {
            Log.e(tag, "保存图片到设备时出错", e)
            _message.postValue("保存失败: ${e.message}")
            _saveResult.postValue(false)
        } finally {
            fos?.close()
            bitmap.recycle()
        }
    }

    /**
     * 导航到主页
     */
    fun navigateToMain() {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        context.startActivity(intent)
    }

    fun updateFilterValues(values: ImageEditorModel.FilterValues) {
        _filterValues.value = values
    }

}

/**
 * 编辑器UI状态
 */
data class EditorUiState(
    val isEditing: Boolean = false,
    val isCropping: Boolean = false,
    val isFiltering: Boolean = false,
    val showFilterControls: Boolean = false
)
