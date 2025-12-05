package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.simpleeditingpictureapp.activity.MainActivity
import com.example.simpleeditingpictureapp.model.LoginModel
import com.example.simpleeditingpictureapp.model.LoginResult
import com.example.simpleeditingpictureapp.model.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 登录ViewModel
 * 负责处理登录的业务逻辑和UI状态
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "LoginViewModel"

    // 数据模型
    private val model = LoginModel()

    // UI状态LiveData
    private val _uiState = MutableLiveData<LoginUiState>()
    val uiState: LiveData<LoginUiState> = _uiState

    // 消息LiveData
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // 导航LiveData
    private val _navigationEvent = MutableLiveData<LoginNavigationEvent>()
    val navigationEvent: LiveData<LoginNavigationEvent> = _navigationEvent

    init {
        // 初始化UI状态
        _uiState.value = LoginUiState()
    }

    /**
     * 登录
     * @param username 用户名
     * @param password 密码
     */
     fun login(username: String, password: String) {
        // 验证输入
        when (val validationResult = model.validateInput(username, password)) {
            is ValidationResult.Success -> {
                // 输入有效，进行登录验证
                _uiState.value = _uiState.value?.copy(isLoading = true)


                viewModelScope.launch {
                    try {
                        val loginResult = withContext(Dispatchers.IO) {
                            model.validateCredentials(username, password)
                        }
                        when (loginResult) {
                            is LoginResult.Success -> {
                                _message.value = "登录成功"
                                _navigationEvent.value = LoginNavigationEvent.ToMain
                            }
                            is LoginResult.Error -> {
                                _message.value = loginResult.message
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "登录出错", e)
                        _message.value = "登录失败: ${e.message}"
                    } finally {
                        _uiState.value = _uiState.value?.copy(isLoading = false)
                    }
                }
            }
            is ValidationResult.Error -> {
                _message.value = validationResult.message
            }
        }
    }
}

/**
 * 登录UI状态
 */
data class LoginUiState(
    val isLoading: Boolean = false
)

/**
 * 导航事件
 */
sealed class LoginNavigationEvent {
    object ToMain : LoginNavigationEvent()
}
