package com.example.simpleeditingpictureapp.activity

import android.annotation.SuppressLint
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.gesture.EditorGestureDetector
import com.example.simpleeditingpictureapp.model.ImageEditorModel
import com.example.simpleeditingpictureapp.opengl_es.CropFrameGLSurfaceView
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import com.example.simpleeditingpictureapp.viewmodel.EditorViewModel

@SuppressLint("UseSwitchCompatOrMaterialCode")
class EditorActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var btnSave: ImageView
    private lateinit var btnUndo: ImageView
    private lateinit var btnRedo: ImageView
    private lateinit var btnCrop: TextView
    private lateinit var btnFilter: TextView
    private val tag = "EditorActivity"
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

    // ViewModel
    private val viewModel: EditorViewModel by viewModels()

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

            val imageUri = intent.getStringExtra("imageUri")?.toUri()
            Log.d(tag, "imageUri: $imageUri")

            // 检查URI是否为空，如果为空则退出编辑器
            if (imageUri == null) {
                Log.e(tag, "图片URI为空，退出编辑器")
                Toast.makeText(this, "无法加载图片", Toast.LENGTH_SHORT).show()
                finish()
                return@onCreate
            }

            // 绑定 OpenGL 画布
            Log.d(tag, "Setting up GLSurfaceView")
            glSurfaceView.setEGLContextClientVersion(2)

            // 创建 OpenGL 渲染器
            Log.d(tag, "Creating EditorRenderer")
            renderer = EditorRenderer(this)
            glSurfaceView.setRenderer(renderer)

            // 注册观察者
            viewModel.bitmap.observe(this) { bitmap ->
                bitmap?.let {
                    Log.d(tag, "Bitmap loaded, setting to renderer")
                    glSurfaceView.queueEvent {
                        renderer.setBitmap(it)
                    }
                }
            }

            // 使用连续渲染模式确保图片加载后能立即显示
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            glSurfaceView.requestRender()

            // 设置渲染器到ViewModel
            viewModel.setRenderer(renderer)
            // 设置ViewModel到渲染器，用于在OpenGL上下文重建时重新加载纹理
            renderer.setViewModel(viewModel)
            Log.d(tag, "EditorRenderer setup completed")

            // 创建缩放手势检测器
            Log.d(tag, "Setting up gesture detectors")
            editorGestureDetector = EditorGestureDetector(renderer, glSurfaceView)
            scaleEditorGestureDetector = ScaleGestureDetector(this, editorGestureDetector)

            // 将触摸监听器设置在父容器上
            canvasContainer.setOnTouchListener { _, event ->
                val uiState = viewModel.uiState.value
                if (uiState?.isCropping == true) {
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
            observeViewModel()

            // 设置URI，触发图片加载
            viewModel.setImageUri(imageUri)

            Log.d(tag, "EditorActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in EditorActivity onCreate", e)
            throw e
        }
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btn_editor_back)
        btnSave = findViewById(R.id.btn_save)
        btnUndo = findViewById(R.id.btn_undo)
        btnRedo = findViewById(R.id.btn_redo)
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
            viewModel.navigateToMain()
            finish()
        }

        btnSave.setOnClickListener {
            // 保存图片
            viewModel.saveEditedImage(glSurfaceView)
        }

        btnUndo.setOnClickListener {
            // 撤销操作
            viewModel.undo()
        }

        btnRedo.setOnClickListener {
            // 重做操作
            viewModel.redo()
        }

        btnCrop.setOnClickListener {
            viewModel.enterCropMode()
        }

        btnFilter.setOnClickListener {
            viewModel.enterFilterMode()
        }

        btnCancel.setOnClickListener {
            val uiState = viewModel.uiState.value
            if (uiState?.isCropping == true) {
                viewModel.exitCropMode(false)
            } else if (uiState?.isFiltering == true) {
                viewModel.exitFilterMode(false)
            }
        }

        btnConfirm.setOnClickListener {
            val uiState = viewModel.uiState.value
            if (uiState?.isCropping == true) {
                viewModel.exitCropMode(true)
            } else if (uiState?.isFiltering == true) {
                viewModel.exitFilterMode(true)
            }
        }

        cropFrameViewGLSurfaceView.setOnCropRectChangeListener { normalizedRect ->
            viewModel.updateCropFrameRect(normalizedRect)
        }

        // Filter Listeners
        switchGrayscale.setOnCheckedChangeListener { _, isChecked ->
            val currentValues = viewModel.filterValues.value ?: ImageEditorModel.FilterValues(false, 1.0f, 1.0f)
            viewModel.updateFilterValues(currentValues.copy(grayscale = isChecked))
        }

        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                val value = progress / 100.0f
                val currentValues = viewModel.filterValues.value ?: ImageEditorModel.FilterValues(false, 1.0f, 1.0f)

                when (seekBar?.id) {
                    R.id.seekbar_contrast -> {
                        viewModel.updateFilterValues(currentValues.copy(contrast = value))
                    }
                    R.id.seekbar_saturation -> {
                        viewModel.updateFilterValues(currentValues.copy(saturation = value))
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekbarContrast.setOnSeekBarChangeListener(seekBarListener)
        seekbarSaturation.setOnSeekBarChangeListener(seekBarListener)
    }

    /**
     * 更新滤镜值
     */
    private fun updateFilterValues(values: ImageEditorModel.FilterValues) {
        viewModel.updateFilter(values)
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察UI状态
        viewModel.uiState.observe(this) { uiState ->
            updateUiForEditing(uiState.isEditing, uiState.isCropping, uiState.isFiltering, uiState.showFilterControls)
        }

        // 只在uri首次设置时加载图片
        var lastObservedUri: Uri? = null
        viewModel.uri.observe(this) { imageUri ->
            Log.d(tag, "URI observed: $imageUri, bitmap is null: ${viewModel.bitmap.value == null}")
            // 防止重复加载同一张图片
            if (imageUri != null && viewModel.bitmap.value == null && imageUri != lastObservedUri) {
                Log.d(tag, "Loading bitmap from URI")
                lastObservedUri = imageUri
                viewModel.loadBitmapToMemory(imageUri)
            }
        }
        // 观察滤镜值
        viewModel.filterValues.observe(this) { filterValues ->
            switchGrayscale.isChecked = filterValues.grayscale
            seekbarContrast.progress = (filterValues.contrast * 100).toInt()
            seekbarSaturation.progress = (filterValues.saturation * 100).toInt()

            // 请求渲染
            glSurfaceView.requestRender()
        }

        // 观察裁剪框
        viewModel.cropRect.observe(this) { cropRect ->
            // 裁剪框更新会自动在CropFrameGLSurfaceView中处理
        }

        // 观察撤销/重做状态
        viewModel.canUndo.observe(this) { canUndo ->
            btnUndo.alpha = if (canUndo) 1.0f else 0.5f
            btnUndo.isEnabled = canUndo
        }

        viewModel.canRedo.observe(this) { canRedo ->
            btnRedo.alpha = if (canRedo) 1.0f else 0.5f
            btnRedo.isEnabled = canRedo
        }

        // 观察消息
        viewModel.message.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // 观察保存结果
        viewModel.saveResult.observe(this) { success ->
            if (success) {
                // 保存成功，导航到主页
                viewModel.navigateToMain()
                finish()
            }
        }

        // 观察显示模式变化
        viewModel.displayMode.observe(this) { mode ->
            glSurfaceView.queueEvent {
                renderer.setDisplayMode(mode)
            }
        }
    }

    /**
     * 更新UI状态
     */
    private fun updateUiForEditing(isEditing: Boolean, isCropping: Boolean, isFiltering: Boolean, showFilterControls: Boolean) {
        if (isEditing) {
            editorTopBar.visibility = View.GONE
            editorActionsBar.visibility = View.VISIBLE
            editorBottomTools.visibility = View.GONE

            if (isCropping) {
                cropFrameViewGLSurfaceView.visibility = View.VISIBLE
                filterControlsBar.visibility = View.GONE
            } else if (isFiltering) {
                cropFrameViewGLSurfaceView.visibility = View.GONE
                filterControlsBar.visibility = View.VISIBLE
            }
        } else {
            editorTopBar.visibility = View.VISIBLE
            editorActionsBar.visibility = View.GONE
            editorBottomTools.visibility = View.VISIBLE
            cropFrameViewGLSurfaceView.visibility = View.GONE
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
}