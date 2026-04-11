package com.discountworld.easypaisasdk.utils

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("dw_discovery_imageUrl")
fun loadImage(view: ImageView, url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(url)
            .placeholder(view.drawable)
            .error(view.drawable)
            .centerCrop()
            .into(view)
    }
}
