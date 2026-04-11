package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemCityBinding

class CityAdapter(
    private val cities: List<City>,
    private val showImage: Boolean = true,
    private val onCityClick: (City) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    inner class CityViewHolder(val binding: DwDiscoveryItemCityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = DwDiscoveryItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]

        holder.binding.name = city.name
        holder.binding.showImage = showImage
        holder.binding.imageUrl = city.imagesList?.getOrNull(0)?.imageUrl
        holder.binding.executePendingBindings()

        holder.binding.root.setOnClickListener {
            onCityClick(city)
        }
    }

    override fun getItemCount() = cities.size
}
