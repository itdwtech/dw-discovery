package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.*
import com.discountworld.dwapp.databinding.FragmentHomeBinding
import com.discountworld.dwapp.models.PopularBrand
import com.discountworld.dwapp.models.TopPick
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupSlider()
    }

    private fun setupSlider() {
        val sliderImages = listOf(
            R.drawable.ic_placeholder,
            R.drawable.ic_placeholder,
            R.drawable.ic_placeholder
        )

        val adapter = SliderAdapter(sliderImages)
        binding.pager.adapter = adapter

        // Setup dots (TabLayout)
        TabLayoutMediator(binding.tabIndicator, binding.pager) { _, _ -> }.attach()

        // Auto-scroll logic
        sliderRunnable = Runnable {
            if (_binding != null) {
                val currentItem = binding.pager.currentItem
                val nextItem = if (currentItem == sliderImages.size - 1) 0 else currentItem + 1
                binding.pager.setCurrentItem(nextItem, true)
                sliderHandler.postDelayed(sliderRunnable, 3000) // 3 seconds delay
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.storyRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        // Horizontal Scroll for Banners
        binding.topBannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val bannerImages = listOf(R.drawable.ic_placeholder, R.drawable.ic_placeholder)
        binding.topBannerRV.adapter = BannerAdapter(bannerImages)

        // Top Picks For You
        binding.bannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val topPicks = listOf(
            TopPick(R.drawable.ic_placeholder, "14th Street Pizza"),
            TopPick(R.drawable.ic_placeholder, "Broadway Pizza"),
            TopPick(R.drawable.ic_placeholder, "Pizza Hut")
        )
        binding.bannerRV.adapter = TopPicksAdapter(topPicks)

        // Popular Brands
        binding.popularDiscRV.layoutManager = LinearLayoutManager(requireContext())
        val popularBrands = listOf(
            PopularBrand(R.drawable.ic_placeholder, "Big Bash - Phase 5", "Food"),
            PopularBrand(R.drawable.ic_placeholder, "Transfit Gym & Fitness - Clifton", "Fitness"),
            PopularBrand(R.drawable.ic_placeholder, "Pengs Salon Bukhari", "Salon & Spa"),
            PopularBrand(R.drawable.ic_placeholder, "Sindbad Extreme Bounce", "Leisure"),
            PopularBrand(R.drawable.ic_placeholder, "Mandi Al Khaleej - PECHS", "Food")
        )
        binding.popularDiscRV.adapter = PopularBrandsAdapter(popularBrands)
    }

    override fun onResume() {
        super.onResume()
        if (::sliderRunnable.isInitialized) {
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::sliderRunnable.isInitialized) {
            sliderHandler.removeCallbacks(sliderRunnable)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
