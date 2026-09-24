package com.discountworld.dwapp.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionVendorSummary
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemDeliveryDealBinding
import com.discountworld.dwapp.utils.fixImageUrl

class DeliveryDealsAdapter(
    private var list: List<RedemptionVendorSummary> = emptyList(),
    private val onItemClick: ((RedemptionVendorSummary) -> Unit)? = null
) : RecyclerView.Adapter<DeliveryDealsAdapter.ViewHolder>() {

    private var forceHideInStore = false

    fun forceHideInStoreUi(hide: Boolean) {
        this.forceHideInStore = hide
    }

    class ViewHolder(val binding: ItemDeliveryDealBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeliveryDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        val title = item.title.ifEmpty { item.companyName }
        holder.binding.tvDealName.text = title

        val types = mutableListOf<String>()
        if (item.inStore && !forceHideInStore) types.add("In-Store")
        if (item.ecommerce) types.add("E-Commerce")
        if (item.delivery) types.add("Delivery")

        val dealType = if (types.isNotEmpty()) {
            types.joinToString(" & ")
        } else {
            item.categoriesList.firstOrNull()?.name ?: "Vendor"
        }
        holder.binding.tvDealCategory.text = dealType

        val bannerUrl = item.bannerUrl.fixImageUrl()
        if (!bannerUrl.isNullOrEmpty()) {
            holder.binding.shimmerBanner.visibility = View.VISIBLE
            holder.binding.shimmerBanner.startShimmer()

            Glide.with(holder.itemView.context)
                .load(bannerUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.binding.shimmerBanner.stopShimmer()
                        holder.binding.shimmerBanner.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.binding.shimmerBanner.stopShimmer()
                        holder.binding.shimmerBanner.visibility = View.GONE
                        return false
                    }
                })
                .into(holder.binding.ivDealBanner)
        } else {
            holder.binding.shimmerBanner.stopShimmer()
            holder.binding.shimmerBanner.visibility = View.GONE
            holder.binding.ivDealBanner.setImageResource(R.drawable.ic_placeholder)
        }

        Glide.with(holder.itemView.context)
            .load(item.logoUrl.fixImageUrl())
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.binding.ivDealLogo)

        holder.itemView.setOnClickListener {
            if (onItemClick != null) {
                onItemClick.invoke(item)
            } else {
                val bundle = Bundle().apply {
                    putLong("vendor_id", item.id)
                }
                it.findNavController().navigate(R.id.nav_brand_detail, bundle)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<RedemptionVendorSummary>) {
        list = newList
        notifyDataSetChanged()
    }
}
