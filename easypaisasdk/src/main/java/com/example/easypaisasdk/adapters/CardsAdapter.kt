package com.example.easypaisasdk.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discovery.CardDiscount
import com.example.easypaisasdk.databinding.ItemCardBinding
import com.example.easypaisasdk.models.Card

class CardsAdapter(private var list: List<CardDiscount>) :
    RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = list[position]
        holder.binding.apply {
            title.text = card.title
            description.text = card.description
            //terms.text = card.terms
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<CardDiscount>) {
        list = newList
        notifyDataSetChanged()
    }
}