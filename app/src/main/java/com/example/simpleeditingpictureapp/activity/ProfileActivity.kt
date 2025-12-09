package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.viewmodel.DialogEvent
import com.example.simpleeditingpictureapp.viewmodel.ProfileNavigationEvent
import com.example.simpleeditingpictureapp.viewmodel.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var llSettings: LinearLayout
    private lateinit var llAbout: LinearLayout
    private lateinit var llLogout: LinearLayout

    // 底部导航栏相关
    private lateinit var llHome: LinearLayout
    private lateinit var llEdit: LinearLayout
    private lateinit var llProfile: LinearLayout
    private lateinit var ivHome: ImageView
    private lateinit var ivEdit: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var tvHome: TextView
    private lateinit var tvEdit: TextView
    private lateinit var tvProfile: TextView

    // ViewModel
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        llSettings = findViewById(R.id.ll_settings)
        llAbout = findViewById(R.id.ll_about)
        llLogout = findViewById(R.id.ll_logout)

        // 初始化底部导航栏
        initBottomNavigation()

        // 观察ViewModel中的数据变化
        observeViewModel()

        llSettings.setOnClickListener {
            viewModel.onSettingsClick()
        }

        llAbout.setOnClickListener {
            viewModel.onAboutClick()
        }

        llLogout.setOnClickListener {
            viewModel.onLogoutClick()
        }
    }

    private fun initBottomNavigation() {
        llHome = findViewById(R.id.ll_home)
        llEdit = findViewById(R.id.ll_edit)
        llProfile = findViewById(R.id.ll_profile)
        ivHome = findViewById(R.id.iv_home)
        ivEdit = findViewById(R.id.iv_edit)
        ivProfile = findViewById(R.id.iv_profile)
        tvHome = findViewById(R.id.tv_home)
        tvEdit = findViewById(R.id.tv_edit)
        tvProfile = findViewById(R.id.tv_profile)

        // 主页点击事件
        llHome.setOnClickListener {
            viewModel.onNavigationItemClick(0)
        }

        // 修图点击事件
        llEdit.setOnClickListener {
            viewModel.onNavigationItemClick(1)
        }

        // 我的点击事件
        llProfile.setOnClickListener {
            viewModel.onNavigationItemClick(2)
        }
    }

/**
 * Sets the selection state of navigation items based on the given position.
 * This function resets all navigation items to a default color and then
 * highlights the selected item with a pink color.
 *
 * @param position The position of the navigation item to be selected (0: Home, 1: Edit, 2: Profile)
 */
    private fun setNavigationItemSelection(position: Int) {
        // 重置所有导航项的颜色
        ivHome.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        ivEdit.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        ivProfile.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
        tvHome.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        tvEdit.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        tvProfile.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        // 设置选中项的颜色
        when (position) {
            0 -> {
                ivHome.setColorFilter(ContextCompat.getColor(this, R.color.pink))
                tvHome.setTextColor(ContextCompat.getColor(this, R.color.pink))
            }
            1 -> {
                ivEdit.setColorFilter(ContextCompat.getColor(this, R.color.pink))
                tvEdit.setTextColor(ContextCompat.getColor(this, R.color.pink))
            }
            2 -> {
                ivProfile.setColorFilter(ContextCompat.getColor(this, R.color.pink))
                tvProfile.setTextColor(ContextCompat.getColor(this, R.color.pink))
            }
        }
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察消息
        viewModel.message.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        // 观察应用信息
        viewModel.appInfo.observe(this, Observer { info ->
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show()
        })

        // 观察选中的导航项
        viewModel.selectedNavigationItem.observe(this, Observer { position ->
            setNavigationItemSelection(position)
        })

        // 观察导航事件
        viewModel.navigationEvent.observe(this, Observer { event ->
            when (event) {
                is ProfileNavigationEvent.ToMain -> {
                    val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is ProfileNavigationEvent.ToGallery -> {
                    val intent = Intent(this@ProfileActivity, GalleryActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is ProfileNavigationEvent.ToLogin -> {
                    val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        })

        // 观察对话框事件
        viewModel.dialogEvent.observe(this, Observer { event ->
            when (event) {
                is DialogEvent.ShowAbout -> {
                    showAboutDialog()
                }
                is DialogEvent.ShowLogout -> {
                    showLogoutDialog()
                }
            }
        })
    }

    private fun showAboutDialog() {
        val appInfo = viewModel.appInfo.value ?: "易修 v1.0 简单易用的图片编辑工具"
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(appInfo)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                viewModel.confirmLogout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onNavigationItemClick(2)
    }
}
