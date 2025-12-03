package com.example.simpleeditingpictureapp.recyclerview.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.widget.CustomImageView

class BannerAdapter(
    private val context: Context,
    private val data: List<Int>
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    // 记录每个位置应该加载的图片
    private val positionToResId = mutableMapOf<Int, Int>()

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradientImageView: CustomImageView = itemView.findViewById(R.id.gradient_image_view)
        var currentPosition = -1

        init {
            // 设置缩放模式：填充整个视图，不留空白
            gradientImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        Log.d("BannerAdapter", "创建ViewHolder")

        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_banner_gradient_clip_image,
            parent,
            false
        )

        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val realPosition = position % data.size
        val imageResId = data[realPosition]

        Log.d("BannerAdapter", "绑定位置: $position, 资源ID: $imageResId")

        // 检查是否需要重新加载
        if (holder.currentPosition == position) {
            Log.d("BannerAdapter", "位置相同，跳过加载")
            return
        }

        holder.currentPosition = position
        positionToResId[position] = imageResId

        // 使用Glide加载图片到ImageView，使用FIT_XY模式
        Glide.with(context)
            .load(imageResId)
            .dontTransform()  // 不进行任何变换，让ImageView的scaleType处理
            .into(holder.gradientImageView)
            
        // 根据位置决定使用哪种效果
        when (realPosition % 2) {
            0 -> {
                // 偶数位置使用扫光效果
                holder.gradientImageView.setSweepEnabled(true)
                holder.gradientImageView.setSweepWidth(200f) // 增加扫光宽度
                holder.gradientImageView.setSweepSpeed(2500f) // 加快扫光速度
                holder.gradientImageView.setGradientHeightRatio(0f) // 不显示底部渐变
            }
            1 -> {
                // 奇数位置使用底部渐变效果
                holder.gradientImageView.setSweepEnabled(false) // 关闭扫光效果
                holder.gradientImageView.setGradientHeightRatio(0.4f) // 进一步增加渐变高度，使其更明显
            }
        }
    }

    override fun onViewRecycled(holder: BannerViewHolder) {
        super.onViewRecycled(holder)
        Log.d("BannerAdapter", "视图被回收: position=${holder.currentPosition}")

        // 清理Glide请求
        Glide.with(context).clear(holder.gradientImageView)
        holder.currentPosition = -1
    }

    override fun getItemCount(): Int {
        return if (data.isEmpty()) 0 else 100
    }
}