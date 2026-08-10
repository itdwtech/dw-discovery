package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.databinding.ItemHistoryBinding
import com.discountworld.dwapp.models.HistoryItem

class HistoryAdapter(private val list: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.ivLogo.setImageResource(item.logo)
        holder.binding.tvTitle.text = item.title
        holder.binding.tvCode.text = item.code
        holder.binding.tvDateTime.text = item.dateTime
    }

    override fun getItemCount(): Int = list.size
}
