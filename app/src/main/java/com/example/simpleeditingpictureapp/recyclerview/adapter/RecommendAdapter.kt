
package com.example.simpleeditingpictureapp.recyclerview.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.bean.RecommendBean
import com.example.simpleeditingpictureapp.recyclerview.viewholder.RecommendViewHolder

class RecommendAdapter(private val recommendList: List<RecommendBean>) : RecyclerView.Adapter<RecommendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recommend, parent, false)
        return RecommendViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendViewHolder, position: Int) {
        val recommendBean = recommendList[position]

        Glide.with(holder.itemView.context)
            .load(recommendBean.imageRes)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        holder.textView.text = recommendBean.title

        // 设置点击事件
        holder.itemView.setOnClickListener {
            // 这里可以添加点击事件处理
        }
    }

    override fun getItemCount(): Int {
        return recommendList.size
    }
}
