package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.activity.GalleryActivity
import com.example.simpleeditingpictureapp.activity.LoginActivity
import com.example.simpleeditingpictureapp.activity.MainActivity
import com.example.simpleeditingpictureapp.model.ProfileModel
import kotlinx.coroutines.launch

/**
 * 个人资料ViewModel
 * 负责处理个人资料的业务逻辑和UI状态
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // 数据模型
    private val model = ProfileModel()

    // UI状态LiveData
    private val _uiState = MutableLiveData<ProfileUiState>()
    val uiState: LiveData<ProfileUiState> = _uiState

    // 消息LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // 应用信息LiveData
    private val _appInfo = MutableLiveData<String>()
    val appInfo: LiveData<String> = _appInfo

    // 当前选中的导航项
    private val _selectedNavigationItem = MutableLiveData<Int>()
    val selectedNavigationItem: LiveData<Int> = _selectedNavigationItem

    // 导航LiveData
    private val _navigationEvent = MutableLiveData<ProfileNavigationEvent>()
    val navigationEvent: LiveData<ProfileNavigationEvent> = _navigationEvent

    // 对话框事件LiveData
    private val _dialogEvent = MutableLiveData<DialogEvent>()
    val dialogEvent: LiveData<DialogEvent> = _dialogEvent

    init {
        // 初始化UI状态
        _uiState.value = ProfileUiState()

        // 设置默认选中的导航项为个人资料
        _selectedNavigationItem.value = 2

        // 加载应用信息
        loadAppInfo()
    }

    /**
     * 加载应用信息
     */
    private fun loadAppInfo() {
        _uiState.value = _uiState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val info = model.getAppVersionInfo()
                _appInfo.postValue(info)
                _uiState.postValue(_uiState.value?.copy(isLoading = false))
            } catch (e: Exception) {
                _uiState.postValue(_uiState.value?.copy(isLoading = false, error = "加载应用信息失败: ${e.message}"))
            }
        }
    }

    /**
     * 处理设置点击
     */
    fun onSettingsClick() {
        if (!model.getSettingsStatus()) {
            _message.value = "设置功能待开发"
        }
    }

    /**
     * 处理关于点击
     */
    fun onAboutClick() {
        _dialogEvent.value = DialogEvent.ShowAbout
    }

    /**
     * 处理退出登录点击
     */
    fun onLogoutClick() {
        _dialogEvent.value = DialogEvent.ShowLogout
    }

    /**
     * 确认退出登录
     */
    fun confirmLogout() {
        _navigationEvent.value = ProfileNavigationEvent.ToLogin
    }

    /**
     * 处理导航项点击
     * @param position 导航项位置
     */
    fun onNavigationItemClick(position: Int) {
        _selectedNavigationItem.value = position

        when (position) {
            0 -> {
                // 主页
                _navigationEvent.value = ProfileNavigationEvent.ToMain
            }
            1 -> {
                // 修图
                _navigationEvent.value = ProfileNavigationEvent.ToGallery
            }
            2 -> {
                // 我的，已经在当前页面，不需要导航
            }
        }
    }
}

/**
 * 个人资料UI状态
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 导航事件
 */
sealed class ProfileNavigationEvent {
    object ToMain : ProfileNavigationEvent()
    object ToGallery : ProfileNavigationEvent()
    object ToLogin : ProfileNavigationEvent()
}

/**
 * 对话框事件
 */
sealed class DialogEvent {
    object ShowAbout : DialogEvent()
    object ShowLogout : DialogEvent()
}
