package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.Branch
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemOutletsBinding

class OutletsAdapter(private var list: List<Branch>) :
    RecyclerView.Adapter<OutletsAdapter.ViewHolder>() {

    private var brandUrl: String = ""

    inner class ViewHolder(val binding: DwDiscoveryItemOutletsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DwDiscoveryItemOutletsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = list[position]

        holder.binding.name = store.name
        holder.binding.address = store.address
        holder.binding.logoUrl = brandUrl
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Branch>) {
        list = newList
        notifyDataSetChanged()
    }

    fun updateVendorUrl(brandUrl: String) {
        this.brandUrl = brandUrl
        notifyDataSetChanged()
    }
}
