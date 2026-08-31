package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionStory
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemStoryBinding

class StoryAdapter(
    private val stories: List<RedemptionStory>,
    private val onStoryClick: (RedemptionStory) -> Unit
) : RecyclerView.Adapter<StoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val story = stories[position]
        holder.binding.tvStoryTitle.text = story.vendorTitle

        Glide.with(holder.itemView.context)
            .load(story.vendorLogoUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.binding.ivStoryLogo)

        holder.itemView.setOnClickListener { onStoryClick(story) }
    }

    override fun getItemCount(): Int = stories.size
}
