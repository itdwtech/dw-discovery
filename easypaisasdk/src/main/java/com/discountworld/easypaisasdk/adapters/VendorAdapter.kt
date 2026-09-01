package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerCardBinding
import com.bumptech.glide.Glide
import com.discountworld.discovery.Banner

class VendorAdapter(
    vendors: List<VendorSummary>,
    private val banners: List<Banner>?,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    private val vendorsList: MutableList<VendorSummary> = vendors.toMutableList()

    fun addVendors(newVendors: List<VendorSummary>) {
        val startPosition = vendorsList.size
        vendorsList.addAll(newVendors)
        notifyItemRangeInserted(startPosition, newVendors.size)
    }

    fun updateVendors(newVendors: List<VendorSummary>) {
        vendorsList.clear()
        vendorsList.addAll(newVendors)
        notifyDataSetChanged()
    }

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
        val vendor = vendorsList[position]
        val banner = banners?.firstOrNull { it.vendorId == vendor.id }

        holder.binding.txtTitle.text = vendor.title ?: ""
        holder.binding.txtSubtitle.text = vendor.shortDescription ?: ""

        Glide.with(holder.binding.root.context)
            .load(banner?.imageUrl)
            .placeholder(com.discountworld.easypaisasdk.utils.getShimmerDrawable())
            .error(R.drawable.dw_discovery_ic_banner)
            .centerCrop()
            .into(holder.binding.imgBanner)
        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }
    }

    override fun getItemCount() = vendorsList.size
}
