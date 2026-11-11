package com.discountworld.dwapp.utils

import android.graphics.Color
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.discountworld.dwapp.R
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable

fun String?.fixImageUrl(): String? {
    if (this.isNullOrEmpty()) return this

    var fixedUrl = this.replace("localhost", "192.168.0.101")

    // Agar admin portal poora URL bhejne ke bajaye sirf relative path bhej raha ho (jaise "/images/pic.png")
    if (fixedUrl.startsWith("/")) {
        // Stage environment par point karein
        fixedUrl = "https://api.appbiance.com$fixedUrl"
    }

    // Is se aap Logcat mein dekh sakenge ke backend se URL kya aa raha hai aur hum kya bana rahe hain
    Log.d("ImageUtils", "Original: $this  --->  Fixed: $fixedUrl")

    return fixedUrl
}

fun createShimmerDrawable(): ShimmerDrawable {
    val shimmer = Shimmer.ColorHighlightBuilder()
        .setBaseColor(Color.parseColor("#E0E0E0"))
        .setHighlightColor(Color.parseColor("#F5F5F5"))
        .setDuration(1200)
        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
        .setAutoStart(true)
        .build()

    return ShimmerDrawable().apply {
        setShimmer(shimmer)
    }
}

fun ImageView.loadImageWithShimmer(
    url: String?,
    errorResId: Int = R.drawable.ic_placeholder
) {
    val fixedUrl = url.fixImageUrl()
    if (fixedUrl.isNullOrEmpty()) {
        this.setImageResource(errorResId)
        return
    }

    val shimmerDrawable = createShimmerDrawable()

    Glide.with(this.context)
        .load(fixedUrl)
        .placeholder(shimmerDrawable)
        .error(errorResId)
        .into(this)
}
