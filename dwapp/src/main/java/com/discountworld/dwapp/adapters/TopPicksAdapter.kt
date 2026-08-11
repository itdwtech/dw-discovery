package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemTopPickBinding
import com.discountworld.dwapp.models.TopPick

class TopPicksAdapter(private val list: List<TopPick>) : RecyclerView.Adapter<TopPicksAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemTopPickBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopPickBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivTopPick.setImageResource(item.image)
        holder.binding.tvBrandName.text = item.name

        holder.itemView.setOnClickListener {
            it.findNavController().navigate(R.id.nav_brand_detail)
        }
    }

    override fun getItemCount(): Int = list.size
}
