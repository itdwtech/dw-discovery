package com.discountworld.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.Branch
import com.discountworld.easypaisasdk.R
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
        with(holder.binding) {
            tvName.text = store.name
            tvAddress.text = store.address
            Glide.with(holder.binding.root.context)
                .load(brandUrl)
                .placeholder(R.drawable.dw_discovery_ic_outlets)
                .into(imgLogo)
        }
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
