package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.City
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.ItemCityBinding

class CityAdapter(
    private val cities: List<City>,
    private val showImage: Boolean = true,
    private val onCityClick: (City) -> Unit
) : RecyclerView.Adapter<CityAdapter.CityViewHolder>() {

    inner class CityViewHolder(val binding: ItemCityBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val binding = ItemCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        val city = cities[position]
        holder.binding.txtCity.text = city.name

        if (showImage) {
            holder.binding.imgCity.visibility = View.VISIBLE

            // safely get first image (if exists)
            val image = city.imagesList?.getOrNull(0)?.imageUrl

            Glide.with(holder.binding.root.context)
                .load(image)
                .placeholder(R.drawable.ic_islamabad)
                .into(holder.binding.imgCity)
        } else {
            holder.binding.imgCity.visibility = View.GONE
        }

        holder.binding.root.setOnClickListener {
            onCityClick(city)
        }
    }

    override fun getItemCount() = cities.size
}
