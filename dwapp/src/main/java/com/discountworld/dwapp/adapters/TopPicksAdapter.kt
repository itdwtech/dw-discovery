package com.discountworld.dwapp.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionVendorSummary
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemTopPickBinding
import com.discountworld.dwapp.models.TopPick

class TopPicksAdapter(
    private val vendorList: List<RedemptionVendorSummary> = emptyList(),
    private val fallbackList: List<TopPick> = emptyList(),
    private val onItemClick: ((RedemptionVendorSummary) -> Unit)? = null
) : RecyclerView.Adapter<TopPicksAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTopPickBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (vendorList.isNotEmpty()) {
            val vendor = vendorList[position]
            val title = vendor.title.ifEmpty { vendor.companyName }
            holder.binding.tvBrandName.text = title

            val imageUrl = if (vendor.bannerUrl.isNotEmpty()) vendor.bannerUrl else vendor.logoUrl
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(holder.binding.ivTopPick)

            holder.itemView.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick.invoke(vendor)
                } else {
                    val bundle = Bundle().apply {
                        putLong("vendor_id", vendor.id)
                    }
                    it.findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail, bundle)
                }
            }
        } else if (fallbackList.isNotEmpty()) {
            val item = fallbackList[position]
            holder.binding.ivTopPick.setImageResource(item.image)
            holder.binding.tvBrandName.text = item.name

            holder.itemView.setOnClickListener {
                it.findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail)
            }
        }
    }

    override fun getItemCount(): Int {
        return if (vendorList.isNotEmpty()) vendorList.size else fallbackList.size
    }
}
