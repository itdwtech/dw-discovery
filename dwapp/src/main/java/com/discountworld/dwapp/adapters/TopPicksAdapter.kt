package com.discountworld.dwapp.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.utils.createShimmerDrawable
import com.discountworld.dwapp.utils.loadImageWithShimmer
import com.discountworld.discount.RedemptionVendorSummary
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemTopPickBinding
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.utils.fixImageUrl

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

            holder.binding.ivTopPick.loadImageWithShimmer(vendor.bannerUrl)

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
            if (item.image != 0) {
                holder.binding.ivTopPick.setImageResource(item.image)
            } else {
                holder.binding.ivTopPick.setImageDrawable(createShimmerDrawable())
            }
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
