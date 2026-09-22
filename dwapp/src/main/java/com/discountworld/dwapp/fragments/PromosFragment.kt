package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.discount.RedemptionBannerItem
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandLogosAdapter
import com.discountworld.dwapp.adapters.SliderAdapter
import com.discountworld.dwapp.adapters.TopPicksAdapter
import com.discountworld.dwapp.databinding.FragmentPromosBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.viewmodels.PromosViewModel
import com.google.android.material.tabs.TabLayoutMediator

class PromosFragment : Fragment() {

    private var _binding: FragmentPromosBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PromosViewModel by viewModels()
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

        setupFallbackSlider()
        setupFallbackDiscountsList()
        observeViewModel()

        val selectedCityId = sessionManager.getSelectedCityId() ?: 1L
        viewModel.loadPromosData(selectedCityId)
    }

    private fun observeViewModel() {
        val selectedCityId = sessionManager.getSelectedCityId() ?: 1L

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.banners.isNotEmpty()) {
                setupBannersSlider(state.banners, selectedCityId)
            }

            if (state.featuredVendors.isNotEmpty()) {
                binding.rvAmazingDiscounts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val adapter = TopPicksAdapter(vendorList = state.featuredVendors) { selectedVendor ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", selectedVendor.id)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_promos_to_nav_brand_detail, bundle)
                }
                binding.rvAmazingDiscounts.adapter = adapter
            }

            binding.rvBrandLogos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            if (state.stories.isNotEmpty()) {
                binding.rvBrandLogos.visibility = View.VISIBLE
                binding.rvBrandLogos.adapter = BrandLogosAdapter(state.stories) { story ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", story.vendorId)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_promos_to_nav_brand_detail, bundle)
                }
            } else if (!state.isLoading) {
                binding.rvBrandLogos.visibility = View.GONE
            }
        }
    }

    private fun setupBannersSlider(bannerItems: List<RedemptionBannerItem>, cityId: Long) {
        val adapter = SliderAdapter(bannerItems = bannerItems) { banner ->
            if (banner != null && banner.vendorId != 0L) {
                val bundle = Bundle().apply {
                    putLong("vendor_id", banner.vendorId)
                    putLong("city_id", cityId)
                }
                findNavController().navigate(R.id.action_nav_promos_to_nav_brand_detail, bundle)
            }
        }
        binding.promoPager.adapter = adapter
        TabLayoutMediator(binding.promoTabIndicator, binding.promoPager) { _, _ -> }.attach()

        startSliderAutoScroll(bannerItems.size)
    }

    private fun setupFallbackSlider() {
        val sliderImages = listOf(R.drawable.ic_placeholder, R.drawable.ic_placeholder, R.drawable.ic_placeholder)
        binding.promoPager.adapter = SliderAdapter(fallbackImages = sliderImages)
        TabLayoutMediator(binding.promoTabIndicator, binding.promoPager) { _, _ -> }.attach()

        startSliderAutoScroll(sliderImages.size)
    }

    private fun startSliderAutoScroll(itemCount: Int) {
        if (::sliderRunnable.isInitialized) {
            sliderHandler.removeCallbacks(sliderRunnable)
        }
        if (itemCount <= 0) return

        sliderRunnable = Runnable {
            if (_binding != null) {
                val currentItem = binding.promoPager.currentItem
                val nextItem = if (currentItem == itemCount - 1) 0 else currentItem + 1
                binding.promoPager.setCurrentItem(nextItem, true)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private fun setupFallbackDiscountsList() {
        binding.rvAmazingDiscounts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val list = listOf(
            TopPick(R.drawable.ic_placeholder, "Allure Beauty"),
            TopPick(R.drawable.ic_placeholder, "Allure Beauty"),
            TopPick(R.drawable.ic_placeholder, "Allure Beauty")
        )
        binding.rvAmazingDiscounts.adapter = TopPicksAdapter(fallbackList = list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::sliderRunnable.isInitialized) {
            sliderHandler.removeCallbacks(sliderRunnable)
        }
        _binding = null
    }
}
