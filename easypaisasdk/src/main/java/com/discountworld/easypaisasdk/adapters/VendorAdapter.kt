package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.ItemBannerCardBinding
import com.bumptech.glide.Glide
import com.discountworld.discovery.Banner

class VendorAdapter(
    private val vendors: List<VendorSummary>,
    private val banners: List<Banner>?,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    inner class VendorViewHolder(val binding: ItemBannerCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val binding = ItemBannerCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VendorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {

        val vendor = vendors[position]

        // ✅ Match banner with vendorId
        val banner = banners?.firstOrNull { it.vendorId == vendor.id }

        // ✅ Vendor data
        holder.binding.txtTitle.text = vendor.title ?: ""
        holder.binding.txtSubtitle.text = "with easypaisa premium debit card "

        // ✅ Banner image (fallback to logo)
        Glide.with(holder.binding.root.context)
            .load(banner?.imageUrl ?: vendor.logoUrl)
            .placeholder(R.drawable.ic_banner)
            .error(R.drawable.ic_banner)
            .centerCrop()
            .into(holder.binding.imgBanner)

        // ✅ Click
        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }
    }

    override fun getItemCount() = vendors.size
}