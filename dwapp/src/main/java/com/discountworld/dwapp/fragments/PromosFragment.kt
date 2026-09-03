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
import com.discountworld.discount.RedemptionBannerItem
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandLogosAdapter
import com.discountworld.dwapp.adapters.SliderAdapter
import com.discountworld.dwapp.adapters.TopPicksAdapter
import com.discountworld.dwapp.databinding.FragmentPromosBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.async
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

        setupFallbackSlider()
        setupFallbackDiscountsList()
        loadPromosDataParallel()
    }

    private fun loadPromosDataParallel() {
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedCityId = sessionManager.getSelectedCityId() ?: 1L

            // Execute Banners, Featured Vendors, and Stories API calls in PARALLEL
            val bannersDeferred = async { redemptionRepository.listBanners(selectedCityId) }
            val featuredVendorsDeferred = async { redemptionRepository.listVendors(page = 1, pageSize = 20, cityId = selectedCityId, featured = true) }
            val storiesDeferred = async { redemptionRepository.listStories(selectedCityId) }

            // 1. Process Banners for promoPager (ViewPager2)
            val bannerResponse = bannersDeferred.await()
            val bannerItems = bannerResponse?.bannersList ?: emptyList()
            if (bannerItems.isNotEmpty()) {
                setupBannersSlider(bannerItems, selectedCityId)
            }

            // 2. Process Featured Vendors for rvAmazingDiscounts (RecyclerView)
            val featuredVendors = featuredVendorsDeferred.await()?.vendorsList ?: emptyList()
            if (featuredVendors.isNotEmpty()) {
                binding.rvAmazingDiscounts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val adapter = TopPicksAdapter(vendorList = featuredVendors) { selectedVendor ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", selectedVendor.id)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_promos_to_nav_brand_detail, bundle)
                }
                binding.rvAmazingDiscounts.adapter = adapter
            }

            // 3. Process Stories for rvBrandLogos (RecyclerView)
            val stories = storiesDeferred.await() ?: emptyList()
            binding.rvBrandLogos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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
        val sliderImages = listOf(R.drawable.ic_arish_pk, R.drawable.ic_anamta_comfort, R.drawable.ic_almasjewellers)
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
            TopPick(R.drawable.ic_arish_pk, "Allure Beauty"),
            TopPick(R.drawable.ic_anamta_comfort, "Allure Beauty"),
            TopPick(R.drawable.ic_almasjewellers, "Allure Beauty")
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
