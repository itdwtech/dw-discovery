package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.VendorSummary
import com.discountworld.easypaisasdk.databinding.ItemBannerBinding

class BrandAdapter(
    private val vendor: List<VendorSummary>,
    private val onClick: (VendorSummary) -> Unit
) :
    RecyclerView.Adapter<BrandAdapter.BrandViewHolder>() {

    inner class BrandViewHolder(val binding: ItemBannerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrandViewHolder {
        val binding = ItemBannerBinding.inflate(
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
            .into(holder.binding.imgBanner)

        holder.binding.root.setOnClickListener {
            onClick(brand)
        }
    }

    override fun getItemCount() = vendor.size
}