package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionDealSummary
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemOfferBinding
import com.discountworld.dwapp.models.Offer

class OffersAdapter(
    dealsList: List<RedemptionDealSummary> = emptyList(),
    dummyOffers: List<Offer> = emptyList(),
    private val userTier: String = "Gold",
    private val isDealRedeemed: (Long) -> Boolean = { false },
    private val isOfferRedeemed: (String) -> Boolean = { false },
    private val onOfferClick: (RedemptionDealSummary?, Offer?) -> Unit
) : RecyclerView.Adapter<OffersAdapter.ViewHolder>() {

    private val filteredDeals: List<RedemptionDealSummary> = dealsList.filter { deal ->
        val requiredTier = deal.customerTier
        isTierEligible(userTier, requiredTier)
    }

    private val filteredDummyOffers: List<Offer> = dummyOffers.filterIndexed { index, _ ->
        val requiredTier = when (index) {
            0 -> "Gold"
            1 -> "Silver"
            else -> "Bronze"
        }
        isTierEligible(userTier, requiredTier)
    }

    class ViewHolder(val binding: ItemOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (filteredDeals.isNotEmpty()) {
            val deal = filteredDeals[position]
            val title = deal.title.ifEmpty { "Buy 1 Get 1" }
            val description = deal.description.ifEmpty { deal.title }

            val isRedeemed = deal.isRedeemedToday || deal.isLimitReached || isDealRedeemed(deal.id)

            holder.binding.tvDiscountAmount.text = title
            holder.binding.tvOfferDescription.text = description

            if (isRedeemed) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
            } else {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_purple)
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener {
                    onOfferClick(deal, null)
                }
            }
        } else if (filteredDummyOffers.isNotEmpty()) {
            val offer = filteredDummyOffers[position]
            holder.binding.tvOfferDescription.text = offer.description

            val isRedeemed = offer.isRedeemed || isOfferRedeemed(offer.discount)

            holder.binding.tvDiscountAmount.text = offer.discount

            if (isRedeemed) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
            } else {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_purple)
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener {
                    onOfferClick(null, offer)
                }
            }
        }
    }

    private fun isTierEligible(userTier: String, requiredTier: String): Boolean {
        if (requiredTier.isEmpty()) return true
        val uTier = userTier.lowercase().trim()
        val rTier = requiredTier.lowercase().trim()

        val uRank = when (uTier) {
            "gold" -> 3
            "silver" -> 2
            "bronze" -> 1
            else -> 3
        }
        val rRank = when (rTier) {
            "gold" -> 3
            "silver" -> 2
            "bronze" -> 1
            else -> 1
        }
        return uRank >= rRank
    }

    override fun getItemCount(): Int {
        return if (filteredDeals.isNotEmpty()) filteredDeals.size else filteredDummyOffers.size
    }
}
