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
        isTierEligible(userTier, deal.customerTier)
    }

    private val filteredDummyOffers: List<Offer> = dummyOffers.filter {
        isTierEligible(userTier, "")
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

            val isNotRedeemable = !deal.isRedeemable || deal.isRedeemedToday || deal.isLimitReached || isDealRedeemed(deal.id)

            holder.binding.tvDiscountAmount.text = title
            holder.binding.tvOfferDescription.text = description

            if (isNotRedeemable) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
                holder.binding.llDiscount.setOnClickListener(null)
                holder.binding.llDiscount.isClickable = false
            } else {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_purple)
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener {
                    onOfferClick(deal, null)
                }
                holder.binding.llDiscount.isClickable = true
                holder.binding.llDiscount.setOnClickListener {
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
                holder.binding.llDiscount.setOnClickListener(null)
                holder.binding.llDiscount.isClickable = false
            } else {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_purple)
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener {
                    onOfferClick(null, offer)
                }
                holder.binding.llDiscount.isClickable = true
                holder.binding.llDiscount.setOnClickListener {
                    onOfferClick(null, offer)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isTierEligible(userTier: String, requiredTier: String): Boolean {
        // Show all offers regardless of user tier
        return true
    }

    override fun getItemCount(): Int {
        return if (filteredDeals.isNotEmpty()) filteredDeals.size else filteredDummyOffers.size
    }
}
