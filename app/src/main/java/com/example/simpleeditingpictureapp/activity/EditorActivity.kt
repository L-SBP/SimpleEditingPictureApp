package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import android.content.ContentValues
import android.os.Environment
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog

class EditorActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView
    private lateinit var btnCrop: TextView
    private lateinit var btnFilter: TextView
    private val tag = "EditorActivity"
    private var imageUri: Uri? = null
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: EditorRenderer
    
    // 缩放相关
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var scaleFactor = 1.0f
    
    // 裁切相关
    private var isCropping = false
    
    // 滤镜相关
    private var isFiltering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        Log.d(tag, "bind item")

        btnBack = findViewById(R.id.btn_editor_back)
        btnSave = findViewById(R.id.btn_save)
        btnCrop = findViewById(R.id.tool_crop)
        btnFilter = findViewById(R.id.tool_filter)

        imageUri = intent.getStringExtra("imageUri")?.let { Uri.parse(it) }

        glSurfaceView = findViewById(R.id.gl_canvas)
        glSurfaceView.setEGLContextClientVersion(2)

        renderer = EditorRenderer(this, imageUri)
        glSurfaceView.setRenderer(renderer)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        
        // 初始化缩放手势检测器
        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())

        btnBack.setOnClickListener {
            val intent = Intent(this@EditorActivity, GalleryActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnSave.setOnClickListener {
            // 保存图片
            //saveEditedImage()
            val intent = Intent(this@EditorActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnCrop.setOnClickListener {
            toggleCropMode()
        }

        btnFilter.setOnClickListener {
            showFilterDialog()
        }
        
        // 添加触摸监听器以处理缩放
        glSurfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector?.onTouchEvent(event)
            // 将事件传递给其他监听器
            false
        }
    }
    
    // 缩放手势监听器
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            
            // 限制缩放范围
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 3.0f))
            
            // 应用缩放
            renderer.applyScaling(scaleFactor, scaleFactor)
            glSurfaceView.requestRender()
            
            return true
        }
    }
    
    // 切换裁切模式
    private fun toggleCropMode() {
        isCropping = !isCropping
        if (isCropping) {
            renderer.setEditMode(EditorRenderer.EditMode.CROPPING)
            btnCrop.setBackgroundResource(R.drawable.bg_button_selected)
            Toast.makeText(this, "进入裁切模式，拖动以选择裁切区域", Toast.LENGTH_SHORT).show()
        } else {
            renderer.setEditMode(EditorRenderer.EditMode.NONE)
            btnCrop.setBackgroundResource(R.drawable.bg_button_normal)
            Toast.makeText(this, "退出裁切模式", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 显示滤镜对话框
    private fun showFilterDialog() {
        isFiltering = !isFiltering
        if (isFiltering) {
            renderer.setEditMode(EditorRenderer.EditMode.FILTERING)
            btnFilter.setBackgroundResource(R.drawable.bg_button_selected)
            
            val dialogView = layoutInflater.inflate(R.layout.dialog_filters, null)
            val grayscaleSwitch = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_grayscale)
            val contrastSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_contrast)
            val saturationSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_saturation)
            
            val builder = AlertDialog.Builder(this)
                .setTitle("滤镜设置")
                .setView(dialogView)
                .setPositiveButton("应用") { dialog, _ ->
                    // 应用滤镜效果
                    renderer.applyGrayscale(grayscaleSwitch.isChecked)
                    renderer.applyContrast(contrastSeekBar.progress / 50.0f)
                    renderer.applySaturation(saturationSeekBar.progress / 50.0f)
                    glSurfaceView.requestRender()
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    isFiltering = false
                    renderer.setEditMode(EditorRenderer.EditMode.NONE)
                    btnFilter.setBackgroundResource(R.drawable.bg_button_normal)
                    dialog.dismiss()
                }
            
            val dialog = builder.create()
            
            // 初始化控件值
            contrastSeekBar.progress = 50
            saturationSeekBar.progress = 50
            
            // 实时更新滤镜效果
            grayscaleSwitch.setOnCheckedChangeListener { _, isChecked ->
                renderer.applyGrayscale(isChecked)
                glSurfaceView.requestRender()
            }
            
            contrastSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        renderer.applyContrast(progress / 50.0f)
                        glSurfaceView.requestRender()
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            saturationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        renderer.applySaturation(progress / 50.0f)
                        glSurfaceView.requestRender()
                    }
                }
                
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            
            dialog.show()
        } else {
            renderer.setEditMode(EditorRenderer.EditMode.NONE)
            btnFilter.setBackgroundResource(R.drawable.bg_button_normal)
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onDestroy() {
//        renderer.release()
        super.onDestroy()
    }
}