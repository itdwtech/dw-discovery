package com.discountworld.dwapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionCategory
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemLocationCategoryBinding

class LocationCategoryAdapter(
    private val categories: List<RedemptionCategory>,
    private val onCategorySelected: (RedemptionCategory?) -> Unit
) : RecyclerView.Adapter<LocationCategoryAdapter.ViewHolder>() {

    private var selectedPosition = 0

    class ViewHolder(val binding: ItemLocationCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val isAll = position == 0
        val isSelected = position == selectedPosition

        val categoryName = if (isAll) "All" else categories[position - 1].name
        holder.binding.tvCategory.text = categoryName

        if (isSelected) {
            holder.binding.tvCategory.setBackgroundResource(R.drawable.bg_category_selected)
            holder.binding.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
        } else {
            holder.binding.tvCategory.setBackgroundResource(R.drawable.bg_category_unselected)
            holder.binding.tvCategory.setTextColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            val currentPos = holder.bindingAdapterPosition
            if (currentPos != RecyclerView.NO_POSITION && selectedPosition != currentPos) {
                val previousPosition = selectedPosition
                selectedPosition = currentPos
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)

                val selectedCategory = if (currentPos == 0) null else categories[currentPos - 1]
                onCategorySelected(selectedCategory)
            }
        }
    }

    override fun getItemCount(): Int = categories.size + 1
}
