package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.adapter.BannerAdapter
import com.example.simpleeditingpictureapp.recyclerview.adapter.RecommendAdapter
import com.example.simpleeditingpictureapp.recyclerview.bean.RecommendBean
import com.bumptech.glide.request.RequestListener

class MainActivity : AppCompatActivity() {
    private val tag = "MainActivity"
    private lateinit var rvBanner: RecyclerView
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var llIndicator: LinearLayout
    private lateinit var mediaPreviewView: ImageView
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

    private val bannerData = listOf(
        R.drawable.img1,
        R.drawable.img2,
        R.drawable.img3,
        R.drawable.img4
    )

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

        bannerAdapter = BannerAdapter(this, bannerData)
        rvBanner.adapter = bannerAdapter

        rvBanner.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    // findFirstCompletelyVisibleItemPosition返回的就是0~getItemCount()-1的数
                    val centerPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
                    if (centerPosition != RecyclerView.NO_POSITION) {
                        updateIndicator(centerPosition % bannerData.size)
                    }
                }
            }
        })

        val initPosition = Int.MAX_VALUE / 2
        rvBanner.post {
            rvBanner.scrollToPosition(initPosition)
            updateIndicator(initPosition % bannerData.size)
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
        val indicatorSize = dp2px(8f)
        val margin = dp2px(4f)

        for (i in bannerData.indices) {
            val indicator = ImageView(this)
            indicator.layoutParams = LinearLayout.LayoutParams(indicatorSize, indicatorSize)
            (indicator.layoutParams as LinearLayout.LayoutParams).setMargins(margin, margin, margin, margin)
            indicator.setBackgroundResource(if (i == 0) R.drawable.shape_indicator_selected else R.drawable.shape_indicator_normal)
            llIndicator.addView(indicator)
        }
    }

    private fun dp2px(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
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
        setNavigationItemSelection(0)
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
        
        // 设置主页为选中状态
        setNavigationItemSelection(0)
        
        // 主页点击事件
        llHome.setOnClickListener {
            setNavigationItemSelection(0)
        }
        
        // 修图点击事件
        llEdit.setOnClickListener {
            setNavigationItemSelection(1)
            val intent = Intent(this@MainActivity, GalleryActivity::class.java)
            startActivity(intent)
        }
        
        // 我的点击事件
        llProfile.setOnClickListener {
            setNavigationItemSelection(2)
            val intent = Intent(this@MainActivity, ProfileActivity::class.java)
            startActivity(intent)
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
    
    private fun initRecommend() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvRecommend.layoutManager = layoutManager
        rvRecommend.setHasFixedSize(true)
        
        // 创建推荐数据
        val recommendData = listOf(
            RecommendBean(R.drawable.reconmend1, "AI 氛围感飘雪"),
            RecommendBean(R.drawable.reconmend2, "发丝发光氛围感"),
            RecommendBean(R.drawable.reconmend3, "无聊自拍秒出片"),
            RecommendBean(R.drawable.reconmend4, "废片自拍一键救回"),
            RecommendBean(R.drawable.reconmend5, "高级人像质感修图"),
            RecommendBean(R.drawable.reconmend6, "冬日飘雪氛围大片"),
            RecommendBean(R.drawable.reconmend7, "户外逆光发丝氛围"),
            RecommendBean(R.drawable.reconmend8, "窗边慵懒日常感")
        )
        
        recommendAdapter = RecommendAdapter(recommendData)
        rvRecommend.adapter = recommendAdapter
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}
