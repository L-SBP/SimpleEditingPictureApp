package com.example.simpleeditingpictureapp.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.bean.ImageBean
import com.example.simpleeditingpictureapp.recyclerview.viewholder.ImageViewHolder

class ImageAdapter(private val imageList: MutableList<ImageBean>) : RecyclerView.Adapter<ImageViewHolder>() {
    private var selectedPosition = -1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageBean = imageList[position]

        Glide.with(holder.itemView.context)
            .load(imageBean.imageUri)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        // 根据选中状态显示或隐藏勾选图标
        holder.checkView.visibility = if (imageBean.isSelected) View.VISIBLE else View.GONE

        // 为ImageView设置点击监听器
        holder.imageView.setOnClickListener {
            // 获取当前holder的实际位置
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) {
                return@setOnClickListener
            }

            // 如果点击的是已选中的项目，则取消选择
            if (currentPosition == selectedPosition) {
                imageList[currentPosition].isSelected = false
                selectedPosition = -1
                notifyItemChanged(currentPosition)
            } else {
                // 取消之前选中的项目
                if (selectedPosition >= 0 && selectedPosition < imageList.size) {
                    val previousSelected = imageList[selectedPosition]
                    previousSelected.isSelected = false
                    notifyItemChanged(selectedPosition)
                }

                // 设置新的选中项
                val currentSelected = imageList[currentPosition]
                currentSelected.isSelected = true
                selectedPosition = currentPosition

                // 更新UI
                notifyItemChanged(currentPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    fun getSelectedImage(): ImageBean? {
        for (imageBean in imageList) {
            if (imageBean.isSelected) {
                return imageBean
            }
        }
        return null
    }
}