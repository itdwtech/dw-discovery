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
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.*
import com.discountworld.dwapp.databinding.DialogCitySelectionBinding
import com.discountworld.dwapp.databinding.FragmentHomeBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager
    private var selectedCityId: Long = 1L

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

        sessionManager = SessionManager(requireContext())

        setupRecyclerViews()
        setupSlider()
        setupSearch()

        binding.imgEcommerce.setOnClickListener {
            navigateToDelivery(showSearch = true, categoryName = "E-Commerce")
        }

        binding.imgDelivery.setOnClickListener {
            navigateToDelivery(showSearch = true, categoryName = "Delivery Deals")
        }

        binding.cities.setOnClickListener {
            android.util.Log.d("CityPopup", "Cities icon clicked")
            showCityPopup()
        }

        loadCategories()
        loadInitialCityAndStories()
    }

    private fun setupSearch() {
        binding.imgSearch.setOnClickListener {
            val query = binding.search.text?.toString()?.trim()
            if (!query.isNullOrEmpty()) {
                navigateToDelivery(showSearch = true, searchQuery = query)
            }
        }

        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.search.text?.toString()?.trim()
                if (!query.isNullOrEmpty()) {
                    navigateToDelivery(showSearch = true, searchQuery = query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun loadCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val categories = redemptionRepository.listCategories() ?: emptyList()

            if (categories.isNotEmpty()) {
                val adapter = HomeCategoryAdapter(categories) { category ->
                    navigateToDelivery(showSearch = true, categoryName = category.name, categoryId = category.id)
                }

                binding.categoryRV.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
                binding.categoryRV.adapter = adapter
            }
        }
    }

    private fun loadPopularVendors(cityId: Long = selectedCityId) {
        viewLifecycleOwner.lifecycleScope.launch {
            val response = redemptionRepository.listVendors(page = 1, pageSize = 20, cityId = cityId)
            val vendors = response?.vendorsList ?: emptyList()
            if (vendors.isNotEmpty()) {
                val adapter = PopularBrandsAdapter(vendors)
                binding.popularDiscRV.adapter = adapter
            }
        }
    }

    private fun loadInitialCityAndStories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val savedCityId = sessionManager.getSelectedCityId()
            if (savedCityId != null) {
                selectedCityId = savedCityId
            } else {
                val cities = redemptionRepository.listCities()
                val defaultCity = cities?.firstOrNull()
                if (defaultCity != null) {
                    selectedCityId = defaultCity.id
                    sessionManager.saveSelectedCityId(selectedCityId)
                }
            }
            loadStories(selectedCityId)
            loadPopularVendors(selectedCityId)
        }
    }

    private fun loadStories(cityId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.storyShimmerLayout.visibility = View.VISIBLE
            binding.storyShimmerLayout.startShimmer()
            binding.storyRV.visibility = View.GONE

            val stories = redemptionRepository.listStories(cityId) ?: emptyList()

            binding.storyShimmerLayout.stopShimmer()
            binding.storyShimmerLayout.visibility = View.GONE

            if (stories.isNotEmpty()) {
                binding.storyRV.visibility = View.VISIBLE
                val adapter = StoryAdapter(stories) { story ->
                    navigateToDelivery(showSearch = true, categoryName = story.vendorTitle)
                }
                binding.storyRV.adapter = adapter
            } else {
                binding.storyRV.visibility = View.GONE
            }
        }
    }

    private fun showCityPopup() {
        viewLifecycleOwner.lifecycleScope.launch {
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
                selectedCityId = city.id
                sessionManager.saveSelectedCityId(city.id)
                loadStories(city.id)
                loadPopularVendors(city.id)
                dialog.dismiss()
            }

            dialogBinding.rvCities.layoutManager = LinearLayoutManager(requireContext())
            dialogBinding.rvCities.adapter = adapter

            if (cities.size > 3) {
                val params = dialogBinding.rvCities.layoutParams
                params.height = (resources.displayMetrics.density * 180).toInt()
                dialogBinding.rvCities.layoutParams = params
            }

            dialogBinding.ivClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun navigateToDelivery(
        showSearch: Boolean = true,
        categoryName: String? = null,
        categoryId: Long? = null,
        searchQuery: String? = null
    ) {
        val bundle = Bundle().apply {
            putBoolean("showSearch", showSearch)
            putBoolean("hideBottomNav", true)
            putLong("cityId", selectedCityId)
            categoryName?.let { putString("categoryName", it) }
            categoryId?.let { putLong("categoryId", it) }
            searchQuery?.let { putString("searchQuery", it) }
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
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.storyRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Horizontal Scroll for Banners (Ecommerce & Delivery)
        binding.topBannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val bannerImages = listOf(R.drawable.ecommerce_title, R.drawable.delivery_title)
        binding.topBannerRV.adapter = BannerAdapter(bannerImages) { position ->
            if (position == 0) {
                navigateToDelivery(showSearch = true, categoryName = "E-Commerce")
            } else {
                navigateToDelivery(showSearch = true, categoryName = "Delivery Deals")
            }
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
