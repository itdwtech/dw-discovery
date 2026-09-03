package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionDealSummary
import com.discountworld.dwapp.databinding.ItemOfferBinding
import com.discountworld.dwapp.models.Offer

class OffersAdapter(
    private val dealsList: List<RedemptionDealSummary> = emptyList(),
    private val dummyOffers: List<Offer> = emptyList(),
    private val onOfferClick: (RedemptionDealSummary?, Offer?) -> Unit
) : RecyclerView.Adapter<OffersAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (dealsList.isNotEmpty()) {
            val deal = dealsList[position]
            val title = deal.title.ifEmpty { "Buy 1 Get 1" }
            val description = deal.description.ifEmpty { deal.title }

            holder.binding.tvDiscountAmount.text = title
            holder.binding.tvOfferDescription.text = description

            holder.itemView.setOnClickListener { onOfferClick(deal, null) }
        } else if (dummyOffers.isNotEmpty()) {
            val offer = dummyOffers[position]
            holder.binding.tvOfferDescription.text = offer.description
            holder.binding.tvDiscountAmount.text = offer.discount

            holder.itemView.setOnClickListener { onOfferClick(null, offer) }
        }
    }

    override fun getItemCount(): Int {
        return if (dealsList.isNotEmpty()) dealsList.size else dummyOffers.size
    }
}
