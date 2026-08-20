package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemCitySelectionBinding
import com.discountworld.dwapp.models.City

class CitySelectionAdapter(
    private val cities: List<City>,
    private val onCitySelected: (City) -> Unit
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
        holder.binding.tvCityName.text = city.name
        holder.itemView.setOnClickListener { onCitySelected(city) }
    }

    override fun getItemCount(): Int = cities.size
}
