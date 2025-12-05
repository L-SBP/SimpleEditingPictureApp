
package com.example.simpleeditingpictureapp.recyclerview.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.simpleeditingpictureapp.R

class RecommendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: ImageView = itemView.findViewById(R.id.iv_recommend)
    val textView: TextView = itemView.findViewById(R.id.tv_recommend)
}
