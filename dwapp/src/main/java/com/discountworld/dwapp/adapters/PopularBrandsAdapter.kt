package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemPopularBrandBinding
import com.discountworld.dwapp.models.PopularBrand

class PopularBrandsAdapter(private val list: List<PopularBrand>) : RecyclerView.Adapter<PopularBrandsAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemPopularBrandBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPopularBrandBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivLogo.setImageResource(item.logo)
        holder.binding.tvBrandTitle.text = item.name
        holder.binding.tvCategory.text = item.category
    }

    override fun getItemCount(): Int = list.size
}
