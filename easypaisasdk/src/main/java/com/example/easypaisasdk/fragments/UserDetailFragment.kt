package com.example.easypaisasdk.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.discountworld.discovery.VendorDetail
import com.example.easypaisasdk.R
import com.example.easypaisasdk.adapters.CardsAdapter
import com.example.easypaisasdk.adapters.OutletsAdapter
import com.example.easypaisasdk.databinding.FragmentUserDetailBinding
import com.example.easypaisasdk.repositories.DetailRepository
import kotlinx.coroutines.launch

class UserDetailFragment : Fragment() {

    private var _binding: FragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = DetailRepository()
    private val args: UserDetailFragmentArgs by navArgs()
    private var terms :String = ""

    val outletsAdapter = OutletsAdapter(emptyList())
    val cardsAdapter = CardsAdapter(emptyList())
    // ✅ STORE VENDOR HERE
    private var vendorDetail: VendorDetail? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvOutlets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = outletsAdapter
        }

        binding.rvCards.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = cardsAdapter
        }

        // ✅ Load Vendor
        loadVendorDetail()

        // Outlets
        lifecycleScope.launch {
            val branches = repository.getListOfBranches(vendorId = args.vendorId)
            branches?.let {
                if (it.isNotEmpty()){
                    binding.tvOutlets.visibility = View.VISIBLE
                    outletsAdapter.updateData(it)
                }
                else binding.tvOutlets.visibility = View.GONE
            }
        }

        // Cards
        lifecycleScope.launch {
            val cardDiscounts = repository.getListOfCardDiscounts(vendorId = args.vendorId)
            cardDiscounts?.let {
                if(it.isNotEmpty()) {
                    binding.tvDiscountCard.visibility = View.VISIBLE
                    cardsAdapter.updateData(it)
                }
                else binding.tvDiscountCard.visibility = View.GONE
            }
        }
        
        binding.btnTerms.setOnClickListener {
            val terms = vendorDetail?.termsAndConditions ?: "No terms available"
            showTermsPopup(terms)
        }

        binding.back.setOnClickListener {
           requireActivity().onBackPressed()
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun loadVendorDetail() {
        lifecycleScope.launch {
            val vendor = repository.getVendor(args.vendorId, 1)

            vendor?.let {
                // ✅ SAVE DATA HERE
                vendorDetail = it

                binding.title.text = it.companyName
                binding.offerTitle.text = it.title
                binding.offerDesc.text = it.description
                val num = if(it.branchesCount == 0) "0" else String.format("%02d", it.branchesCount)
                binding.outletCount.text = "${num} Outlets"
                terms = it.termsAndConditions
                outletsAdapter.updateVendorUrl(it.logoUrl)
                // Load banner image
                Glide.with(requireContext())
                    .load(it.logoUrl)
                    .placeholder(R.drawable.ic_banner)
                    .into(binding.bannerImage)
            }
        }
    }

    private fun showTermsPopup(terms: String) {
        val dialog = Dialog(requireContext(), R.style.CenterDialogTheme)
        dialog.setContentView(R.layout.dialog_terms_conditions)

        val tvTerms = dialog.findViewById<TextView>(R.id.tvTerms)

        // Bullet formatting
        val formattedTerms = if (terms.contains("\n")) {
            "• " + terms.replace("\n", "\n• ")
        } else {
            "• $terms"
        }

        tvTerms.text = formattedTerms

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}