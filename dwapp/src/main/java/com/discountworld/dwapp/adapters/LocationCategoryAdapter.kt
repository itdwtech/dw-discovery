package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemLocationCategoryBinding

class LocationCategoryAdapter(private val categories: List<String>) : RecyclerView.Adapter<LocationCategoryAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemLocationCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.tvCategory.text = categories[position]
    }

    override fun getItemCount(): Int = categories.size
}
