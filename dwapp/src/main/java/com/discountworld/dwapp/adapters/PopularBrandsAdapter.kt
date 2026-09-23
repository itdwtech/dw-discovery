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
import com.discountworld.dwapp.databinding.ItemPopularBrandBinding
import com.discountworld.dwapp.utils.fixImageUrl

class PopularBrandsAdapter(
    private var list: List<RedemptionVendorSummary> = emptyList(),
    private val onItemClick: ((RedemptionVendorSummary) -> Unit)? = null
) : RecyclerView.Adapter<PopularBrandsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPopularBrandBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPopularBrandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        val title = item.title.ifEmpty { item.companyName }
        holder.binding.tvBrandTitle.text = title

        val categoryName = if (item.categoriesList.isNotEmpty()) {
            item.categoriesList.joinToString(", ") { cat -> cat.name }
        } else if (item.shortDescription.isNotEmpty()) {
            item.shortDescription
        } else {
            val types = mutableListOf<String>()
            if (item.delivery) types.add("Delivery")
            if (item.inStore) types.add("In-Store")
            if (item.ecommerce) types.add("E-Commerce")
            if (types.isNotEmpty()) types.joinToString(" & ") else "Food"
        }
        holder.binding.tvCategory.text = categoryName

        val storeTypes = mutableListOf<String>()
        if (item.inStore) storeTypes.add("In-store")
        if (item.delivery) storeTypes.add("Delivery")
        if (item.ecommerce) storeTypes.add("E-commerce")

        if (storeTypes.isNotEmpty()) {
            holder.binding.btnInStore.visibility = View.VISIBLE
            holder.binding.btnInStore.text = storeTypes.joinToString(" & ")
        } else {
            holder.binding.btnInStore.visibility = View.GONE
        }

        val imageUrl = if (item.logoUrl.isNotEmpty()) item.logoUrl else item.bannerUrl
        Glide.with(holder.itemView.context)
            .load(imageUrl.fixImageUrl())
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.binding.ivLogo)

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

        holder.binding.btnInStore.setOnClickListener {
            holder.itemView.performClick()
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<RedemptionVendorSummary>) {
        list = newList
        notifyDataSetChanged()
    }
}
