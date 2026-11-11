package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionBannerItem
import com.discountworld.dwapp.databinding.ItemSliderBinding
import com.discountworld.dwapp.utils.loadImageWithShimmer

class SliderAdapter(
    private val bannerItems: List<RedemptionBannerItem> = emptyList(),
    private val fallbackImages: List<Int> = emptyList(),
    private val onBannerClick: (RedemptionBannerItem?) -> Unit = {}
) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    class SliderViewHolder(val binding: ItemSliderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val binding = ItemSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SliderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        if (bannerItems.isNotEmpty()) {
            val item = bannerItems[position]
            holder.binding.ivSlider.loadImageWithShimmer(item.imageUrl)
            holder.itemView.setOnClickListener { onBannerClick(item) }
        } else if (fallbackImages.isNotEmpty()) {
            holder.binding.ivSlider.setImageResource(fallbackImages[position])
            holder.itemView.setOnClickListener { onBannerClick(null) }
        }
    }

    override fun getItemCount(): Int {
        return if (bannerItems.isNotEmpty()) bannerItems.size else fallbackImages.size
    }
}
