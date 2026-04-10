package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.databinding.ItemCitiesBinding

class CitiesAdapter(
    private val cities: List<City>,
    private val onCityClick: (City) -> Unit
) : RecyclerView.Adapter<CitiesAdapter.CityViewHolder>() {

    inner class CityViewHolder(val binding: ItemCitiesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCitiesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.binding.txtCity.text = city.name

        holder.binding.root.setOnClickListener {
            onCityClick(city)
        }
    }

    override fun getItemCount() = cities.size
}
