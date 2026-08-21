package com.discountworld.dwapp.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.*
import com.discountworld.dwapp.databinding.DialogCitySelectionBinding
import com.discountworld.dwapp.databinding.FragmentHomeBinding
import com.discountworld.dwapp.models.PopularBrand
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()

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

        binding.imgSearch.setOnClickListener {
            navigateToDelivery(showSearch = false)
        }

        binding.imgEcommerce.setOnClickListener {
            navigateToDelivery(showSearch = true)
        }

        binding.imgDelivery.setOnClickListener {
            navigateToDelivery(showSearch = true)
        }

        binding.cities.setOnClickListener {
            android.util.Log.d("CityPopup", "Cities icon clicked")
            showCityPopup()
        }

        loadCategories()
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = redemptionRepository.listCategories() ?: emptyList()
            
            if (categories.isNotEmpty()) {
                val adapter = HomeCategoryAdapter(categories) { category ->
                    navigateToDelivery(showSearch = true)
                }
                
                binding.categoryRV.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
                binding.categoryRV.adapter = adapter
            }
        }
    }

    private fun showCityPopup() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Show a simple toast to indicate loading if needed, or just fetch
            val cities = redemptionRepository.listCities()

            if (cities == null) {
                android.widget.Toast.makeText(requireContext(), "Failed to load cities", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (cities.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "No cities found", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            val dialogBinding = DialogCitySelectionBinding.inflate(layoutInflater)
            dialog.setContentView(dialogBinding.root)
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)

            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            val adapter = CitySelectionAdapter(cities) { city ->
                // Handle city selection here
                dialog.dismiss()
            }

            dialogBinding.rvCities.layoutManager = LinearLayoutManager(requireContext())
            dialogBinding.rvCities.adapter = adapter

            // Show scroll after 3 cities by fixing height
            if (cities.size > 3) {
                val params = dialogBinding.rvCities.layoutParams
                params.height = (resources.displayMetrics.density * 180).toInt() // Approx 3 items height
                dialogBinding.rvCities.layoutParams = params
            }

            dialogBinding.ivClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun navigateToDelivery(showSearch: Boolean) {
        val bundle = Bundle().apply {
            putBoolean("showSearch", showSearch)
            putBoolean("hideBottomNav", true)
        }
        findNavController().navigate(R.id.action_nav_home_to_nav_delivery, bundle)
    }

    private fun setupSlider() {
        val sliderImages = listOf(
            R.drawable.ic_almasjewellers,
            R.drawable.ic_beatsandcuts,
            R.drawable.ic_anamta_comfort
        )

        val adapter = SliderAdapter(sliderImages) {
            navigateToDelivery(showSearch = true)
        }
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
        val bannerImages = listOf(R.drawable.ic_almasjewellers, R.drawable.ic_beatsandcuts)
        binding.topBannerRV.adapter = BannerAdapter(bannerImages) {
            navigateToDelivery(showSearch = true)
        }

        // Top Picks For You
        binding.bannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val topPicks = listOf(
            TopPick(R.drawable.ic_almasjewellers, "14th Street Pizza"),
            TopPick(R.drawable.ic_beatsandcuts, "Broadway Pizza"),
            TopPick(R.drawable.ic_anamta_comfort, "Pizza Hut")
        )
        binding.bannerRV.adapter = TopPicksAdapter(topPicks)

        // Popular Brands
        binding.popularDiscRV.layoutManager = LinearLayoutManager(requireContext())
        val popularBrands = listOf(
            PopularBrand(R.drawable.ic_allurebeauty, "Big Bash - Phase 5", "Food"),
            PopularBrand(R.drawable.ic_allurebeauty, "Transfit Gym & Fitness - Clifton", "Fitness"),
            PopularBrand(R.drawable.ic_allurebeauty, "Pengs Salon Bukhari", "Salon & Spa"),
            PopularBrand(R.drawable.ic_allurebeauty, "Sindbad Extreme Bounce", "Leisure"),
            PopularBrand(R.drawable.ic_allurebeauty, "Mandi Al Khaleej - PECHS", "Food")
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
