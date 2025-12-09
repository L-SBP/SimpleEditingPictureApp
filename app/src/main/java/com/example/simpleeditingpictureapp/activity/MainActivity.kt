package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.adapter.BannerAdapter
import com.example.simpleeditingpictureapp.recyclerview.adapter.RecommendAdapter
import com.example.simpleeditingpictureapp.recyclerview.bean.RecommendBean
import com.example.simpleeditingpictureapp.viewmodel.MainViewModel
import com.example.simpleeditingpictureapp.viewmodel.MainNavigationEvent
import com.bumptech.glide.request.RequestListener

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"

    // Banner相关
    private lateinit var rvBanner: RecyclerView
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var llIndicator: LinearLayout

    // GIF相关
    private lateinit var mediaPreviewView: ImageView

    // 推荐相关
    private lateinit var rvRecommend: RecyclerView
    private lateinit var recommendAdapter: RecommendAdapter

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
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(tag, "binding the view")
        rvBanner = findViewById(R.id.rv_banner)
        llIndicator = findViewById(R.id.ll_indicator)
        mediaPreviewView = findViewById(R.id.iv_media_preview)
        rvRecommend = findViewById(R.id.rv_recommend)

        // 初始化底部导航栏
        initBottomNavigation()

        // 观察ViewModel中的数据变化
        observeViewModel()

        // 先初始化Banner，确保视图已经正确加载
        Log.d(tag, "init the banner")
        initBanner()

        Log.d(tag, "init indicator")
        initIndicator()

        // 延迟初始化媒体预览，确保视图已经完全准备好
        rvBanner.postDelayed({
            Log.d(tag, "init media preview")
            initMediaPreview()
        }, 100)

        // 初始化推荐组件
        Log.d(tag, "init recommend")
        initRecommend()
    }

    private fun initBanner() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvBanner.layoutManager = layoutManager
        rvBanner.setHasFixedSize(true)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rvBanner)

        bannerAdapter = BannerAdapter(this, emptyList())
        rvBanner.adapter = bannerAdapter

        rvBanner.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    // findFirstCompletelyVisibleItemPosition返回的就是0~getItemCount()-1的数
                    val centerPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (centerPosition != RecyclerView.NO_POSITION) {
                        updateIndicator(centerPosition % (viewModel.bannerData.value?.size ?: 1))
                    }
                }
            }
        })

        val initPosition = Int.MAX_VALUE / 2
        rvBanner.post {
            rvBanner.scrollToPosition(initPosition)
            updateIndicator(initPosition % (viewModel.bannerData.value?.size ?: 1))
        }
    }

    private fun updateIndicator(selectedPosition: Int) {
        for (i in 0 until llIndicator.childCount) {
            val indicator = llIndicator.getChildAt(i)
            indicator.setBackgroundResource(if (i == selectedPosition) R.drawable.shape_indicator_selected else R.drawable.shape_indicator_normal)
        }
    }

    private fun initIndicator() {
        llIndicator.removeAllViews()
        val indicatorSize = viewModel.dp2px(8f)
        val margin = viewModel.dp2px(4f)

        val bannerCount = viewModel.bannerData.value?.size ?: 0
        for (i in 0 until bannerCount) {
            val indicator = ImageView(this)
            indicator.layoutParams = LinearLayout.LayoutParams(indicatorSize, indicatorSize)
            (indicator.layoutParams as LinearLayout.LayoutParams).setMargins(margin, margin, margin, margin)
            indicator.setBackgroundResource(if (i == 0) R.drawable.shape_indicator_selected else R.drawable.shape_indicator_normal)
            llIndicator.addView(indicator)
        }
    }

    private fun initMediaPreview() {
        Glide.with(this)
            .asGif()
            .load(R.drawable.sample_2)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<GifDrawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<GifDrawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    resource.setLoopCount(GifDrawable.LOOP_FOREVER)
                    resource.start()
                    return false
                }
            })
            .into(mediaPreviewView)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onNavigationItemClick(0)
    }

    override fun onPause() {
        super.onPause()
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

    private fun initRecommend() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRecommend.layoutManager = layoutManager
        rvRecommend.setHasFixedSize(true)

        recommendAdapter = RecommendAdapter(emptyList())
        rvRecommend.adapter = recommendAdapter
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察Banner数据，当然在我目前版本的app中不会更新banner，只有加载的时候会传递图片数据
        viewModel.bannerData.observe(this, Observer { data ->
            bannerAdapter.updateData(data)
            // 重新初始化指示器
            initIndicator()
        })

        // 观察推荐数据，其实也不会更新recommend，只有加载的时候会传递图片数据
        viewModel.recommendData.observe(this, Observer { data ->
            recommendAdapter.updateData(data)
        })

        // 观察选中的导航项
        viewModel.selectedNavigationItem.observe(this, Observer { position ->
            setNavigationItemSelection(position)
        })

        // 观察导航事件
        viewModel.navigationEvent.observe(this, Observer { event ->
            when (event) {
                is MainNavigationEvent.ToGallery -> {
                    val intent = Intent(this@MainActivity, GalleryActivity::class.java)
                    startActivity(intent)
                }
                is MainNavigationEvent.ToProfile -> {
                    val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                    startActivity(intent)
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
