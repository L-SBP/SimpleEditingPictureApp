package com.example.simpleeditingpictureapp.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.gesture.EditorGestureDetector
import com.example.simpleeditingpictureapp.opengl_es.CropFrameGLSurfaceView
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditorActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView
    private lateinit var btnCrop: TextView
    private lateinit var btnFilter: TextView
    private val tag = "EditorActivity"
    private var imageUri: Uri? = null
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var renderer: EditorRenderer
    private lateinit var cropFrameViewGLSurfaceView: CropFrameGLSurfaceView
    private lateinit var editorGestureDetector: EditorGestureDetector

    // UI 控件
    private lateinit var editorTopBar: ConstraintLayout
    private lateinit var editorActionsBar: ConstraintLayout
    private lateinit var editorBottomTools: LinearLayout
    private lateinit var filterControlsBar: LinearLayout
    private lateinit var canvasContainer: FrameLayout // 新增的画布容器
    private lateinit var btnCancel: TextView
    private lateinit var btnConfirm: TextView
    private lateinit var switchGrayscale: Switch
    private lateinit var seekbarContrast: SeekBar
    private lateinit var seekbarSaturation: SeekBar

    // 缩放相关
    private var scaleEditorGestureDetector: ScaleGestureDetector? = null

    // 状态变量
    private var isCropping = false
    private var isFiltering = false

    // 裁剪框相关
    private var previewCropRect: RectF = RectF(0f, 0f, 1f, 1f)


    // 滤镜默认值
    private var originalUseGrayscale = false
    private var originalContrastValue = 1.0f
    private var originalSaturationValue = 1.0f
    private var useGrayscale = false
    private var contrastValue = 1.0f
    private var saturationValue = 1.0f

    @SuppressLint("ClickableViewAccessibility", "UseSwitchCompatOrMaterialCode")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(tag, "EditorActivity onCreate started")
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_editor)
            Log.d(tag, "setContentView completed")

            Log.d(tag, "bind item")
            bindViews()
            Log.d(tag, "bindViews completed")

            imageUri = intent.getStringExtra("imageUri")?.toUri()
            Log.d(tag, "imageUri: $imageUri")

            // 绑定 OpenGL 画布
            Log.d(tag, "Setting up GLSurfaceView")
            glSurfaceView.setEGLContextClientVersion(2)

            // 创建 OpenGL 渲染器
            Log.d(tag, "Creating EditorRenderer")
            renderer = EditorRenderer(this, imageUri)
            glSurfaceView.setRenderer(renderer)
            // 使用连续渲染模式确保图片加载后能立即显示
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            glSurfaceView.requestRender()
            Log.d(tag, "EditorRenderer setup completed")

            // 创建缩放手势检测器
            Log.d(tag, "Setting up gesture detectors")
            editorGestureDetector = EditorGestureDetector(renderer, glSurfaceView)
            scaleEditorGestureDetector = ScaleGestureDetector(this, editorGestureDetector)

            // 将触摸监听器设置在父容器上
            canvasContainer.setOnTouchListener { _, event ->
                if (isCropping) {
                    // 裁剪模式下，事件只交给 CropFrameView 处理
                    cropFrameViewGLSurfaceView.onTouchEvent(event)
                } else {
                    // 非裁剪模式下，同时处理平移和缩放手势
                    // 缩放手势检测器总是接收事件
                    scaleEditorGestureDetector?.onTouchEvent(event)

                    // 平移手势检测器只在单指触摸时处理事件
                    if (event.pointerCount <= 1) {
                        editorGestureDetector.onTouchEvent(event)
                    }
                }
                true // 消费事件
            }
            Log.d(tag, "Touch listener setup completed")

            Log.d(tag, "Setting up listeners")
            setupListeners()
            Log.d(tag, "EditorActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in EditorActivity onCreate", e)
            throw e
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btn_editor_back)
        btnSave = findViewById(R.id.btn_save)
        btnCrop = findViewById(R.id.tool_crop)
        btnFilter = findViewById(R.id.tool_filter)
        cropFrameViewGLSurfaceView = findViewById(R.id.crop_frame_gl_surface)
        glSurfaceView = findViewById(R.id.gl_canvas)
        canvasContainer = findViewById(R.id.canvas_container)

        editorTopBar = findViewById(R.id.editor_top_bar)
        editorActionsBar = findViewById(R.id.editor_actions_bar)
        editorBottomTools = findViewById(R.id.editor_bottom_tools)
        filterControlsBar = findViewById(R.id.filter_controls_bar)
        btnCancel = findViewById(R.id.btn_cancel)
        btnConfirm = findViewById(R.id.btn_confirm)
        switchGrayscale = findViewById(R.id.switch_grayscale)
        seekbarContrast = findViewById(R.id.seekbar_contrast)
        seekbarSaturation = findViewById(R.id.seekbar_saturation)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            // 直接返回MainActivity，不经过GalleryActivity
            val intent = Intent(this@EditorActivity, MainActivity::class.java)
            // 使用FLAG_ACTIVITY_CLEAR_TOP确保返回到MainActivity而不是创建新实例
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        btnSave.setOnClickListener {
            // 保存图片
            saveEditedImage()
        }

        btnCrop.setOnClickListener {
            enterCropMode()
        }

        btnFilter.setOnClickListener {
            enterFilterMode()
        }

        btnCancel.setOnClickListener {
            if (isCropping) {
                exitCropMode(false)
            } else if (isFiltering) {
                exitFilterMode(false)
            }
        }

        btnConfirm.setOnClickListener {
            if (isCropping) {
                exitCropMode(true)
            } else if (isFiltering) {
                exitFilterMode(true)
            }
        }

        cropFrameViewGLSurfaceView.setOnCropRectChangeListener { normalizedRect ->
            if (isCropping) {
                previewCropRect.set(normalizedRect)
            }
        }

        // Filter Listeners
        switchGrayscale.setOnCheckedChangeListener { _, isChecked ->
            useGrayscale = isChecked
            renderer.setGrayscaleEnabled(useGrayscale)
            glSurfaceView.requestRender()
        }

        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress / 100.0f
                when (seekBar?.id) {
                    R.id.seekbar_contrast -> {
                        contrastValue = value // 0.0 - 2.0 (max is 200, divided by 100)
                        renderer.setContrast(contrastValue)
                    }
                    R.id.seekbar_saturation -> {
                        saturationValue = value // 0.0 - 2.0 (max is 200, divided by 100)
                        renderer.setSaturation(saturationValue)
                    }
                }
                glSurfaceView.requestRender()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekbarContrast.setOnSeekBarChangeListener(seekBarListener)
        seekbarSaturation.setOnSeekBarChangeListener(seekBarListener)
    }

    private fun enterCropMode() {
        isCropping = true
        isFiltering = false
        renderer.setCropMode(false)

        previewCropRect.set(0f, 0f, 1f, 1f)

        cropFrameViewGLSurfaceView.visibility = View.VISIBLE
        updateUiForEditing(true)
        glSurfaceView.requestRender()
    }

    private fun exitCropMode(applyChanges: Boolean) {
        isCropping = false
        cropFrameViewGLSurfaceView.visibility = View.GONE

        if (applyChanges) {
            renderer.setCropMode(true)
            renderer.setCropRect(previewCropRect)
            Log.d(tag, "applying $previewCropRect")
        } else {
            renderer.setCropMode(false)
            renderer.setCropRect(RectF(0f, 0f, 1f, 1f))
            Log.d(tag, "reverting to original")
        }

        updateUiForEditing(false)
        glSurfaceView.requestRender()
    }

    private fun enterFilterMode() {
        isFiltering = true
        isCropping = false

        originalUseGrayscale = useGrayscale
        originalContrastValue = contrastValue
        originalSaturationValue = saturationValue

        // 设置SeekBar的初始值
        switchGrayscale.isChecked = useGrayscale
        seekbarContrast.progress = (contrastValue * 100).toInt()
        seekbarSaturation.progress = (saturationValue * 100).toInt()

        updateUiForEditing(true)
        glSurfaceView.requestRender()

        Log.d(tag, "进入滤镜模式，原始状态: grayscale=$originalUseGrayscale, contrast=$originalContrastValue, saturation=$originalSaturationValue")
    }

    private fun exitFilterMode(applyChanges: Boolean) {
        isFiltering = false
        if (applyChanges) {
            Log.d(tag, "应用滤镜: grayscale=$useGrayscale, contrast=$contrastValue, saturation=$saturationValue")
        } else {
            Log.d(tag, "取消滤镜应用，恢复原始状态: grayscale=$originalUseGrayscale, contrast=$originalContrastValue, saturation=$originalSaturationValue")
            useGrayscale = originalUseGrayscale
            contrastValue = originalContrastValue
            saturationValue = originalSaturationValue

            switchGrayscale.isChecked = useGrayscale
            seekbarContrast.progress = (contrastValue * 100).toInt()
            seekbarSaturation.progress = (saturationValue * 100).toInt()

            renderer.setGrayscaleEnabled(useGrayscale)
            renderer.setContrast(contrastValue)
            renderer.setSaturation(saturationValue)
        }

        updateUiForEditing(false)
        glSurfaceView.requestRender()
    }

    private fun updateUiForEditing(isEditing: Boolean) {
        if (isEditing) {
            editorTopBar.visibility = View.GONE
            editorActionsBar.visibility = View.VISIBLE
            editorBottomTools.visibility = View.GONE

            if (isFiltering) {
                filterControlsBar.visibility = View.VISIBLE
            } else {
                filterControlsBar.visibility = View.GONE
            }
        } else {
            editorTopBar.visibility = View.VISIBLE
            editorActionsBar.visibility = View.GONE
            editorBottomTools.visibility = View.VISIBLE
            filterControlsBar.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        cropFrameViewGLSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        cropFrameViewGLSurfaceView.onResume()
    }

    override fun onDestroy() {
        renderer.release()
        cropFrameViewGLSurfaceView.releaseRenderer()
        super.onDestroy()
    }

    fun getGLSurfaceView(): GLSurfaceView {
        return glSurfaceView
    }

    private fun saveEditedImage() {
        // 在OpenGL线程中获取处理后的图像
        glSurfaceView.queueEvent {
            try {
                // 获取处理后的图像
                val editedBitmap = renderer.getFullBitmap(glSurfaceView.width, glSurfaceView.height)

                // 回到主线程保存图像
                runOnUiThread {
                    if (editedBitmap != null) {
                        saveBitmapToDevice(editedBitmap)
                    } else {
                        Toast.makeText(this@EditorActivity, "保存失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "保存图片时出错", e)
                runOnUiThread {
                    Toast.makeText(this@EditorActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveBitmapToDevice(bitmap: Bitmap) {
        val filename = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        var fos: OutputStream? = null
        var imageUri: Uri?

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上版本使用MediaStore保存
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = contentResolver.openOutputStream(imageUri!!)
            } else {
                // Android 10以下版本使用传统文件保存方式
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SimpleEditingPictureApp")
                if (!picturesDir.exists()) {
                    picturesDir.mkdirs()
                }

                val imageFile = File(picturesDir, filename)
                fos = FileOutputStream(imageFile)
                imageUri = Uri.fromFile(imageFile)
            }

            // 保存图像
            fos?.use{ outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            // 通知媒体扫描器更新图库
            imageUri?.let { uri ->
                val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                scanIntent.data = uri
                sendBroadcast(scanIntent)
            }

            // 显示成功消息并返回主页
            Toast.makeText(this@EditorActivity, "图片已保存", Toast.LENGTH_SHORT).show()

            // 返回主页
            val intent = Intent(this@EditorActivity, MainActivity::class.java)
            // 使用FLAG_ACTIVITY_CLEAR_TOP确保返回到MainActivity而不是创建新实例
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            Log.e(tag, "保存图片到设备时出错", e)
            Toast.makeText(this@EditorActivity, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            fos?.close()
            bitmap.recycle()
        }
    }
}