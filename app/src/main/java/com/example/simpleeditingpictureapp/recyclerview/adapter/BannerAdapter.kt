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
    private var data: List<Int>
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private val tag = "BannerAdapter"
    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val gradientImageView: CustomImageView = itemView.findViewById(R.id.gradient_image_view)

        init {
            // 设置缩放模式：填充整个视图，不留空白
            gradientImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        Log.d("BannerAdapter", "创建ViewHolder")

        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_banner_image,
            parent,
            false
        )

        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val realPosition = position % data.size
        val imageResId = data[realPosition]

        Log.d("BannerAdapter", "绑定位置: $position, 资源ID: $imageResId")

        holder.gradientImageView.apply {
            setSweepEnabled(false)
            setGradientHeightRatio(0f)
            setSweepWidth(0f)
            setSweepSpeed(0f)
        }

        // 使用Glide加载图片到ImageView，使用FIT_XY模式
        Glide.with(holder.itemView.context)
            .load(imageResId)
            .dontTransform()
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
        Log.d(tag, "回收ViewHolder")
        holder.gradientImageView.apply {
            setSweepEnabled(false)
            clearAnimation()
        }
    }

    override fun getItemCount(): Int {
        return if (data.isEmpty()) 0 else Int.MAX_VALUE
    }

    /**
     * 更新数据
     * @param newData 新数据列表
     */
    fun updateData(newData: List<Int>) {
        data = newData
        notifyDataSetChanged()
    }
}