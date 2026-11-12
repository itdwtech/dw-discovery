package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerBinding
import com.discountworld.easypaisasdk.utils.loadOptimizedImage
import com.discountworld.easypaisasdk.utils.toPx

class BrandAdapter(
    private val vendor: List<VendorSummary>,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<BrandAdapter.BrandViewHolder>() {

    inner class BrandViewHolder(val binding: DwDiscoveryItemBannerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        val binding = DwDiscoveryItemBannerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BrandViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BrandViewHolder, position: Int) {
        val brand = vendor[position]

        holder.binding.txtBanner.text = brand.companyName

        holder.binding.imgBanner.loadOptimizedImage(
            url = brand.logoUrl,
            fallbackWidthPx = 138.toPx(),
            fallbackHeightPx = 93.toPx(),
            priority = if (position < 4) Priority.HIGH else Priority.LOW,
            centerCrop = true,
            requestManager = Glide.with(holder.itemView.context)
        )

        holder.binding.root.setOnClickListener {
            onClick(brand)
        }
    }

    override fun getItemCount() = vendor.size
}
