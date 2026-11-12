package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.utils.loadImageWithShimmer
import com.discountworld.discount.RedemptionCategory
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemHomeCategoryBinding
import com.discountworld.dwapp.utils.fixImageUrl

class HomeCategoryAdapter(
    private val categories: List<RedemptionCategory>,
    private val onCategoryClick: (RedemptionCategory) -> Unit
) : RecyclerView.Adapter<HomeCategoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.binding.tvCategoryName.text = category.name
        
        holder.binding.ivCategoryImage.loadImageWithShimmer(category.imageUrl)

        holder.itemView.setOnClickListener { onCategoryClick(category) }
    }

    override fun getItemCount(): Int = categories.size
}
