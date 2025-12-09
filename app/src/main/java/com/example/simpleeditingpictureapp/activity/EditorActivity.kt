package com.example.simpleeditingpictureapp.activity

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.lifecycle.observe
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.gesture.EditorGestureDetector
import com.example.simpleeditingpictureapp.opengl_es.CropFrameGLSurfaceView
import com.example.simpleeditingpictureapp.opengl_es.EditorRenderer
import com.example.simpleeditingpictureapp.viewmodel.EditorViewModel

/**
 * 编辑器Activity - 纯View层，负责UI展示和用户交互
 * 所有业务逻辑通过ViewModel处理
 */
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
    private lateinit var canvasContainer: FrameLayout
    private lateinit var btnCancel: TextView
    private lateinit var btnConfirm: TextView
    private lateinit var switchGrayscale: Switch
    private lateinit var seekbarContrast: SeekBar
    private lateinit var seekbarSaturation: SeekBar

    // 裁剪比例选择器
    private lateinit var cropAspectRatioSelector: View
    private lateinit var btnAspectFree: TextView
    private lateinit var btnAspect1To1: TextView
    private lateinit var btnAspect3To4: TextView
    private lateinit var btnAspect4To3: TextView
    private lateinit var btnAspect9To16: TextView
    private lateinit var btnAspect16To9: TextView

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

            // 初始化ViewModel
            viewModel.initializeEditor(intent.getStringExtra("imageUri")?.toUri())

            // 观察ViewModel状态变化
            observeViewModelEvents()
            observeViewModel()

            // 设置UI
            setupOpenGLRenderer()
            setupGestureDetectors()
            setupListeners()

            Log.d(tag, "EditorActivity onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error in EditorActivity onCreate", e)
            throw e
        }
    }

    /**
     * 设置OpenGL渲染器
     */
    private fun setupOpenGLRenderer() {
        Log.d(tag, "Setting up GLSurfaceView")
        glSurfaceView.setEGLContextClientVersion(2)

        // 创建 OpenGL 渲染器
        Log.d(tag, "Creating EditorRenderer")
        renderer = EditorRenderer(this)
        glSurfaceView.setRenderer(renderer)

        // 使用连续渲染模式确保图片加载后能立即显示
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        glSurfaceView.requestRender()

        // 设置渲染器到ViewModel
        viewModel.setRenderer(renderer)
        // 设置ViewModel到渲染器，用于在OpenGL上下文重建时重新加载纹理
        renderer.setViewModel(viewModel)
        Log.d(tag, "EditorRenderer setup completed")
    }

    /**
     * 设置手势检测器
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetectors() {
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
                // 首先处理缩放手势
                scaleEditorGestureDetector?.onTouchEvent(event)

                // 然后处理平移手势（单指触摸时）
                if (event.pointerCount <= 1) {
                    editorGestureDetector.onTouchEvent(event)
                }
            }
            true // 消费事件
        }
        Log.d(tag, "Touch listener setup completed")
    }

    /**
     * 观察ViewModel事件
     */
    private fun observeViewModelEvents() {
        // 观察错误事件
        viewModel.errorEvent.observe(this) { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            if (errorMessage.contains("无法加载图片")) {
                finish()
            }
        }

        // 观察Bitmap加载
        viewModel.bitmap.observe(this) { bitmap ->
            bitmap?.let {
                Log.d(tag, "Bitmap loaded, setting to renderer")
                glSurfaceView.queueEvent {
                    renderer.setBitmap(it)
                }
            }
        }

        // 观察导航事件
        viewModel.navigationEvent.observe(this) { navigateToMain ->
            if (navigateToMain) {
                // 导航到MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
        
        // 观察保存结果
        viewModel.saveResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "图片保存成功", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 观察消息
        viewModel.message.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
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

        // 绑定裁剪比例选择器
        cropAspectRatioSelector = findViewById(R.id.crop_aspect_ratio_selector)
        btnAspectFree = cropAspectRatioSelector.findViewById(R.id.btn_aspect_free)
        btnAspect1To1 = cropAspectRatioSelector.findViewById(R.id.btn_aspect_1_1)
        btnAspect3To4 = cropAspectRatioSelector.findViewById(R.id.btn_aspect_3_4)
        btnAspect4To3 = cropAspectRatioSelector.findViewById(R.id.btn_aspect_4_3)
        btnAspect9To16 = cropAspectRatioSelector.findViewById(R.id.btn_aspect_9_16)
        btnAspect16To9 = cropAspectRatioSelector.findViewById(R.id.btn_aspect_16_9)
    }

    /**
     * 设置UI控件监听器
     */
    private fun setupListeners() {
        // 顶部工具栏按钮
        btnBack.setOnClickListener { viewModel.onBackClicked() }
        btnSave.setOnClickListener { viewModel.onSaveClicked(glSurfaceView) }
        btnUndo.setOnClickListener { viewModel.onUndoClicked() }
        btnRedo.setOnClickListener { viewModel.onRedoClicked() }

        // 底部工具按钮
        btnCrop.setOnClickListener { viewModel.onCropClicked() }
        btnFilter.setOnClickListener { viewModel.onFilterClicked() }

        // 裁剪比例选择器按钮
        btnAspectFree.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.FREE) }
        btnAspect1To1.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.SQUARE) }
        btnAspect3To4.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.RATIO_3_4) }
        btnAspect4To3.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.RATIO_4_3) }
        btnAspect9To16.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.RATIO_9_16) }
        btnAspect16To9.setOnClickListener { viewModel.onAspectRatioChanged(CropFrameGLSurfaceView.AspectRatio.RATIO_16_9) }

        // 取消和确认按钮
        btnCancel.setOnClickListener { viewModel.onCancelClicked() }
        btnConfirm.setOnClickListener { viewModel.onConfirmClicked() }

        // 裁剪框变化监听
        cropFrameViewGLSurfaceView.setOnCropRectChangeListener { rect ->
            viewModel.onCropRectChanged(rect)
        }

        // 滤镜控件监听
        setupFilterListeners()
    }

    /**
     * 设置滤镜控件监听器
     */
    private fun setupFilterListeners() {
        // 灰度开关
        switchGrayscale.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onGrayscaleChanged(isChecked)
        }

        // 对比度和饱和度滑块
        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return

                val value = progress / 100.0f
                when (seekBar?.id) {
                    R.id.seekbar_contrast -> viewModel.onContrastChanged(value)
                    R.id.seekbar_saturation -> viewModel.onSaturationChanged(value)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekbarContrast.setOnSeekBarChangeListener(seekBarListener)
        seekbarSaturation.setOnSeekBarChangeListener(seekBarListener)
    }

    /**
     * 观察ViewModel中的数据变化并更新UI
     */
    private fun observeViewModel() {
        // 观察UI状态
        viewModel.uiState.observe(this) { uiState ->
            updateUiForEditing(uiState.isEditing, uiState.isCropping, uiState.isFiltering, uiState.showFilterControls)
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

        // 观察裁剪比例变化
        viewModel.aspectRatio.observe(this) { aspectRatio ->
            cropFrameViewGLSurfaceView.setAspectRatio(aspectRatio)
            updateAspectRatioButtons(aspectRatio)
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
                cropAspectRatioSelector.visibility = View.VISIBLE

                // 初始化比例选择器按钮状态
                val currentRatio = cropFrameViewGLSurfaceView.getCurrentAspectRatio()
                updateAspectRatioButtons(currentRatio)
            } else if (isFiltering) {
                cropFrameViewGLSurfaceView.visibility = View.GONE
                filterControlsBar.visibility = View.VISIBLE
                cropAspectRatioSelector.visibility = View.GONE
            }
        } else {
            editorTopBar.visibility = View.VISIBLE
            editorActionsBar.visibility = View.GONE
            editorBottomTools.visibility = View.VISIBLE
            cropFrameViewGLSurfaceView.visibility = View.GONE
            filterControlsBar.visibility = View.GONE
            cropAspectRatioSelector.visibility = View.GONE
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

    /**
     * 更新比例选择器按钮的选中状态
     */
    private fun updateAspectRatioButtons(selectedRatio: CropFrameGLSurfaceView.AspectRatio) {
        // 重置所有按钮的选中状态
        btnAspectFree.isSelected = false
        btnAspect1To1.isSelected = false
        btnAspect3To4.isSelected = false
        btnAspect4To3.isSelected = false
        btnAspect9To16.isSelected = false
        btnAspect16To9.isSelected = false

        // 设置当前选中的按钮
        when (selectedRatio) {
            CropFrameGLSurfaceView.AspectRatio.FREE -> btnAspectFree.isSelected = true
            CropFrameGLSurfaceView.AspectRatio.SQUARE -> btnAspect1To1.isSelected = true
            CropFrameGLSurfaceView.AspectRatio.RATIO_3_4 -> btnAspect3To4.isSelected = true
            CropFrameGLSurfaceView.AspectRatio.RATIO_4_3 -> btnAspect4To3.isSelected = true
            CropFrameGLSurfaceView.AspectRatio.RATIO_9_16 -> btnAspect9To16.isSelected = true
            CropFrameGLSurfaceView.AspectRatio.RATIO_16_9 -> btnAspect16To9.isSelected = true
        }
    }
}