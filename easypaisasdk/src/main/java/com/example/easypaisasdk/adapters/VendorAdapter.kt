package com.example.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.VendorSummary
import com.example.easypaisasdk.R
import com.example.easypaisasdk.databinding.ItemBannerCardBinding
import com.bumptech.glide.Glide
class VendorAdapter(
    private val vendors: List<VendorSummary>,
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

        holder.binding.txtTitle.text = vendor.title
        holder.binding.txtSubtitle.text = vendor.description
        //holder.binding.txtTerms.text = vendor.title

        Glide.with(holder.binding.root.context)
            .load(vendor.logoUrl)
            .placeholder(R.drawable.ic_banner)
            .into(holder.binding.imgBanner)

        holder.binding.root.setOnClickListener {
            onClick(vendor)
        }

//        if (vendor.featured) {
//            holder.binding.root.setBackgroundResource(R.drawable.bg_featured_vendor)
//        } else {
//            holder.binding.root.setBackgroundResource(R.drawable.bg_normal_vendor)
//        }
    }

    override fun getItemCount() = vendors.size
}