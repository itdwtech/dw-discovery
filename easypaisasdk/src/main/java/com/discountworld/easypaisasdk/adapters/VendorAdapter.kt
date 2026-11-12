package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.discountworld.discovery.Banner
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerCardBinding
import com.discountworld.easypaisasdk.utils.loadBannerImage

class VendorAdapter(
    vendors: List<VendorSummary>,
    private var banners: List<Banner>?,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    private val vendorsList: MutableList<VendorSummary> = vendors.toMutableList()

    fun addVendors(newVendors: List<VendorSummary>, newBanners: List<Banner>? = null) {
        if (!newBanners.isNullOrEmpty()) {
            banners = ((banners ?: emptyList()) + newBanners).distinctBy { it.vendorId to it.id }
        }
        val startPosition = vendorsList.size
        vendorsList.addAll(newVendors)
        notifyItemRangeInserted(startPosition, newVendors.size)
    }

    fun updateBanners(updatedBanners: List<Banner>) {
        val previousBanners = banners
        banners = updatedBanners
        vendorsList.forEachIndexed { index, vendor ->
            val oldUrl = previousBanners?.firstOrNull { it.vendorId == vendor.id }?.imageUrl
            val newUrl = updatedBanners.firstOrNull { it.vendorId == vendor.id }?.imageUrl
            if (oldUrl != newUrl) {
                notifyItemChanged(index)
            }
        }
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

        holder.binding.imgBanner.loadBannerImage(
            url = banner?.imageUrl,
            priority = if (position < 4) Priority.HIGH else Priority.LOW,
            requestManager = Glide.with(holder.binding.root.context)
        )
        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }
    }

    override fun getItemCount() = vendorsList.size
}
