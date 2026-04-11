package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.Banner
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerCardBinding

class VendorAdapter(
    private val vendors: List<VendorSummary>,
    private val banners: List<Banner>?,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    inner class VendorViewHolder(val binding: DwDiscoveryItemBannerCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendorViewHolder {
        val binding = DwDiscoveryItemBannerCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VendorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VendorViewHolder, position: Int) {
        val vendor = vendors[position]
        val banner = banners?.firstOrNull { it.vendorId == vendor.id }

        holder.binding.title = vendor.title ?: ""
        holder.binding.subtitle = "with easypaisa premium debit card "
        holder.binding.imageUrl = banner?.imageUrl ?: vendor.logoUrl
        holder.binding.executePendingBindings()

        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }
    }

    override fun getItemCount() = vendors.size
}
