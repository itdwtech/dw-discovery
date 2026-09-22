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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.discount.RedemptionBannerItem
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.*
import com.discountworld.dwapp.databinding.DialogCitySelectionBinding
import com.discountworld.dwapp.databinding.FragmentHomeBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.TopPick
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.discountworld.dwapp.viewmodels.HomeViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
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
        setupFallbackSlider()
        setupSearch()
        observeViewModel()

        binding.imgEcommerce.setOnClickListener {
            navigateToDelivery(showSearch = true, categoryName = "E-Commerce")
        }

        binding.imgDelivery.setOnClickListener {
            navigateToDelivery(showSearch = true, categoryName = "Delivery Deals")
        }

        binding.cities.setOnClickListener {
            showCityPopup()
        }

        initHomeData()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) {
                binding.storyShimmerLayout.visibility = View.VISIBLE
                binding.storyShimmerLayout.startShimmer()
                binding.storyRV.visibility = View.GONE
            } else {
                binding.storyShimmerLayout.stopShimmer()
                binding.storyShimmerLayout.visibility = View.GONE
            }

            if (state.stories.isNotEmpty()) {
                binding.storyRV.visibility = View.VISIBLE
                val adapter = StoryAdapter(state.stories) { story ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", story.vendorId)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail, bundle)
                }
                binding.storyRV.adapter = adapter
                startStoryAutoScroll(state.stories.size)
            } else if (!state.isLoading) {
                binding.storyRV.visibility = View.GONE
            }

            if (state.featuredVendors.isNotEmpty()) {
                val adapter = TopPicksAdapter(vendorList = state.featuredVendors) { selectedVendor ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", selectedVendor.id)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail, bundle)
                }
                binding.bannerRV.adapter = adapter
            }

            if (state.banners.isNotEmpty()) {
                setupBannersSlider(state.banners)
            }

            if (state.popularVendors.isNotEmpty()) {
                val adapter = PopularBrandsAdapter(state.popularVendors) { selectedVendor ->
                    val bundle = Bundle().apply {
                        putLong("vendor_id", selectedVendor.id)
                        putLong("city_id", selectedCityId)
                    }
                    findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail, bundle)
                }
                binding.popularDiscRV.adapter = adapter
            }
        }
    }

    private fun initHomeData() {
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

            // Load categories
            val rawCategories = redemptionRepository.listCategories() ?: emptyList()
            if (rawCategories.isNotEmpty()) {
                val sortedCategories = rawCategories.sortedBy { it.sortOrder }
                val adapter = HomeCategoryAdapter(sortedCategories) { category ->
                    navigateToDelivery(showSearch = true, categoryName = category.name, categoryId = category.id)
                }
                binding.categoryRV.layoutManager = GridLayoutManager(requireContext(), 4)
                binding.categoryRV.adapter = adapter
            }

            viewModel.loadHomeData(selectedCityId)
        }
    }

    private fun setupSearch() {
        binding.imgSearch.setOnClickListener {
            val query = binding.search.text?.toString()?.trim()
            navigateToDelivery(
                showSearch = true,
                categoryName = "Search Results",
                searchQuery = if (!query.isNullOrEmpty()) query else null
            )
        }

        binding.search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.search.text?.toString()?.trim()
                navigateToDelivery(
                    showSearch = true,
                    categoryName = "Search Results",
                    searchQuery = if (!query.isNullOrEmpty()) query else null
                )
                true
            } else {
                false
            }
        }
    }

    private fun setupBannersSlider(bannerItems: List<RedemptionBannerItem>) {
        val adapter = SliderAdapter(bannerItems = bannerItems) { banner ->
            if (banner != null && banner.vendorId != 0L) {
                val bundle = Bundle().apply {
                    putLong("vendor_id", banner.vendorId)
                    putLong("city_id", selectedCityId)
                }
                findNavController().navigate(R.id.action_nav_home_to_nav_brand_detail, bundle)
            } else {
                navigateToDelivery(showSearch = true)
            }
        }
        binding.pager.adapter = adapter
        TabLayoutMediator(binding.tabIndicator, binding.pager) { _, _ -> }.attach()

        startSliderAutoScroll(bannerItems.size)
    }

    private fun setupFallbackSlider() {
        val sliderImages = listOf(
            R.drawable.ic_placeholder,
            R.drawable.ic_placeholder,
            R.drawable.ic_placeholder
        )

        val adapter = SliderAdapter(fallbackImages = sliderImages) {
            navigateToDelivery(showSearch = true)
        }
        binding.pager.adapter = adapter
        TabLayoutMediator(binding.tabIndicator, binding.pager) { _, _ -> }.attach()

        startSliderAutoScroll(sliderImages.size)
    }

    private fun startSliderAutoScroll(itemCount: Int) {
        if (::sliderRunnable.isInitialized) {
            sliderHandler.removeCallbacks(sliderRunnable)
        }
        if (itemCount <= 0) return

        sliderRunnable = Runnable {
            if (_binding != null) {
                val currentItem = binding.pager.currentItem
                val nextItem = if (currentItem == itemCount - 1) 0 else currentItem + 1
                binding.pager.setCurrentItem(nextItem, true)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private var storyRunnable: Runnable? = null
    private val storyHandler = Handler(Looper.getMainLooper())

    private fun startStoryAutoScroll(itemCount: Int) {
        storyRunnable?.let { storyHandler.removeCallbacks(it) }
        if (itemCount <= 0) return

        storyRunnable = Runnable {
            if (_binding != null) {
                val layoutManager = binding.storyRV.layoutManager as? LinearLayoutManager
                if (layoutManager != null) {
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    val nextItem = if (firstVisible >= itemCount - 1) 0 else firstVisible + 1
                    binding.storyRV.smoothScrollToPosition(nextItem)
                }
                storyRunnable?.let { storyHandler.postDelayed(it, 3000) }
            }
        }
        storyRunnable?.let { storyHandler.postDelayed(it, 3000) }
    }

    private fun showCityPopup() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cities = redemptionRepository.listCities()

            if (cities == null || cities.isEmpty()) {
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

            val adapter = CitySelectionAdapter(cities, selectedCityId) { city ->
                selectedCityId = city.id
                sessionManager.saveSelectedCityId(city.id)
                viewModel.loadHomeData(city.id)
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

    private fun setupRecyclerViews() {
        binding.storyRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.topBannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val bannerImages = listOf(R.drawable.ic_ecommerce_banner, R.drawable.ic_delivierybanner)
        binding.topBannerRV.adapter = BannerAdapter(bannerImages) { position ->
            if (position == 0) {
                navigateToDelivery(showSearch = true, categoryName = "E-Commerce")
            } else {
                navigateToDelivery(showSearch = true, categoryName = "Delivery Deals")
            }
        }

        binding.bannerRV.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val topPicks = listOf(
            TopPick(R.drawable.ic_placeholder, "14th Street Pizza"),
            TopPick(R.drawable.ic_placeholder, "Broadway Pizza"),
            TopPick(R.drawable.ic_placeholder, "Pizza Hut")
        )
        binding.bannerRV.adapter = TopPicksAdapter(fallbackList = topPicks)

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
