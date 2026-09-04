package com.discountworld.dwapp.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.discountworld.discount.CustomerRedemptionItem
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(private var list: List<CustomerRedemptionItem> = emptyList()) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        Log.d("HistoryAdapter", "Binding item at pos $position: dealTitle='${item.dealTitle}', vendorTitle='${item.vendorTitle}', code='${item.redemptionCode}'")

        holder.binding.tvTitle.text = item.dealTitle.ifEmpty { item.vendorTitle }
        holder.binding.tvCode.text = item.redemptionCode

        val timestamp = item.redeemedAt
        if (timestamp.seconds > 0) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            holder.binding.tvDateTime.text = sdf.format(Date(timestamp.seconds * 1000))
        } else {
            holder.binding.tvDateTime.text = ""
        }

        val imageUrl = item.vendorLogoUrl.ifEmpty { null }?.replace("localhost", "192.168.0.101")

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_placeholder)
            .error(R.drawable.ic_placeholder)
            .into(holder.binding.ivLogo)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Redemption Code", item.redemptionCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Code copied: ${item.redemptionCode}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        Log.d("HistoryAdapter", "getItemCount: ${list.size}")
        return list.size
    }

    fun updateData(newList: List<CustomerRedemptionItem>) {
        Log.d("HistoryAdapter", "updateData called with ${newList.size} items")
        list = newList
        notifyDataSetChanged()
    }
}
