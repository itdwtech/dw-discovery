package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemBannerBinding

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

        Glide.with(holder.itemView.context)
            .load(brand.logoUrl)
            .placeholder(com.discountworld.easypaisasdk.utils.getShimmerDrawable())
            .error(com.discountworld.easypaisasdk.R.drawable.dw_discovery_ic_banner)
            .into(holder.binding.imgBanner)

        holder.binding.root.setOnClickListener {
            onClick(brand)
        }
    }

    override fun getItemCount() = vendor.size
}
