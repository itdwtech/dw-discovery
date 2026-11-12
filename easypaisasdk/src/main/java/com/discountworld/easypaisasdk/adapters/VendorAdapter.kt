package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerCardBinding
import android.util.Log
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.discountworld.discovery.Banner
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.repositories.HomeRepository

class VendorAdapter(
    vendors: List<VendorSummary>,
    banners: List<Banner>?,
    private val onClick: (VendorSummary) -> Unit
) : RecyclerView.Adapter<VendorAdapter.VendorViewHolder>() {

    private val vendorsList: MutableList<VendorSummary> = vendors.toMutableList()

    // O(1) lookup instead of scanning the whole banner list on every bind
    private val bannerByVendor: Map<Long, String> = banners.orEmpty()
        .filter { it.imageUrl.isNotBlank() }
        .associate { it.vendorId to it.imageUrl }

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
        // Vendor's own banner (from SearchVendors) -> global banner list -> logo
        val imageUrl = HomeRepository.vendorBannerUrls[vendor.id]
            ?: bannerByVendor[vendor.id]
            ?: vendor.logoUrl.takeIf { it.isNotBlank() }

        holder.binding.txtTitle.text = vendor.title ?: ""
        holder.binding.txtSubtitle.text = vendor.shortDescription ?: ""

        Glide.with(holder.binding.imgBanner)
            .load(imageUrl)
            .placeholder(com.discountworld.easypaisasdk.utils.getShimmerDrawable())
            .error(R.color.dw_discovery_dim_gray)
            .fallback(R.color.dw_discovery_dim_gray)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .priority(if (position < 3) Priority.HIGH else Priority.NORMAL)
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
                ): Boolean {
                    Log.e("VendorAdapter", "Image load failed: $model", e)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean = false
            })
            .into(holder.binding.imgBanner)
        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }
    }

    override fun getItemCount() = vendorsList.size
}
