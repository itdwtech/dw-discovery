package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.OffersAdapter
import com.discountworld.dwapp.databinding.FragmentBrandDetailBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.Offer
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

class BrandDetailFragment : Fragment() {

    private var _binding: FragmentBrandDetailBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val vendorId = arguments?.getLong("vendor_id", -1L) ?: -1L
        val cityIdArg = arguments?.getLong("city_id", -1L) ?: -1L
        val selectedCityId = if (cityIdArg != -1L) cityIdArg else sessionManager.getSelectedCityId()

        if (vendorId != -1L) {
            loadVendorDetail(vendorId, selectedCityId)
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
    }

    private fun loadVendorDetail(vendorId: Long, cityId: Long?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val vendorDetail = redemptionRepository.getVendorDetail(vendorId, cityId)
            vendorDetail?.let {
                binding.tvBrandName.text = it.title.ifEmpty { it.companyName }
                if (it.categoriesList.isNotEmpty()) {
                    binding.tvCategory.text = it.categoriesList.joinToString(", ") { cat -> cat.name }
                }
                if (it.galleryImagesList.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.galleryImagesList.first())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(binding.ivBanner)
                } else if (it.logoUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.logoUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(binding.ivBanner)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvOffers.layoutManager = LinearLayoutManager(requireContext())
        val dummyOffers = listOf(
            Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
            Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
            Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
            Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1")
        )
        binding.rvOffers.adapter = OffersAdapter(dummyOffers)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
