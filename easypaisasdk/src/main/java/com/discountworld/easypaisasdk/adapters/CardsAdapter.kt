package com.discountworld.easypaisasdk.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.CardDiscount
import com.discountworld.easypaisasdk.databinding.DwDiscoveryItemCardBinding

class CardsAdapter(private var list: List<CardDiscount>) :
    RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: DwDiscoveryItemCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = DwDiscoveryItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = list[position]

        val formattedDate = try {
            val rawDate = card.endDate.takeIf { it.isNotEmpty() }?.substring(0, 10) ?: ""
            val parsedDate = java.time.LocalDate.parse(rawDate)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            parsedDate.format(formatter)
        } catch (e: Exception) {
            card.endDate
        }

        holder.binding.cardTitle = card.title
        holder.binding.cardDate = formattedDate
        holder.binding.cardTerms = card.terms
        holder.binding.cardImageUrl = card.cardTypesList.firstOrNull()?.imageUrl?.takeIf { it.isNotEmpty() }
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<CardDiscount>) {
        list = newList
        notifyDataSetChanged()
    }
}
