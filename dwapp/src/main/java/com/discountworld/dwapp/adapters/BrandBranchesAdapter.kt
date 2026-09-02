package com.discountworld.dwapp.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.discount.RedemptionBranch
import com.discountworld.dwapp.databinding.ItemBrandBranchBinding

class BrandBranchesAdapter(
    private val branches: List<RedemptionBranch>,
    private val onBranchClick: (RedemptionBranch) -> Unit
) : RecyclerView.Adapter<BrandBranchesAdapter.BranchViewHolder>() {

    class BranchViewHolder(val binding: ItemBrandBranchBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchViewHolder {
        val binding = ItemBrandBranchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BranchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BranchViewHolder, position: Int) {
        val branch = branches[position]

        if (branch.name.isNotEmpty()) {
            holder.binding.tvBranchName.visibility = View.VISIBLE
            holder.binding.tvBranchName.text = branch.name
        } else {
            holder.binding.tvBranchName.visibility = View.GONE
        }

        holder.binding.tvBranchAddress.text = branch.address

        if (branch.phoneNumber.isNotEmpty()) {
            holder.binding.llBranchPhone.visibility = View.VISIBLE
            holder.binding.tvBranchPhone.text = branch.phoneNumber
        } else {
            holder.binding.llBranchPhone.visibility = View.GONE
        }

        // CALL button action
        holder.binding.btnCall.setOnClickListener {
            val phone = branch.phoneNumber.ifEmpty { "021111363636" }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            holder.itemView.context.startActivity(intent)
        }

        // DIRECTIONS button action
        holder.binding.btnDirections.setOnClickListener {
            val ctx = holder.itemView.context
            val lat = branch.latitude
            val lng = branch.longitude
            if (lat != 0.0 && lng != 0.0) {
                val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(branch.name)})")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(ctx.packageManager) != null) {
                    ctx.startActivity(mapIntent)
                } else {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                    ctx.startActivity(browserIntent)
                }
            } else {
                onBranchClick(branch)
            }
        }

        holder.itemView.setOnClickListener {
            onBranchClick(branch)
        }
    }

    override fun getItemCount(): Int = branches.size
}
