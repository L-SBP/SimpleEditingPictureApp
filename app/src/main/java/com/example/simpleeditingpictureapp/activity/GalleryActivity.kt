package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.adapter.ImageAdapter
import com.example.simpleeditingpictureapp.recyclerview.bean.ImageBean
import java.util.*

class GalleryActivity : AppCompatActivity() {
    private lateinit var galleryToolbar: Toolbar
    private val imageList: MutableList<ImageBean> = ArrayList()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        galleryToolbar = findViewById(R.id.toolbar_gallery)
        setSupportActionBar(galleryToolbar)

        mRecyclerView = findViewById(R.id.rv_gallery_grid)

        galleryToolbar.setNavigationOnClickListener {
            val intent = Intent(this@GalleryActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 使用3列网格布局而不是4列，使图片更大更清晰
        val gridLayoutManager = GridLayoutManager(this, 3)
        gridLayoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = gridLayoutManager

        imageAdapter = ImageAdapter(imageList)
        mRecyclerView.adapter = imageAdapter

        // 检查权限并加载图片
        if (checkPermission()) {
            loadImageList()
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
            val selectedImage = imageAdapter.getSelectedImage()
            if (selectedImage != null) {
                // 启动图片编辑页面
                val intent = Intent(this@GalleryActivity, EditorActivity::class.java)
                intent.putExtra("imageUri", selectedImage.imageUri)
                startActivity(intent)
            } else {
                Toast.makeText(this@GalleryActivity, "请选择一张图片", Toast.LENGTH_SHORT).show()
            }
            return true
        } else if (itemId == android.R.id.home) {
            // 处理返回按钮
            val intent = Intent(this@GalleryActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImageList()
            } else {
                Toast.makeText(this, "需要存储权限才能加载相册图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadImageList() {
        Thread {
            imageList.clear()

            try {
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA
                )
                val cursor: Cursor? = contentResolver.query(
                    uri, projection, null, null,
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
                )

                cursor?.use { c ->
                    while (c.moveToNext()) {
                        try {
                            // 获取图片ID并构建Uri
                            val imageIdIndex =
                                c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                            val imageId = c.getLong(imageIdIndex)

                            // 构建图片Uri
                            val imageUri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                imageId.toString()
                            )

                            val imageBean = ImageBean()
                            imageBean.imageUri = imageUri
                            imageList.add(imageBean)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 切换到主线程更新UI
            runOnUiThread {
                if (::mRecyclerView.isInitialized && ::imageAdapter.isInitialized) {
                    imageAdapter.notifyDataSetChanged()
                }
            }
        }.start()
    }
}