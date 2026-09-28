package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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

    private fun isTierEligible(userTier: String, requiredTier: String): Boolean {
        val uTier = userTier.trim().lowercase()
        val rTier = requiredTier.trim().lowercase()

        if (rTier.isEmpty()) return true

        return when (uTier) {
            "gold" -> true // Gold user can redeem all offers
            "silver" -> rTier != "gold" // Silver user cannot redeem Gold offers
            "bronze" -> rTier != "gold" && rTier != "silver" // Bronze user cannot redeem Silver or Gold offers
            else -> uTier == rTier
        }
    }

    private fun isDealNotRedeemable(deal: RedemptionDealSummary): Boolean {
        val dealTier = getDealTier(deal)
        val isEligible = isTierEligible(userTier, dealTier)
        return !isEligible || !deal.isRedeemable || deal.isRedeemedToday || deal.isLimitReached || isDealRedeemed(deal.id)
    }

    private fun isOfferNotRedeemable(offer: Offer): Boolean {
        val offerTier = getOfferTier(offer)
        val isEligible = isTierEligible(userTier, offerTier)
        return !isEligible || offer.isRedeemed || isOfferRedeemed(offer.discount)
    }

    private val filteredDeals: List<RedemptionDealSummary> = dealsList.sortedWith(
        compareBy(
            { isDealNotRedeemable(it) },
            { getTierPriority(getDealTier(it), userTier) },
            { it.sortOrder }
        )
    )

    private val filteredDummyOffers: List<Offer> = dummyOffers.sortedWith(
        compareBy(
            { isOfferNotRedeemable(it) },
            { getTierPriority(getOfferTier(it), userTier) }
        )
    )

    companion object {
        private fun getDealTier(deal: RedemptionDealSummary): String {
            if (deal.customerTier.isNotBlank()) return deal.customerTier
            val text = "${deal.title} ${deal.description} ${deal.customMessage1} ${deal.customMessage2}".lowercase()
            return when {
                text.contains("gold") -> "Gold"
                text.contains("silver") -> "Silver"
                text.contains("bronze") -> "Bronze"
                else -> ""
            }
        }

        private fun getOfferTier(offer: Offer): String {
            if (offer.tier.isNotBlank()) return offer.tier
            val text = "${offer.discount} ${offer.description}".lowercase()
            return when {
                text.contains("gold") -> "Gold"
                text.contains("silver") -> "Silver"
                text.contains("bronze") -> "Bronze"
                else -> ""
            }
        }

        private fun getTierPriority(dealTier: String, userTier: String): Int {
            val dTier = dealTier.trim().lowercase()
            val uTier = userTier.trim().lowercase()

            // Top priority (0) for deals matching user's active tier
            if ((dTier.isNotEmpty()) && (dTier == uTier)) return 0

            // Priority order for remaining deals
            return when (uTier) {
                "gold" -> when (dTier) {
                    "silver" -> 1
                    "bronze" -> 2
                    "" -> 3
                    else -> 4
                }
                "silver" -> when (dTier) {
                    "gold" -> 1
                    "bronze" -> 2
                    "" -> 3
                    else -> 4
                }
                "bronze" -> when (dTier) {
                    "silver" -> 1
                    "gold" -> 2
                    "" -> 3
                    else -> 4
                }
                else -> when (dTier) {
                    "gold" -> 1
                    "silver" -> 2
                    "bronze" -> 3
                    else -> 4
                }
            }
        }
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

            val isNotRedeemable = isDealNotRedeemable(deal)

            holder.binding.tvDiscountAmount.text = title
            holder.binding.tvOfferDescription.text = description

            if (isNotRedeemable) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                val clickListener = { _: android.view.View ->
                    val tier = getDealTier(deal)
                    val targetUser = tier.ifBlank { "this" }
                    val message = when {
                        !isTierEligible(userTier, tier) -> "Offer available for $targetUser user"
                        deal.isRedeemedToday || isDealRedeemed(deal.id) -> "Offer already redeemed refresh at 12am"
                        deal.isLimitReached -> "Offer limit reached"
                        deal.lockReason.isNotBlank() -> deal.lockReason
                        else -> "Offer available for $targetUser user"
                    }
                    Toast.makeText(holder.itemView.context, message, Toast.LENGTH_SHORT).show()
                }
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener(clickListener)
                holder.binding.llDiscount.isClickable = true
                holder.binding.llDiscount.setOnClickListener(clickListener)
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

            val isRedeemed = isOfferNotRedeemable(offer)

            holder.binding.tvDiscountAmount.text = offer.discount

            if (isRedeemed) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                val clickListener = { _: android.view.View ->
                    val tier = getOfferTier(offer)
                    val targetUser = tier.ifBlank { "this" }
                    Toast.makeText(holder.itemView.context, "Offer available for $targetUser user", Toast.LENGTH_SHORT).show()
                }
                holder.itemView.isClickable = true
                holder.itemView.setOnClickListener(clickListener)
                holder.binding.llDiscount.isClickable = true
                holder.binding.llDiscount.setOnClickListener(clickListener)
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

    override fun getItemCount(): Int {
        return if (filteredDeals.isNotEmpty()) filteredDeals.size else filteredDummyOffers.size
    }
}
