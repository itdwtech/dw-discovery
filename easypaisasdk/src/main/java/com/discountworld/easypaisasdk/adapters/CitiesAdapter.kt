package com.discountworld.easypaisasdk.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemCitiesBinding

class CitiesAdapter(
    private val cities: List<City>,
    private val selectedCityId: Long?,
    private val onCityClick: (City) -> Unit
) : RecyclerView.Adapter<CitiesAdapter.CityViewHolder>() {

    inner class CityViewHolder(val binding: DwDiscoveryItemCitiesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = DwDiscoveryItemCitiesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.binding.txtCity.text = city.name

        if (city.id == selectedCityId) {
            holder.binding.txtCity.setTextColor(Color.parseColor("#2196F3"))
            holder.binding.txtCity.setTypeface(null, Typeface.BOLD)
        } else {
            holder.binding.txtCity.setTextColor(Color.BLACK)
            holder.binding.txtCity.setTypeface(null, Typeface.NORMAL)
        }

        holder.binding.root.setOnClickListener {
            onCityClick(city)
        }
    }

    override fun getItemCount() = cities.size
}
