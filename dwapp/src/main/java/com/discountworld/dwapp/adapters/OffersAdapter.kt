package com.discountworld.dwapp.adapters

import android.view.LayoutInflater
import android.view.View
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
    private val isDealRedeemed: (Long) -> Boolean = { false },
    private val isOfferRedeemed: (String) -> Boolean = { false },
    private val onOfferClick: (RedemptionDealSummary?, Offer?) -> Unit
) : RecyclerView.Adapter<OffersAdapter.ViewHolder>() {

    private fun isDealNotRedeemable(deal: RedemptionDealSummary): Boolean {
        return !deal.isRedeemable || deal.isRedeemedToday || deal.isLimitReached || isDealRedeemed(deal.id)
    }

    private fun isOfferNotRedeemable(offer: Offer): Boolean {
        return offer.isRedeemed || isOfferRedeemed(offer.discount)
    }

    private val filteredDeals: List<RedemptionDealSummary> = dealsList.sortedWith(
        compareBy(
            { isDealNotRedeemable(it) },
            { it.sortOrder }
        )
    )

    private val filteredDummyOffers: List<Offer> = dummyOffers.sortedWith(
        compareBy { isOfferNotRedeemable(it) }
    )

    class ViewHolder(val binding: ItemOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.imgOffer.visibility = View.GONE

        if (filteredDeals.isNotEmpty()) {
            val deal = filteredDeals[position]
            val title = deal.title.ifEmpty { "Buy 1 Get 1" }
            val description = deal.description.ifEmpty { deal.title }

            val isNotRedeemable = isDealNotRedeemable(deal)

            holder.binding.tvDiscountAmount.text = title
            holder.binding.tvOfferDescription.text = description

            if (isNotRedeemable) {
                holder.binding.llDiscount.setBackgroundResource(R.drawable.bg_discount_gray)
                val clickListener = { _: View ->
                    val message = when {
                        deal.isRedeemedToday || isDealRedeemed(deal.id) -> "Offer already redeemed, refreshes at 12am"
                        deal.isLimitReached -> "Offer limit reached"
                        deal.lockReason.isNotBlank() -> deal.lockReason
                        else -> "Offer is currently not available"
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
                val clickListener = { _: View ->
                    Toast.makeText(holder.itemView.context, "Offer already redeemed", Toast.LENGTH_SHORT).show()
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
