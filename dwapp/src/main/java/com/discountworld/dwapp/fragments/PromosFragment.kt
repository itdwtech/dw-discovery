package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandLogosAdapter
import com.discountworld.dwapp.adapters.SliderAdapter
import com.discountworld.dwapp.adapters.TopPicksAdapter
import com.discountworld.dwapp.databinding.FragmentPromosBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class PromosFragment : Fragment() {

    private var _binding: FragmentPromosBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager

    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupPromoSlider()
        setupDiscountsList()
        setupLogosList()
    }

    private fun setupPromoSlider() {
        val sliderImages = listOf(R.drawable.ic_arish_pk, R.drawable.ic_anamta_comfort, R.drawable.ic_almasjewellers)
        binding.promoPager.adapter = SliderAdapter(fallbackImages = sliderImages)
        TabLayoutMediator(binding.promoTabIndicator, binding.promoPager) { _, _ -> }.attach()

        sliderRunnable = Runnable {
            if (_binding != null) {
                val currentItem = binding.promoPager.currentItem
                val nextItem = if (currentItem == sliderImages.size - 1) 0 else currentItem + 1
                binding.promoPager.setCurrentItem(nextItem, true)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private fun setupDiscountsList() {
        binding.rvAmazingDiscounts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val list = listOf(
            TopPick(R.drawable.ic_arish_pk, "Allure Beauty"),
            TopPick(R.drawable.ic_anamta_comfort, "Allure Beauty"),
            TopPick(R.drawable.ic_almasjewellers, "Allure Beauty")
        )
        // Reusing TopPicksAdapter as it matches the design (Banner + Brand Name)
        binding.rvAmazingDiscounts.adapter = TopPicksAdapter(fallbackList = list)
    }

    private fun setupLogosList() {
        binding.rvBrandLogos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedCityId = sessionManager.getSelectedCityId() ?: 1L
            val stories = redemptionRepository.listStories(selectedCityId) ?: emptyList()

            if (stories.isNotEmpty()) {
                binding.rvBrandLogos.visibility = View.VISIBLE
                binding.rvBrandLogos.adapter = BrandLogosAdapter(stories) { story ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", story.vendorId)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_promos_to_nav_brand_detail, bundle)
                }
            } else {
                binding.rvBrandLogos.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sliderHandler.removeCallbacks(sliderRunnable)
        _binding = null
    }
}
