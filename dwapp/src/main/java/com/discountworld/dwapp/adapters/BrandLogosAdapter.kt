package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionStory
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemBrandLogoBinding

class BrandLogosAdapter(
    private val stories: List<RedemptionStory>,
    private val onLogoClick: (RedemptionStory) -> Unit
) : RecyclerView.Adapter<BrandLogosAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBrandLogoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBrandLogoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = stories[position]
        holder.binding.tvLogoName.text = story.vendorTitle

        if (story.vendorLogoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(story.vendorLogoUrl)
                .placeholder(R.drawable.ic_allurebeauty)
                .error(R.drawable.ic_allurebeauty)
                .into(holder.binding.ivLogo)
        } else {
            holder.binding.ivLogo.setImageResource(R.drawable.ic_allurebeauty)
        }

        holder.itemView.setOnClickListener { onLogoClick(story) }
    }

    override fun getItemCount(): Int = stories.size
}
