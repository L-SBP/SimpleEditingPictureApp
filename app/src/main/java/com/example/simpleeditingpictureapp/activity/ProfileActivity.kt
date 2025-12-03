
package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleeditingpictureapp.R

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        llSettings = findViewById(R.id.ll_settings)
        llAbout = findViewById(R.id.ll_about)
        llLogout = findViewById(R.id.ll_logout)
        
        // 初始化底部导航栏
        initBottomNavigation()

        llSettings.setOnClickListener {
            Toast.makeText(this, "设置功能待开发", Toast.LENGTH_SHORT).show()
        }

        llAbout.setOnClickListener {
            showAboutDialog()
        }

        llLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage("易修 v1.0 简单易用的图片编辑工具")
            .setPositiveButton("确定", null)
            .show()
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
        
        // 设置我的为选中状态
        setNavigationItemSelection(2)
        
        // 主页点击事件
        llHome.setOnClickListener {
            setNavigationItemSelection(0)
            val intent = Intent(this@ProfileActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // 修图点击事件
        llEdit.setOnClickListener {
            setNavigationItemSelection(1)
            val intent = Intent(this@ProfileActivity, GalleryActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // 我的点击事件
        llProfile.setOnClickListener {
            setNavigationItemSelection(2)
        }
    }
    
    private fun setNavigationItemSelection(position: Int) {
        // 重置所有导航项的颜色
        ivHome.setColorFilter(resources.getColor(android.R.color.darker_gray))
        ivEdit.setColorFilter(resources.getColor(android.R.color.darker_gray))
        ivProfile.setColorFilter(resources.getColor(android.R.color.darker_gray))
        tvHome.setTextColor(resources.getColor(android.R.color.darker_gray))
        tvEdit.setTextColor(resources.getColor(android.R.color.darker_gray))
        tvProfile.setTextColor(resources.getColor(android.R.color.darker_gray))
        
        // 设置选中项的颜色
        when (position) {
            0 -> {
                ivHome.setColorFilter(resources.getColor(R.color.pink))
                tvHome.setTextColor(resources.getColor(R.color.pink))
            }
            1 -> {
                ivEdit.setColorFilter(resources.getColor(R.color.pink))
                tvEdit.setTextColor(resources.getColor(R.color.pink))
            }
            2 -> {
                ivProfile.setColorFilter(resources.getColor(R.color.pink))
                tvProfile.setTextColor(resources.getColor(R.color.pink))
            }
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定") { _, _ ->
                // 退出登录，返回登录界面
                val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
