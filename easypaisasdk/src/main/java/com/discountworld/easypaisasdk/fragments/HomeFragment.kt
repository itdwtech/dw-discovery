package com.discountworld.easypaisasdk.fragments

import com.discountworld.easypaisasdk.variables.Constants.Companion.FONT_BOLD
import com.discountworld.easypaisasdk.variables.Constants.Companion.FONT_REG
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.adapters.*
import com.discountworld.easypaisasdk.databinding.DwDiscoveryBottomSheetCityBinding
import com.discountworld.easypaisasdk.databinding.DwDiscoveryFragmentHomeBinding
import com.discountworld.easypaisasdk.managers.CoroutineTask
import com.discountworld.easypaisasdk.repositories.HomeRepository
import com.discountworld.easypaisasdk.utils.LocationUtility
import com.discountworld.easypaisasdk.utils.TypeFaceUtils
import com.discountworld.easypaisasdk.utils.getShimmerDrawable
import com.discountworld.easypaisasdk.utils.loadBannerImage
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import com.discountworld.easypaisasdk.utils.toPx
import com.discountworld.easypaisasdk.utils.toast

class HomeFragment : Fragment() {

    private var _binding: DwDiscoveryFragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var txtLocation: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var categoriesList: List<com.discountworld.discovery.Category> = emptyList()

    private val repository = HomeRepository()

    private var cityId: Long? = null
    private var isSearching = false

    private var selectedCategoryId: Long? = null
    private var selectedTabPosition: Int = 0
    private var isInitialLoad = true

    // Pagination variables
    private var currentPage = 1
    private var isLoadingPage = false
    private var hasMorePages = true

    // Cached adapters (survive view destruction on back stack)
    private var cityAdapter: CityAdapter? = null
    private var brandAdapter: BrandAdapter? = null
    private var vendorAdapter: VendorAdapter? = null
    private var cachedFeaturedVendor: com.discountworld.discovery.VendorSummary? = null
    private var cachedBannerUrl: String? = null
    private var lastLoadedBannerUrl: String? = null

    // Data caches keyed by cityId or (cityId, categoryId)
    private var cachedBanners: List<com.discountworld.discovery.Banner>? = null
    private val vendorDataCache = mutableMapOf<Pair<Long?, Long?>, List<com.discountworld.discovery.VendorSummary>>()
    private val brandDataCache = mutableMapOf<Long?, List<com.discountworld.discovery.VendorSummary>>()

    companion object {
        private const val KEY_LAST_CITY_ID = "last_city_id"
        private const val KEY_FEATURED_CITY_ID = "last_featured_city_id"
        private const val KEY_FEATURED_VENDOR_ID = "last_featured_vendor_id"
        private const val KEY_FEATURED_COMPANY = "last_featured_company"
        private const val KEY_FEATURED_DESC = "last_featured_desc"
        private const val KEY_FEATURED_TITLE = "last_featured_title"
        private const val KEY_FEATURED_SUBTITLE = "last_featured_subtitle"
        private const val KEY_FEATURED_BANNER_URL = "last_featured_banner_url"
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (_binding == null) return@registerForActivityResult
            if (isGranted) {
                fetchLocation()
            } else {
                txtLocation.text = "Location permission denied"
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DwDiscoveryFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        txtLocation = binding.txtLocation

        sharedPreferences =
            requireActivity().getSharedPreferences("dw_discovery_prefs", Context.MODE_PRIVATE)

        // Prevent Android from restoring EditText text (triggers TextWatcher too early)
        binding.search.isSaveEnabled = false

        setupStatusBar()

        if (isInitialLoad) {
            checkLocationPermissionAndFetch()
            initialLoad()
            isInitialLoad = false
        } else {
            restoreFromCache()
        }

        binding.seeAllCities.setOnClickListener {
            showCityPopup()
        }

        binding.search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val text = s.toString()
                if (text.isNotEmpty()) {

                    enterSearchMode()
                    findVendor(text)

                } else {

                    exitSearchMode()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.nestedScroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (v.getChildAt(0) != null) {
                if (scrollY >= (v.getChildAt(0).measuredHeight - v.measuredHeight - 300)) {
                    loadMoreVendors()
                }
            }
        })
    }

    // New method to hide UI elements when searching
    private fun enterSearchMode() {
        isSearching = true

        // Hide all UI elements similar to category selection
        binding.lvCity.visibility = View.GONE
        binding.lvFeaturedVendor.visibility = View.GONE
        binding.rcyCities.visibility = View.GONE
        binding.rcyTopBrands.visibility = View.GONE
        binding.tvTopBrands.visibility = View.GONE
        binding.category.visibility = View.GONE  // Hide categories tab
        binding.seeAllCities.visibility = View.GONE  // Hide "See All Cities" button

        // Show only search results area
        binding.rcyVendors.visibility = View.VISIBLE
    }

    // New method to show UI elements when search is cleared
    private fun exitSearchMode() {

        isSearching = false

        binding.tvNoVendor.visibility = View.GONE
        binding.rcyVendors.visibility = View.VISIBLE
        binding.lvCity.visibility = View.VISIBLE
        binding.lvFeaturedVendor.visibility = View.VISIBLE
        binding.rcyCities.visibility = View.VISIBLE
        binding.rcyTopBrands.visibility = View.VISIBLE
        binding.tvTopBrands.visibility = View.VISIBLE
        binding.category.visibility = View.VISIBLE
        binding.seeAllCities.visibility = View.VISIBLE

        val selectedTabPosition = binding.category.selectedTabPosition
        val categoryIndex = selectedTabPosition - 1

        if (selectedTabPosition <= 0 || categoryIndex >= categoriesList.size) {
            selectedCategoryId = null
            setupVendorsList()
        } else {
            selectedCategoryId = categoriesList[categoryIndex].id
            selectedCategoryId?.let { loadVendorsByCategory(it) }
        }
    }

    private fun setupStatusBar() {

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            requireActivity().window.statusBarColor = Color.WHITE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requireActivity().window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun initialLoad() {
        val savedCityId = sharedPreferences.getLong(KEY_LAST_CITY_ID, -1L).takeIf { it > 0 }
        if (cityId == null && savedCityId != null) {
            cityId = savedCityId
        }

        val hasCachedBanner = restoreFeaturedFromPrefs()
        binding.loaderLayout.visibility = if (hasCachedBanner) View.GONE else View.VISIBLE

        binding.rcyCities.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcyTopBrands.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rcyVendors.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            val citiesDeferred = async(Dispatchers.IO) { repository.getListOfCities(true) }
            val categoriesDeferred = async(Dispatchers.IO) { repository.getCategories() }
            val bannersDeferred = async(Dispatchers.IO) { repository.getBanners() }

            val initialCityResolvedAtStart = cityId != null
            if (!initialCityResolvedAtStart) {
                val cities = citiesDeferred.await()
                if (_binding == null) return@launch
                if (!cities.isNullOrEmpty()) {
                    bindCitiesList(cities)
                }
            }

            val requestCityId = cityId
            val topBrandsDeferred = async(Dispatchers.IO) { repository.getTopBrands(cityId = requestCityId) }
            val vendorsDeferred = async(Dispatchers.IO) { repository.getVendorsWithBanners(cityId = requestCityId) }

            if (initialCityResolvedAtStart) {
                launch {
                    val cities = citiesDeferred.await()
                    if (_binding == null || cities.isNullOrEmpty()) return@launch
                    val previousCityId = cityId
                    bindCitiesList(cities)
                    if (cityId != null && cityId != previousCityId) {
                        setupBrands()
                        setupVendorsList()
                    }
                }
            }

            launch {
                val categories = categoriesDeferred.await()
                if (_binding == null || categories == null) return@launch
                categoriesList = categories
                binding.category.removeAllTabs()
                binding.category.addTab(binding.category.newTab().setText("All"))
                categories.forEach {
                    binding.category.addTab(binding.category.newTab().setText(it.name.toTitleCase()))
                }
                setupCategoryTabListener()
            }

            launch {
                val topBrands = topBrandsDeferred.await()
                if (_binding == null || topBrands.isNullOrEmpty()) return@launch
                brandDataCache[requestCityId] = topBrands
                if (cityId == requestCityId) {
                    brandAdapter = createBrandAdapter(topBrands)
                    binding.rcyTopBrands.adapter = brandAdapter
                }
            }

            launch {
                val banners = bannersDeferred.await()
                if (_binding == null || banners == null) return@launch
                val merged = mergeBanners(cachedBanners, banners)
                cachedBanners = merged
                vendorAdapter?.updateBanners(merged)
                if (cachedBannerUrl.isNullOrBlank()) {
                    cachedFeaturedVendor?.let { updateFeaturedFromList(it) }
                }
            }

            try {
                val vendorsResult = vendorsDeferred.await()
                if (_binding == null) return@launch

                val vendors = vendorsResult?.vendors ?: emptyList()
                val embeddedBanners = vendorsResult?.banners
                if (!embeddedBanners.isNullOrEmpty()) {
                    cachedBanners = mergeBanners(embeddedBanners, cachedBanners)
                }

                val key = Pair(requestCityId, null as Long?)
                vendorDataCache[key] = vendors
                if (cityId == requestCityId) {
                    vendorAdapter = createVendorAdapter(vendors, cachedBanners ?: emptyList())
                    binding.rcyVendors.adapter = vendorAdapter

                    currentPage = 1
                    hasMorePages = vendors.size >= 10
                    isLoadingPage = false

                    if (vendors.isNotEmpty()) {
                        updateFeaturedFromList(vendors[0])
                    }
                }
            } finally {
                _binding?.loaderLayout?.visibility = View.GONE
            }
        }
    }

    private fun bindCitiesList(cities: List<com.discountworld.discovery.City>) {
        val sortedCities = cities.sortedBy { it.sortOrder }
        val currentValid = cityId != null && sortedCities.any { it.id == cityId }
        if (!currentValid) {
            val islamabadCity = sortedCities.firstOrNull { it.name.contains("Islamabad", ignoreCase = true) }
            cityId = islamabadCity?.id ?: sortedCities.first().id
        }
        cityId?.let { saveLastCityId(it) }

        cityAdapter = CityAdapter(sortedCities, selectedCityId = cityId) { city ->
            if (_binding == null) return@CityAdapter
            cityId = city.id
            saveLastCityId(city.id)
            cityAdapter?.setSelectedCity(cityId)
            setupBrands()
            setupVendorsList()
        }
        binding.rcyCities.adapter = cityAdapter
    }

    private fun setupCategoryTabListener() {
        binding.category.clearOnTabSelectedListeners()
        binding.category.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                if (_binding == null) return

                selectedTabPosition = tab.position

                val typeface = TypeFaceUtils.get(requireContext(), FONT_BOLD)
                (tab.view.getChildAt(1) as? TextView)?.setTypeface(typeface)

                val isVisible = if (tab.position == 0) View.VISIBLE else View.GONE

                binding.lvCity.visibility = isVisible
                binding.lvFeaturedVendor.visibility = isVisible
                binding.rcyCities.visibility = isVisible
                binding.rcyTopBrands.visibility = isVisible
                binding.tvTopBrands.visibility = isVisible

                if (tab.position == 0) {
                    binding.search.setText("")
                    selectedCategoryId = null
                    addMargin(binding.rcyVendors, 0, 0, 0, 0)
                } else {
                    selectedCategoryId = categoriesList[tab.position - 1].id
                    addMargin(binding.rcyVendors, 16, 0, 0, 0)
                }

                val searchText = binding.search.text.toString()

                if (searchText.isNotEmpty()) {
                    findVendor(searchText)
                    return
                }

                if (tab.position == 0) {
                    setupVendorsList()
                } else {
                    selectedCategoryId?.let { loadVendorsByCategory(it) }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                if (_binding == null) return
                val typeface = TypeFaceUtils.get(requireContext(), FONT_REG)
                (tab.view.getChildAt(1) as? TextView)?.setTypeface(typeface)
            }

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupBrands() {
        binding.rcyTopBrands.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        val cached = brandDataCache[cityId]
        if (cached != null) {
            brandAdapter = createBrandAdapter(cached)
            binding.rcyTopBrands.adapter = brandAdapter
            return
        }

        CoroutineTask.ioThenMain({

            repository.getTopBrands(cityId = cityId)

        }, { vendors ->

            if (_binding == null) return@ioThenMain

            if(!vendors.isNullOrEmpty()){
                brandDataCache[cityId] = vendors
                brandAdapter = createBrandAdapter(vendors)
                binding.rcyTopBrands.adapter = brandAdapter
            }
        })
    }

    private fun createBrandAdapter(vendors: List<com.discountworld.discovery.VendorSummary>): BrandAdapter {
        return BrandAdapter(vendors) { vendor ->
            if (_binding == null) return@BrandAdapter
            val action = HomeFragmentDirections
                .actionHomeFragmentToUserDetailFragment(
                    bannerTitle = vendor.companyName,
                    bannerSubtitle = vendor.description,
                    bannerTerms = vendor.title,
                    vendorId = vendor.id,
                    cityId = cityId ?: 1
                )
            findNavController().navigate(action)
        }
    }

    private fun setupVendorsList() {
        resetPagination()

        binding.tvNoVendor.visibility = View.GONE
        binding.rcyVendors.visibility = View.VISIBLE
        binding.rcyVendors.layoutManager = LinearLayoutManager(requireContext())

        val key = Pair(cityId, null as Long?)
        val cachedVendors = vendorDataCache[key]
        if (cachedVendors != null) {
            vendorAdapter = createVendorAdapter(cachedVendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = vendorAdapter

            currentPage = (cachedVendors.size / 10).coerceAtLeast(1)
            hasMorePages = cachedVendors.size % 10 == 0

            if (cachedVendors.isNotEmpty()) {
                updateFeaturedFromList(cachedVendors[0])
            }
            binding.loaderLayout.visibility = View.GONE
            return
        }

        binding.loaderLayout.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val requestCityId = cityId
            val bannersDeferred = if (cachedBanners == null) {
                async(Dispatchers.IO) { repository.getBanners() }
            } else {
                null
            }
            val vendorsResult = repository.getVendorsWithBanners(cityId = requestCityId, page = 1, pageSize = 10)
            val vendors = vendorsResult?.vendors ?: emptyList()

            if (_binding == null) return@launch

            if (!vendorsResult?.banners.isNullOrEmpty()) {
                cachedBanners = mergeBanners(vendorsResult?.banners, cachedBanners)
            } else if (cachedBanners == null) {
                cachedBanners = bannersDeferred?.await() ?: emptyList()
            }

            if (_binding == null) return@launch

            vendorDataCache[key] = vendors
            vendorAdapter = createVendorAdapter(vendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = vendorAdapter

            currentPage = 1
            hasMorePages = vendors.size >= 10

            if (vendors.isNotEmpty()) {
                updateFeaturedFromList(vendors[0])
            }
            binding.loaderLayout.visibility = View.GONE

            // Ensure any remaining banners from listBanners are merged without delaying initial banner render
            bannersDeferred?.await()?.let { allBanners ->
                if (_binding == null) return@launch
                val merged = mergeBanners(cachedBanners, allBanners)
                cachedBanners = merged
                vendorAdapter?.updateBanners(merged)
                if (cachedBannerUrl.isNullOrBlank() && vendors.isNotEmpty()) {
                    updateFeaturedFromList(vendors[0])
                }
            }
        }
    }

    private fun updateFeaturedFromList(vendor: com.discountworld.discovery.VendorSummary) {
        val previousVendorId = cachedFeaturedVendor?.id
        cachedFeaturedVendor = vendor
        val matchedUrl = cachedBanners?.firstOrNull { it.vendorId == vendor.id }?.imageUrl
        if (matchedUrl != null || previousVendorId != vendor.id) {
            cachedBannerUrl = matchedUrl
        }
        saveFeaturedCache(vendor, cachedBannerUrl, cityId)
        bindFeaturedVendor()
    }

    @SuppressLint("SetTextI18n")
    private fun bindFeaturedVendor() {
        if (_binding == null) return

        val vendor = cachedFeaturedVendor
        if (vendor != null) {
            binding.txtTitle.text = vendor.title ?: ""
            binding.txtSubtitle.text = vendor.shortDescription ?: ""

            val url = cachedBannerUrl
            if (url != lastLoadedBannerUrl || binding.imgBanner.drawable == null) {
                lastLoadedBannerUrl = url
                binding.imgBanner.loadBannerImage(
                    url = url,
                    priority = Priority.IMMEDIATE,
                    requestManager = Glide.with(this)
                )
            }

            binding.card.setOnClickListener {
                val action = HomeFragmentDirections.actionHomeFragmentToUserDetailFragment(
                    bannerTitle = vendor.companyName ?: "",
                    bannerSubtitle = vendor.description ?: "",
                    bannerTerms = vendor.title ?: "",
                    vendorId = vendor.id,
                    cityId = cityId ?: 1
                )
                findNavController().navigate(action)
            }
        } else {
            lastLoadedBannerUrl = null
            binding.txtTitle.text = "No Data"
            binding.txtSubtitle.text = ""
            binding.imgBanner.setImageDrawable(getShimmerDrawable())
        }
    }

    private fun checkLocationPermissionAndFetch() {

        if (checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fetchLocation()

        } else {

            requestPermissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        }

    }

    private fun fetchLocation() {

        activity?.let { act ->

            LocationUtility.getCurrentLocation(
                act,
                object : LocationUtility.LocationResultCallback {

                    override fun onLocationSuccess(latitude: Double, longitude: Double) {

                        if (_binding == null) return

                        sharedPreferences.edit()
                            .putFloat("latitude", latitude.toFloat())
                            .putFloat("longitude", longitude.toFloat())
                            .apply()

                        resolveCity(latitude, longitude)

                    }

                    override fun onFailure(message: String) {

                        if (_binding == null) return
                        txtLocation.text = message

                    }

                })

        }

    }

    private fun resolveCity(latitude: Double, longitude: Double) {

        CoroutineTask.ioThenMain({

            repository.getCityByCoordinates(
                latitude.toFloat(),
                longitude.toFloat()
            )

        }, { city ->

            if (_binding == null) return@ioThenMain

            if (city != null) {
                if (cityId != city.id) {
                    cityId = city.id
                    saveLastCityId(city.id)
                    cityAdapter?.setSelectedCity(cityId)
                    setupBrands()
                    setupVendorsList()
                }
                txtLocation.text = city.name
                Log.d("grpc", "Nearest City: ${city.name}")
            }

        })

    }

    private fun showCityPopup() {
        if (_binding == null) return

        val dialog = Dialog(requireContext(), R.style.DwDiscovery_CenterDialogTheme)

        val dialogBinding = DwDiscoveryBottomSheetCityBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)

        dialog.window?.setGravity(Gravity.CENTER)

        dialogBinding.rvAllCities.layoutManager =
            LinearLayoutManager(requireContext())

        CoroutineTask.ioThenMain({

            repository.getListOfCities(false)

        }, { cities ->

            // Fragment's own view may be gone even though the dialog is separate;
            // guard before touching fragment state (cityId, cityAdapter, setupBrands, etc.)
            if (!cities.isNullOrEmpty()) {
                val sortedCities = cities.sortedBy { it.sortOrder }
                dialogBinding.rvAllCities.adapter = CitiesAdapter(sortedCities, selectedCityId = cityId){ city ->
                    if (_binding != null) {
                        cityId = city.id
                        saveLastCityId(city.id)
                        cityAdapter?.setSelectedCity(cityId)
                        setupBrands()
                        setupVendorsList()
                    }
                    dialog.dismiss()
                }
            }

        })

        val divider = DividerItemDecoration(
            requireContext(),
            DividerItemDecoration.VERTICAL
        )

        ContextCompat.getDrawable(
            requireContext(),
            R.drawable.dw_discovery_divider
        )?.let { divider.setDrawable(it) }

        dialogBinding.rvAllCities.addItemDecoration(divider)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()

    }

    private fun resetPagination() {
        currentPage = 1
        isLoadingPage = false
        hasMorePages = true
    }

    private fun loadMoreVendors() {
        if (isLoadingPage || !hasMorePages) return

        isLoadingPage = true
        val nextPage = currentPage + 1

        viewLifecycleOwner.lifecycleScope.launch {
            val searchText = binding.search.text.toString()
            val search = if (searchText.isNotEmpty()) searchText else null

            val result = repository.getVendorsWithBanners(
                cityId = cityId,
                categoryId = selectedCategoryId,
                search = search,
                page = nextPage,
                pageSize = 10
            )
            val newVendors = result?.vendors ?: emptyList()

            if (_binding == null) {
                isLoadingPage = false
                return@launch
            }

            if (newVendors.isNotEmpty()) {
                currentPage = nextPage
                if (!result?.banners.isNullOrEmpty()) {
                    cachedBanners = mergeBanners(cachedBanners, result?.banners)
                }
                vendorAdapter?.addVendors(newVendors, result?.banners)
                val key = Pair(cityId, selectedCategoryId)
                val existing = vendorDataCache[key] ?: emptyList()
                vendorDataCache[key] = existing + newVendors

                if (newVendors.size < 10) {
                    hasMorePages = false
                }
            } else {
                hasMorePages = false
            }
            isLoadingPage = false
        }
    }

    private fun loadVendorsByCategory(categoryId: Long) {
        resetPagination()

        binding.tvNoVendor.visibility = View.GONE
        binding.rcyVendors.visibility = View.VISIBLE
        val key = Pair(cityId, categoryId as Long?)
        val cachedVendors = vendorDataCache[key]
        if (cachedVendors != null) {
            vendorAdapter = createVendorAdapter(cachedVendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = vendorAdapter
            currentPage = (cachedVendors.size / 10).coerceAtLeast(1)
            hasMorePages = cachedVendors.size % 10 == 0
            binding.loaderLayout.visibility = View.GONE
            return
        }

        binding.loaderLayout.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            val bannersDeferred = if (cachedBanners == null) {
                async(Dispatchers.IO) { repository.getBanners() }
            } else {
                null
            }
            val vendorsResult = repository.getVendorsWithBanners(
                categoryId = categoryId,
                cityId = cityId,
                page = 1,
                pageSize = 10
            )
            val vendors = vendorsResult?.vendors ?: emptyList()

            if (_binding == null) return@launch

            if (!vendorsResult?.banners.isNullOrEmpty()) {
                cachedBanners = mergeBanners(vendorsResult?.banners, cachedBanners)
            } else if (cachedBanners == null) {
                cachedBanners = bannersDeferred?.await() ?: emptyList()
            }

            if (_binding == null) return@launch

            vendorDataCache[key] = vendors
            vendorAdapter = createVendorAdapter(vendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = vendorAdapter

            currentPage = 1
            hasMorePages = vendors.size >= 10

            // Update featured from list even when category changes
            if (vendors.isNotEmpty()) {
                updateFeaturedFromList(vendors[0])
            }
            binding.loaderLayout.visibility = View.GONE

            bannersDeferred?.await()?.let { allBanners ->
                if (_binding == null) return@launch
                val merged = mergeBanners(cachedBanners, allBanners)
                cachedBanners = merged
                vendorAdapter?.updateBanners(merged)
                if (cachedBannerUrl.isNullOrBlank() && vendors.isNotEmpty()) {
                    updateFeaturedFromList(vendors[0])
                }
            }
        }
    }

    private fun createVendorAdapter(
        vendors: List<com.discountworld.discovery.VendorSummary>,
        banners: List<com.discountworld.discovery.Banner>
    ): VendorAdapter {
        return VendorAdapter(vendors = vendors, banners = banners) { vendor ->
            if (_binding == null) return@VendorAdapter
            val action = HomeFragmentDirections.actionHomeFragmentToUserDetailFragment(
                bannerTitle = vendor.companyName,
                bannerSubtitle = vendor.description,
                bannerTerms = vendor.title,
                vendorId = vendor.id,
                cityId = cityId ?: 1
            )
            findNavController().navigate(action)
        }
    }

    private fun addMargin(view: View, top: Int, bottom: Int, left: Int, right: Int){
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(left.toPx(), top.toPx(), right.toPx(), bottom.toPx())
        view.layoutParams = params
    }

    @SuppressLint("SetTextI18n")
    private fun restoreFromCache() {
        if (_binding == null) return

        // Re-attach cached adapters to new views — no API calls
        binding.rcyCities.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        cityAdapter?.let { binding.rcyCities.adapter = it }

        binding.rcyTopBrands.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        brandAdapter?.let { binding.rcyTopBrands.adapter = it }

        binding.rcyVendors.layoutManager = LinearLayoutManager(requireContext())
        vendorAdapter?.let { binding.rcyVendors.adapter = it }

        // Restore featured vendor using shared binding helper
        if (cachedFeaturedVendor != null) {
            bindFeaturedVendor()
        }

        // Restore category tabs
        if (categoriesList.isNotEmpty()) {
            binding.category.removeAllTabs()
            binding.category.addTab(binding.category.newTab().setText("All"))
            categoriesList.forEach {
                binding.category.addTab(
                    binding.category.newTab().setText(it.name.toTitleCase())
                )
            }
            setupCategoryTabListener()

            // Restore previously selected tab
            if (selectedTabPosition in 0 until binding.category.tabCount) {
                binding.category.getTabAt(selectedTabPosition)?.select()
            }
        }
    }

    private fun findVendor(text: String) {
        resetPagination()
        binding.loaderLayout.visibility = View.VISIBLE
        binding.tvNoVendor.visibility = View.GONE

        CoroutineTask.ioThenMain({

            val result = repository.getVendorsWithBanners(
                search = text,
                categoryId = selectedCategoryId,
                cityId = cityId,
                page = 1,
                pageSize = 10
            )
            val banners = if (!result?.banners.isNullOrEmpty()) {
                result?.banners
            } else {
                repository.getBanners()
            }

            Pair(result?.vendors, banners)

        }, { result ->

            if (_binding == null) return@ioThenMain

            binding.loaderLayout.visibility = View.GONE

            val vendors = result?.first ?: emptyList()
            val banners = result?.second ?: emptyList()

            currentPage = 1
            hasMorePages = vendors.size >= 10

            if (vendors.isEmpty()) {
                binding.tvNoVendor.visibility = View.VISIBLE
                binding.rcyVendors.visibility = View.GONE
            } else {
                binding.tvNoVendor.visibility = View.GONE
                binding.rcyVendors.visibility = View.VISIBLE
                vendorAdapter = createVendorAdapter(vendors, banners)
                binding.rcyVendors.adapter = vendorAdapter
            }
        })
    }

    private fun mergeBanners(
        primary: List<com.discountworld.discovery.Banner>?,
        secondary: List<com.discountworld.discovery.Banner>?
    ): List<com.discountworld.discovery.Banner> {
        val merged = mutableListOf<com.discountworld.discovery.Banner>()
        val seen = HashSet<Pair<Long, Long>>()
        fun appendUnique(list: List<com.discountworld.discovery.Banner>?) {
            list?.forEach { banner ->
                if (seen.add(banner.vendorId to banner.id)) {
                    merged.add(banner)
                }
            }
        }
        appendUnique(primary)
        appendUnique(secondary)
        return merged
    }

    private fun saveLastCityId(id: Long) {
        sharedPreferences.edit().putLong(KEY_LAST_CITY_ID, id).apply()
    }

    private fun saveFeaturedCache(
        vendor: com.discountworld.discovery.VendorSummary,
        bannerUrl: String?,
        forCityId: Long?
    ) {
        if (bannerUrl.isNullOrBlank()) return
        sharedPreferences.edit()
            .putLong(KEY_FEATURED_CITY_ID, forCityId ?: -1L)
            .putLong(KEY_FEATURED_VENDOR_ID, vendor.id)
            .putString(KEY_FEATURED_COMPANY, vendor.companyName ?: "")
            .putString(KEY_FEATURED_DESC, vendor.description ?: "")
            .putString(KEY_FEATURED_TITLE, vendor.title ?: "")
            .putString(KEY_FEATURED_SUBTITLE, vendor.shortDescription ?: "")
            .putString(KEY_FEATURED_BANNER_URL, bannerUrl)
            .apply()
    }

    private fun restoreFeaturedFromPrefs(): Boolean {
        val savedUrl = sharedPreferences.getString(KEY_FEATURED_BANNER_URL, null)
        val savedVendorId = sharedPreferences.getLong(KEY_FEATURED_VENDOR_ID, -1L)
        if (savedUrl.isNullOrBlank() || savedVendorId <= 0L) return false

        val savedFeaturedCityId = sharedPreferences.getLong(KEY_FEATURED_CITY_ID, -1L)
        if (cityId != null && savedFeaturedCityId > 0L && cityId != savedFeaturedCityId) {
            return false
        }

        val vendor = com.discountworld.discovery.VendorSummary.newBuilder()
            .setId(savedVendorId)
            .setCompanyName(sharedPreferences.getString(KEY_FEATURED_COMPANY, "") ?: "")
            .setDescription(sharedPreferences.getString(KEY_FEATURED_DESC, "") ?: "")
            .setTitle(sharedPreferences.getString(KEY_FEATURED_TITLE, "") ?: "")
            .setShortDescription(sharedPreferences.getString(KEY_FEATURED_SUBTITLE, "") ?: "")
            .build()

        cachedFeaturedVendor = vendor
        cachedBannerUrl = savedUrl
        bindFeaturedVendor()
        return true
    }

    override fun onDestroyView() {

        super.onDestroyView()

        lastLoadedBannerUrl = null
        _binding = null

    }
}
fun String.toTitleCase(): String {
    return this.lowercase().replaceFirstChar { it.uppercaseChar() }
}