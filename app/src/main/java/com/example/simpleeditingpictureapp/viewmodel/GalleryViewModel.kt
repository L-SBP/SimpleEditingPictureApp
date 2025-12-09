package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.simpleeditingpictureapp.model.GalleryModel
import com.example.simpleeditingpictureapp.recyclerview.bean.ImageBean
import kotlinx.coroutines.launch

/**
 * 图库ViewModel
 * 负责处理图库的业务逻辑和UI状态
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "GalleryViewModel"
    private val context = application.applicationContext

    // 数据模型
    private val model = GalleryModel()

    // UI状态LiveData
    private val _uiState = MutableLiveData<GalleryUiState>()
    val uiState: LiveData<GalleryUiState> = _uiState

    // 图片列表LiveData
    private val _imageList = MutableLiveData<List<ImageBean>>()
    val imageList: LiveData<List<ImageBean>> = _imageList

    // 消息LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // 导航LiveData
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // 权限请求码
    val PERMISSION_REQUEST_CODE = 100

    init {
        // 初始化UI状态
        _uiState.value = GalleryUiState()
    }

    /**
     * 检查权限
     */
    fun checkPermission(): Boolean {
        // Android 13 (API 33)及以上使用READ_MEDIA_IMAGES权限
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == 
                PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12及以下使用READ_EXTERNAL_STORAGE权限
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == 
                PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取请求权限的数组
     */
    fun getPermissionArray(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImageList()
        } else {
            _message.value = "需要存储权限才能加载相册图片"
        }
    }

    /**
     * 加载图片列表
     */
    fun loadImageList() {
        _uiState.value = _uiState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val images = model.loadImageList(context)
                _imageList.postValue(images)
                _uiState.postValue(_uiState.value?.copy(isLoading = false))
            } catch (e: Exception) {
                Log.e(tag, "加载图片列表出错", e)
                _message.postValue("加载图片失败: ${e.message}")
                _uiState.postValue(_uiState.value?.copy(isLoading = false))
            }
        }
    }

    /**
     * 处理选择图片
     */
    fun onSelectImage(selectedImage: ImageBean?) {
        if (selectedImage != null) {
            // 启动图片编辑页面
            _navigationEvent.value = NavigationEvent.ToEditor(selectedImage.imageUri)
        } else {
            _message.value = "请选择一张图片"
        }
    }

    /**
     * 导航返回
     */
    fun navigateBack() {
        _navigationEvent.value = NavigationEvent.ToMain
    }
}

/**
 * 图库UI状态
 */
data class GalleryUiState(
    val isLoading: Boolean = false
)

/**
 * 导航事件
 */
sealed class NavigationEvent {
    object ToMain : NavigationEvent()
    data class ToEditor(val imageUri: Uri?) : NavigationEvent()
}
