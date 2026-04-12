package com.discountworld.easypaisasdk.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemCityBinding
import com.google.android.material.card.MaterialCardView

class CityAdapter(
    private val cities: List<City>,
    private var selectedCityId: Long? = null,
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
        holder.binding.txtCity.text = city.name

        // Highlight selected city with blue border
        val card = holder.binding.root as MaterialCardView
        if (city.id == selectedCityId) {
            card.strokeColor = Color.parseColor("#2196F3")
            card.strokeWidth = 4
        } else {
            card.strokeColor = Color.WHITE
            card.strokeWidth = 0
        }

        if (showImage) {
            holder.binding.imgCity.visibility = View.VISIBLE

            val image = city.imagesList?.getOrNull(0)?.imageUrl

            Glide.with(holder.binding.root.context)
                .load(image)
                .placeholder(R.drawable.dw_discovery_ic_islamabad)
                .into(holder.binding.imgCity)
        } else {
            holder.binding.imgCity.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            selectedCityId = city.id
            notifyDataSetChanged()
            onCityClick(city)
        }
    }

    fun setSelectedCity(cityId: Long?) {
        selectedCityId = cityId
        notifyDataSetChanged()
    }

    override fun getItemCount() = cities.size
}
