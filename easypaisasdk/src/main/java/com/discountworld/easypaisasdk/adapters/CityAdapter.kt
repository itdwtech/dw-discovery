package com.discountworld.easypaisasdk.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemCityBinding
import com.discountworld.easypaisasdk.utils.loadOptimizedImage
import com.discountworld.easypaisasdk.utils.toPx
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

        // Highlight selected city with green border
        val card = holder.binding.root as MaterialCardView
        if (city.id == selectedCityId) {
            card.strokeColor = Color.parseColor("#00A859")
            card.strokeWidth = 4
        } else {
            card.strokeColor = Color.WHITE
            card.strokeWidth = 0
        }

        if (showImage) {
            holder.binding.imgCity.visibility = View.VISIBLE

            val image = city.imagesList?.getOrNull(0)?.imageUrl

            holder.binding.imgCity.loadOptimizedImage(
                url = image,
                fallbackWidthPx = 90.toPx(),
                fallbackHeightPx = 60.toPx(),
                priority = if (position < 4) Priority.HIGH else Priority.LOW,
                centerCrop = true,
                errorResId = R.drawable.dw_discovery_ic_islamabad,
                requestManager = Glide.with(holder.binding.root.context)
            )
        } else {
            holder.binding.imgCity.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            setSelectedCity(city.id)
            onCityClick(city)
        }
    }

    fun setSelectedCity(cityId: Long?) {
        val oldId = selectedCityId
        if (oldId == cityId) return
        selectedCityId = cityId
        val oldIndex = cities.indexOfFirst { it.id == oldId }
        val newIndex = cities.indexOfFirst { it.id == cityId }
        if (oldIndex >= 0) notifyItemChanged(oldIndex)
        if (newIndex >= 0) notifyItemChanged(newIndex)
    }

    override fun getItemCount() = cities.size
}
