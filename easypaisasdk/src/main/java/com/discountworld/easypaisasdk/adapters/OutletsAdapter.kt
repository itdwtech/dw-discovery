package com.discountworld.easypaisasdk.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discovery.Branch
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemOutletsBinding

class OutletsAdapter(
    private var list: List<Branch>,
    private val onItemClick: ((Branch) -> Unit)? = null
) : RecyclerView.Adapter<OutletsAdapter.ViewHolder>() {

    private var brandUrl: String = ""
    private var webUrl: String = ""

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

        holder.binding.root.setOnClickListener {
            if (onItemClick != null) {
                onItemClick.invoke(store)
            } else {
                val urlToOpen = webUrl.trim()
                if (urlToOpen.isNotBlank()) {
                    val formattedUrl = if (!urlToOpen.startsWith("http://") && !urlToOpen.startsWith("https://")) {
                        "https://$urlToOpen"
                    } else {
                        urlToOpen
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                        holder.binding.root.context.startActivity(intent)
                    } catch (e: Exception) {
                        println("Error opening web link: ${e.message}")
                    }
                }
            }
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

    fun updateWebUrl(webUrl: String) {
        this.webUrl = webUrl
    }
}
