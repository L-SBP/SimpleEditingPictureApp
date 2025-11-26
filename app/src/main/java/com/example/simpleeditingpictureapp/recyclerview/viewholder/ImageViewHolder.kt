package com.example.simpleeditingpictureapp.recyclerview.viewholder

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.simpleeditingpictureapp.R

class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: ImageView = itemView.findViewById(R.id.iv_thumb)
    val checkView: ImageView = itemView.findViewById(R.id.iv_check)
}