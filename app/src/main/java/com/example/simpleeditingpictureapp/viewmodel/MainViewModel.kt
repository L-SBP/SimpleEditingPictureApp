package com.example.simpleeditingpictureapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.activity.GalleryActivity
import com.example.simpleeditingpictureapp.activity.ProfileActivity
import com.example.simpleeditingpictureapp.model.MainModel
import com.example.simpleeditingpictureapp.recyclerview.bean.RecommendBean
import kotlinx.coroutines.launch

/**
 * 主页ViewModel
 * 负责处理主页的业务逻辑和UI状态
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "MainViewModel"
    private val context = application.applicationContext

    // 数据模型
    private val model = MainModel()

    // UI状态LiveData
    private val _uiState = MutableLiveData<MainUiState>()
    val uiState: LiveData<MainUiState> = _uiState

    // Banner数据LiveData
    private val _bannerData = MutableLiveData<List<Int>>()
    val bannerData: LiveData<List<Int>> = _bannerData

    // 推荐数据LiveData
    private val _recommendData = MutableLiveData<List<RecommendBean>>()
    val recommendData: LiveData<List<RecommendBean>> = _recommendData

    // 当前选中的导航项
    private val _selectedNavigationItem = MutableLiveData<Int>()
    val selectedNavigationItem: LiveData<Int> = _selectedNavigationItem

    // 导航LiveData
    private val _navigationEvent = MutableLiveData<MainNavigationEvent>()
    val navigationEvent: LiveData<MainNavigationEvent> = _navigationEvent

    init {
        // 初始化UI状态
        _uiState.value = MainUiState()

        // 加载数据
        loadData()

        // 设置默认选中的导航项
        _selectedNavigationItem.value = 0
    }

    /**
     * 加载数据
     */
    private fun loadData() {
        _uiState.value = _uiState.value?.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // 加载Banner数据
                val bannerList = model.getBannerData()
                _bannerData.postValue(bannerList)

                // 加载推荐数据
                val recommendList = model.getRecommendData()
                _recommendData.postValue(recommendList)

                _uiState.postValue(_uiState.value?.copy(isLoading = false))
            } catch (e: Exception) {
                Log.e(tag, "加载数据出错", e)
                _uiState.postValue(_uiState.value?.copy(isLoading = false, error = "加载数据失败: ${e.message}"))
            }
        }
    }

    /**
     * 处理导航项点击
     * @param position 导航项位置
     */
    fun onNavigationItemClick(position: Int) {
        _selectedNavigationItem.value = position

        when (position) {
            0 -> {
                // 主页，不需要导航
            }
            1 -> {
                // 修图，导航到图库页面
                _navigationEvent.value = MainNavigationEvent.ToGallery
            }
            2 -> {
                // 我的，导航到个人资料页面
                _navigationEvent.value = MainNavigationEvent.ToProfile
            }
        }
    }

    /**
     * 将dp转换为px
     * @param dp dp值
     * @return px值
     */
    fun dp2px(dp: Float): Int {
        return model.dp2px(dp, context.resources.displayMetrics.density)
    }
}

/**
 * 主页UI状态
 */
data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 导航事件
 */
sealed class MainNavigationEvent {
    object ToGallery : MainNavigationEvent()
    object ToProfile : MainNavigationEvent()
}
