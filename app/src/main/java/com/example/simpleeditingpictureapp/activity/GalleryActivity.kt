package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
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
import android.util.Log

class GalleryActivity : AppCompatActivity() {
    private lateinit var galleryToolbar: Toolbar
    private val imageList: MutableList<ImageBean> = ArrayList()
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter
    private val PERMISSION_REQUEST_CODE = 100
    private val tag = "GalleryActivity"

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
            Log.d(tag, "select")
            val selectedImage = imageAdapter.getSelectedImage()
            if (selectedImage != null) {
                // 启动图片编辑页面
                val intent = Intent(this@GalleryActivity, EditorActivity::class.java)
                intent.putExtra("imageUri", selectedImage.imageUri.toString())
                Log.d(tag, "imageUri: ${selectedImage.imageUri}")
                startActivity(intent)
            } else {
                Toast.makeText(this@GalleryActivity, "请选择一张图片", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkPermission(): Boolean {
        // Android 13 (API 33)及以上使用READ_MEDIA_IMAGES权限
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12及以下使用READ_EXTERNAL_STORAGE权限
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        // Android 13 (API 33)及以上使用READ_MEDIA_IMAGES权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // Android 12及以下使用READ_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
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

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImageList() {
        Thread {
            imageList.clear()
            Log.d(tag, "开始加载图片列表")

            try {
                val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10及以上使用新的API
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.SIZE
                    )
                } else {
                    // Android 9及以下使用旧的API
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA
                    )
                }
                
                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val cursor: Cursor? = contentResolver.query(
                    uri, projection, null, null, sortOrder
                )

                cursor?.use { c ->
                    val idColumn = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    var count = 0
                    
                    Log.d(tag, "找到图片数量: ${c.count}")
                    
                    while (c.moveToNext()) {
                        try {
                            val id = c.getLong(idColumn)
                            val contentUri = Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                            Log.d(tag, "加载图片: $contentUri")
                            
                            val imageBean = ImageBean()
                            imageBean.imageUri = contentUri
                            imageList.add(imageBean)
                            
                            count++
                        } catch (e: Exception) {
                            Log.e(tag, "加载图片出错", e)
                        }
                    }
                    
                    Log.d(tag, "成功加载 $count 张图片")
                }
            } catch (e: Exception) {
                Log.e(tag, "加载图片列表出错", e)
            }

            // 切换到主线程更新UI
            runOnUiThread {
                if (::mRecyclerView.isInitialized && ::imageAdapter.isInitialized) {
                    Log.d(tag, "更新UI，图片数量: ${imageList.size}")
                    imageAdapter.notifyDataSetChanged()
                }
            }
        }.start()
    }
}