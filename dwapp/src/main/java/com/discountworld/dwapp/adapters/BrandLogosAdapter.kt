package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemBrandLogoBinding
import com.discountworld.dwapp.models.TopPick

class BrandLogosAdapter(private val list: List<TopPick>) : RecyclerView.Adapter<BrandLogosAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemBrandLogoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBrandLogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivLogo.setImageResource(item.image)
        holder.binding.tvLogoName.text = item.name
    }

    override fun getItemCount(): Int = list.size
}
