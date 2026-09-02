package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionCity
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemCitySelectionBinding

class CitySelectionAdapter(
    private val cities: List<RedemptionCity>,
    private var selectedCityId: Long? = null,
    private val onCitySelected: (RedemptionCity) -> Unit
) : RecyclerView.Adapter<CitySelectionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCitySelectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCitySelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val city = cities[position]
        val context = holder.itemView.context

        holder.binding.tvCityName.text = city.name

        // Highlight selected city in app primary color (purple_primary)
        if (selectedCityId != null && city.id == selectedCityId) {
            holder.binding.tvCityName.setTextColor(ContextCompat.getColor(context, R.color.purple_primary))
        } else {
            holder.binding.tvCityName.setTextColor(ContextCompat.getColor(context, R.color.black))
        }

        holder.itemView.setOnClickListener {
            selectedCityId = city.id
            notifyDataSetChanged()
            onCitySelected(city)
        }
    }

    override fun getItemCount(): Int = cities.size
}
