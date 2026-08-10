package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemDeliveryDealBinding
import com.discountworld.dwapp.models.DeliveryDeal

class DeliveryDealsAdapter(private val list: List<DeliveryDeal>) : RecyclerView.Adapter<DeliveryDealsAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemDeliveryDealBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDeliveryDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivDealBanner.setImageResource(item.banner)
        holder.binding.ivDealLogo.setImageResource(item.logo)
        holder.binding.tvDealName.text = item.name
        holder.binding.tvDealCategory.text = item.category
    }

    override fun getItemCount(): Int = list.size
}
