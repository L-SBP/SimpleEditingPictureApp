package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.Observer
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.adapter.ImageAdapter
import com.example.simpleeditingpictureapp.recyclerview.bean.ImageBean
import com.example.simpleeditingpictureapp.viewmodel.GalleryViewModel
import com.example.simpleeditingpictureapp.viewmodel.NavigationEvent
import java.util.*
import android.util.Log

class GalleryActivity : AppCompatActivity() {
    private lateinit var galleryToolbar: Toolbar
    private val imageList: MutableList<ImageBean> = ArrayList()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val tag = "GalleryActivity"

    // ViewModel
    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        galleryToolbar = findViewById(R.id.toolbar_gallery)
        setSupportActionBar(galleryToolbar)

        mRecyclerView = findViewById(R.id.rv_gallery_grid)

        // 设置导航点击监听
        galleryToolbar.setNavigationOnClickListener {
            viewModel.navigateBack()
        }

        val gridLayoutManager = GridLayoutManager(this, 3)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = gridLayoutManager

        imageAdapter = ImageAdapter(imageList)
        mRecyclerView.adapter = imageAdapter

        // 观察ViewModel中的数据变化
        observeViewModel()

        // 检查权限并加载图片
        if (viewModel.checkPermission()) {
            viewModel.loadImageList()
        } else {
            requestPermission()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 加载菜单资源
        menuInflater.inflate(R.menu.menu_gallery_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        if (itemId == R.id.menu_select) {
            // 处理"选择"菜单项点击事件
            Log.d(tag, "select")
            val selectedImage = imageAdapter.getSelectedImage()
            viewModel.onSelectImage(selectedImage)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestPermission() {
        androidx.core.app.ActivityCompat.requestPermissions(
            this,
            viewModel.getPermissionArray(),
            viewModel.PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == viewModel.PERMISSION_REQUEST_CODE) {
            viewModel.handlePermissionResult(grantResults)
        }
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察图片列表
        viewModel.imageList.observe(this, Observer { images ->
            imageList.clear()
            imageList.addAll(images)
            imageAdapter.notifyDataSetChanged()
            Log.d(tag, "更新UI，图片数量: ${imageList.size}")
        })

        // 观察消息
        viewModel.message.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        // 观察导航事件
        viewModel.navigationEvent.observe(this, Observer { event ->
            when (event) {
                is NavigationEvent.ToMain -> {
                    val intent = Intent(this@GalleryActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is NavigationEvent.ToEditor -> {
                    val intent = Intent(this@GalleryActivity, EditorActivity::class.java)
                    intent.putExtra("imageUri", event.imageUri.toString())
                    Log.d(tag, "imageUri: ${event.imageUri}")
                    startActivity(intent)
                }
            }
        })
    }
}
