package com.example.simpleeditingpictureapp.model

import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.recyclerview.bean.RecommendBean

/**
 * 主页数据模型
 * 负责处理主页相关的数据
 */
class MainModel {

    /**
     * 获取Banner数据
     * @return Banner图片资源ID列表
     */
    fun getBannerData(): List<Int> {
        return listOf(
            R.drawable.img1,
            R.drawable.img2,
            R.drawable.img3,
            R.drawable.img4
        )
    }

    /**
     * 获取推荐数据
     * @return 推荐项列表
     */
    fun getRecommendData(): List<RecommendBean> {
        return listOf(
            RecommendBean(R.drawable.reconmend1, "AI 氛围感飘雪"),
            RecommendBean(R.drawable.reconmend2, "发丝发光氛围感"),
            RecommendBean(R.drawable.reconmend3, "无聊自拍秒出片"),
            RecommendBean(R.drawable.reconmend4, "废片自拍一键救回"),
            RecommendBean(R.drawable.reconmend5, "高级人像质感修图"),
            RecommendBean(R.drawable.reconmend6, "冬日飘雪氛围大片"),
            RecommendBean(R.drawable.reconmend7, "户外逆光发丝氛围"),
            RecommendBean(R.drawable.reconmend8, "窗边慵懒日常感")
        )
    }

    /**
     * 将dp转换为px
     * @param dp dp值
     * @param density 屏幕密度
     * @return px值
     */
    fun dp2px(dp: Float, density: Float): Int {
        return (dp * density + 0.5f).toInt()
    }
}
