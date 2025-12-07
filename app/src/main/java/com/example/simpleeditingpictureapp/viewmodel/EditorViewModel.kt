package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.simpleeditingpictureapp.activity.MainActivity
import com.example.simpleeditingpictureapp.model.ImageEditorModel
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * 编辑器ViewModel
 * 负责处理编辑器的业务逻辑和UI状态
 */
class EditorViewModel(application: Application) : AndroidViewModel(application), EditorRenderer.CropMatrixChangeListener {
    private val tag = "EditorViewModel"
    private val context = application.applicationContext

    // 数据模型
    val model = ImageEditorModel()

    // 渲染器
    private var renderer: EditorRenderer? = null

    // UI状态LiveData
    private val _uiState = MutableLiveData<EditorUiState>()
    val uiState: LiveData<EditorUiState> = _uiState

    private val _uri = MutableLiveData<Uri>()
    val uri: LiveData<Uri> = _uri

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> = _bitmap

    // 操作历史记录
    private val _history = MutableLiveData<ConcurrentLinkedDeque<EditorHistory>>(ConcurrentLinkedDeque())
    private val _redoHistory = MutableLiveData<ConcurrentLinkedDeque<EditorHistory>>(ConcurrentLinkedDeque())

    // 滤镜值LiveData
    private val _filterValues = MutableLiveData<ImageEditorModel.FilterValues>()
    val filterValues: LiveData<ImageEditorModel.FilterValues> = _filterValues

    // 裁剪框LiveData
    private val _cropRect = MutableLiveData<RectF>()
    val cropRect: LiveData<RectF> = _cropRect

    // 消息LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // 是否能撤销
    val canUndo: LiveData<Boolean> = _history.map { it?.isNotEmpty() ?: false }

    // 是否能重做
    val canRedo: LiveData<Boolean> = _redoHistory.map { it?.isNotEmpty() ?: false }

    // 保存结果LiveData
    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    // 显示模式
    private val _displayMode = MutableLiveData(EditorRenderer.DisplayMode.FIT_CENTER)
    val displayMode: LiveData<EditorRenderer.DisplayMode> = _displayMode

    init {
        // 初始化UI状态
        _uiState.value = EditorUiState()

        // 初始化滤镜值
        _filterValues.value = model.getCurrentFilterValues()

        // 初始化操作历史记录
        _history.value = ConcurrentLinkedDeque()

        // 初始化重做历史记录
        _redoHistory.value = ConcurrentLinkedDeque()

//        // 保存最开始的
//        saveEditorHistory(EditorHistory(cropRect = model.previewCropRect, cropTexMatrix = model.getCropTexMatrix(), filterValues = model.getCurrentFilterValues()))
    }

    /**
     * 设置图片URI
     */
    fun setImageUri(uri: Uri) {
        model.imageUri = uri
        _uri.value = uri
    }

    /**
     * 设置渲染器
     */
    fun setRenderer(renderer: EditorRenderer) {
        this.renderer = renderer
        // 设置裁剪矩阵变化监听器
        renderer.setCropMatrixChangeListener(this)
    }

    /**
     * 实现CropMatrixChangeListener接口
     * 当裁剪矩阵变化时调用
     */
    override fun onCropMatrixChanged(matrix: FloatArray) {
        // 保存裁剪矩阵到模型中
        model.setCropTexMatrix(matrix)
    }

    /**
     * 进入裁剪模式
     */
    fun enterCropMode() {
        model.isCropping = true
        model.isFiltering = false

        _cropRect.value = model.cropFrameRect
        _uiState.value = _uiState.value?.copy(
            isEditing = true,
            isCropping = true,
            isFiltering = false,
            showFilterControls = false
        )

        // 切换到裁剪显示模式
        _displayMode.value = EditorRenderer.DisplayMode.CROP_FILL
        renderer?.setCropMode(false)
    }

    /**
     * 退出裁剪模式
     */
    fun exitCropMode(applyChanges: Boolean) {
        model.isCropping = false

        model.applyCropToRenderer(renderer ?: return, applyChanges)
        if (applyChanges) {
            saveEditorHistory(cropRect = model.cropFrameRect, cropTexMatrix = model.getCropTexMatrix(), filterValues = model.getCurrentFilterValues())
        }

        _uiState.value = _uiState.value?.copy(
            isEditing = false,
            isCropping = false,
            isFiltering = false,
            showFilterControls = false
        )

        // 恢复到居中适配显示模式
        _displayMode.value = EditorRenderer.DisplayMode.FIT_CENTER
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
        } else {
            saveEditorHistory(cropRect = model.cropFrameRect, cropTexMatrix = model.getCropTexMatrix(), filterValues = model.getCurrentFilterValues())
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
    fun updateCropFrameRect(rect: RectF) {
        if (model.isCropping) {
            model.cropFrameRect.set(rect)
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
     * 保存用户应用到图片的操作
     */
    fun saveEditorHistory(action: EditorHistory) {
        synchronized(_history) {
            _history.value?.addLast(action)
            _history.postValue(_history.value)
        }
        // 清空 redo 历史
        synchronized(_redoHistory) {
            _redoHistory.value?.clear()
            _redoHistory.postValue(_redoHistory.value)
        }
    }

    /**
     * 保存编辑历史（包含图片尺寸）
     */
    private fun saveEditorHistory(cropRect: RectF, cropTexMatrix: FloatArray, filterValues: ImageEditorModel.FilterValues) {
        val history = EditorHistory(
            cropRect = cropRect,
            cropTexMatrix = cropTexMatrix,
            filterValues = filterValues,
            imageWidth = model.currentCropWidth,
            imageHeight = model.currentCropHeight
        )
        saveEditorHistory(history)
    }

    private fun applyHistoryAction(action: EditorHistory) {
        // 更新图片尺寸
        model.currentCropWidth = action.imageWidth
        model.currentCropHeight = action.imageHeight
        // 更新裁剪框状态
        updateCropFrameRect(action.cropRect)
        // 更新滤镜状态
        updateFilter(action.filterValues)
        // 同步到渲染器
        model.applyCropToRenderer(renderer ?: return, true)
        model.applyCropTexMatrixToRenderer(renderer ?: return, action.cropTexMatrix)
        model.applyFiltersToRenderer(renderer ?: return)
        model.applyImageDimensions(renderer ?: return, action.imageWidth, action.imageHeight)

        // 确保显示模式为FIT_CENTER，避免图片变形
        _displayMode.value = EditorRenderer.DisplayMode.FIT_CENTER
        renderer?.setDisplayMode(EditorRenderer.DisplayMode.FIT_CENTER)
    }

    fun undo() {
        synchronized(_history) {
            val action = _history.value?.pollLast() ?: return
            synchronized(_redoHistory) {
                val newRedoHistory = _redoHistory.value?.let {
                    ConcurrentLinkedDeque(it) // 复制旧集合的内容到新集合
                } ?: ConcurrentLinkedDeque() // 若旧集合为 null，创建空新集合

                newRedoHistory.addLast(action)
                _redoHistory.postValue(newRedoHistory)
            }

            // 获取当前历史记录的最后一个状态（即要恢复到的状态）
            val currentState = _history.value?.lastOrNull()

            if (currentState != null) {
                // 应用上一个状态到当前状态
                applyHistoryAction(currentState)
            } else {
                // 如果没有历史记录了，恢复到初始状态
                applyHistoryAction(EditorHistory(
                    cropRect = RectF(0f, 0f, 1f, 1f),
                    cropTexMatrix = FloatArray(16).apply { Matrix.setIdentityM(this, 0) },
                    filterValues = ImageEditorModel.FilterValues(false, 1.0f, 1.0f),
                    imageWidth = _bitmap.value!!.width,
                    imageHeight = bitmap.value!!.height
                ))
            }

            _history.postValue(_history.value)
        }
    }

    fun redo() {
        synchronized(_redoHistory) {
            val action = _redoHistory.value?.pollLast() ?: return
            synchronized(_history) {
                val newHistory = _history.value?.let {
                    ConcurrentLinkedDeque(it) // 复制旧集合的内容到新集合
                    } ?: ConcurrentLinkedDeque() // 若旧集合为 null，创建空新集合

                newHistory.addLast(action)
                _history.postValue(newHistory)
            }
            applyHistoryAction(action)
            _redoHistory.postValue(_redoHistory.value)
        }
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
     * 加载Bitmap到内存
     */
    fun loadBitmapToMemory(uri: Uri) {
        Log.d(tag, "loadBitmapToMemory called with URI: $uri")
        _uiState.value = _uiState.value?.copy(isLoading = true)
        _uri.value = uri

        viewModelScope.launch {
            try {
                Log.d(tag, "Starting to load bitmap from URI")
                val bitmap = model.loadBitmapFromUri(context, uri)
                Log.d(tag, "Bitmap loaded, is null: ${bitmap == null}")
                if (bitmap != null) {
                    _bitmap.postValue(bitmap)
                    Log.d(tag, "Bitmap posted to LiveData")
                } else {
                    Log.e(tag, "Bitmap为空")
                    _message.postValue("加载图片失败: 无法获取图片")
                }
                _uiState.postValue(_uiState.value?.copy(isLoading = false))
            }
            catch (e: Exception) {
                Log.e(tag, "加载图片到内存出错", e)
                _message.postValue("加载图片失败: ${e.message}")
                _uiState.postValue(_uiState.value?.copy(isLoading = false))
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
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10及以上版本使用MediaStore API
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    context.contentResolver.update(uri, values, null, null)
                } else {
                    // Android 10以下版本使用广播方式
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    scanIntent.data = uri
                    context.sendBroadcast(scanIntent)
                }
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
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun updateFilterValues(values: ImageEditorModel.FilterValues) {
        model.updateFilterValues(values)
        _filterValues.value = values

        // 应用到渲染器
        model.applyFiltersToRenderer(renderer ?: return)
    }

    /**
     * ViewModel销毁时调用，释放资源
     */
    override fun onCleared() {
        super.onCleared()
        // 释放bitmap资源
        model.originalBitmap?.recycle()
        model.originalBitmap = null
        Log.d(tag, "EditorViewModel已销毁，bitmap资源已释放")
    }

}

/**
 * 编辑器UI状态
 */
data class EditorUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isCropping: Boolean = false,
    val isFiltering: Boolean = false,
    val showFilterControls: Boolean = false
)

/**
 * 用户应用到图片的操作历史记录
 */
data class EditorHistory(
    val cropRect: RectF,
    val cropTexMatrix: FloatArray,
    val filterValues: ImageEditorModel.FilterValues,
    val imageWidth: Int,
    val imageHeight: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditorHistory

        if (cropRect != other.cropRect) return false
        if (!cropTexMatrix.contentEquals(other.cropTexMatrix)) return false
        if (filterValues != other.filterValues) return false
        if (imageWidth != other.imageWidth) return false
        if (imageHeight != other.imageHeight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cropRect.hashCode()
        result = 31 * result + cropTexMatrix.contentHashCode()
        result = 31 * result + filterValues.hashCode()
        result = 31 * result + imageWidth
        result = 31 * result + imageHeight
        return result
    }
}