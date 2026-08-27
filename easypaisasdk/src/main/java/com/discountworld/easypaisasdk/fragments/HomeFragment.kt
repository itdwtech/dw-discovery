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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.adapters.*
import com.discountworld.easypaisasdk.databinding.DwDiscoveryBottomSheetCityBinding
import com.discountworld.easypaisasdk.databinding.DwDiscoveryFragmentHomeBinding
import com.discountworld.easypaisasdk.managers.CoroutineTask
import com.discountworld.easypaisasdk.repositories.HomeRepository
import com.discountworld.easypaisasdk.utils.LocationUtility
import com.discountworld.easypaisasdk.utils.TypeFaceUtils
import com.google.android.material.tabs.TabLayout
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

    // Cached adapters (survive view destruction on back stack)
    private var cityAdapter: CityAdapter? = null
    private var brandAdapter: BrandAdapter? = null
    private var vendorAdapter: VendorAdapter? = null
    private var cachedFeaturedVendor: com.discountworld.discovery.VendorSummary? = null
    private var cachedBannerUrl: String? = null

    // Data caches keyed by cityId or (cityId, categoryId)
    private var cachedBanners: List<com.discountworld.discovery.Banner>? = null
    private val vendorDataCache = mutableMapOf<Pair<Long?, Long?>, List<com.discountworld.discovery.VendorSummary>>()
    private val brandDataCache = mutableMapOf<Long?, List<com.discountworld.discovery.VendorSummary>>()


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
            shimmerStarted()
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
            loadVendorsByCategory(selectedCategoryId!!)
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
        // Load cities FIRST (warms gRPC channel + sets cityId),
        // then load everything else that depends on cityId
        binding.rcyCities.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // viewLifecycleOwner.lifecycleScope: auto-cancelled on onDestroyView,
        // so this coroutine won't resume and touch a null binding.
        viewLifecycleOwner.lifecycleScope.launch {
            // Step 1: fetch cities (this also establishes the gRPC connection)
            val cities = repository.getListOfCities(true)

            // Extra safety: view may have been torn down while the call was in flight
            if (_binding == null) return@launch

            if (!cities.isNullOrEmpty()) {
                val sortedCities = cities.sortedBy { it.sortOrder }
                if (cityId == null) {
                    cityId = sortedCities.first().id
                }

                cityAdapter = CityAdapter(sortedCities, selectedCityId = cityId) { city ->
                    if (_binding == null) return@CityAdapter
                    cityId = city.id
                    cityAdapter?.setSelectedCity(cityId)
                    setupBrands()
                    setupVendorsList()
                    //requireContext().toast("Selected city is ${city.name}")
                }
                binding.rcyCities.adapter = cityAdapter
            }

            // Step 2: now cityId is set — load everything else
            loadCategories()
            setupBrands()
            setupVendorsList()
        }
    }

    private fun loadCities() {
        binding.rcyCities.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        CoroutineTask.ioThenMain({

            repository.getListOfCities(true)

        }, { cities ->

            if (_binding == null) return@ioThenMain

            if (!cities.isNullOrEmpty()) {
                val sortedCities = cities.sortedBy { it.sortOrder }
                // Default to first city if location hasn't resolved yet
                if (cityId == null) {
                    cityId = sortedCities.first().id
                }

                cityAdapter = CityAdapter(sortedCities, selectedCityId = cityId){ city ->
                    if (_binding == null) return@CityAdapter
                    cityId = city.id
                    cityAdapter?.setSelectedCity(cityId)
                    setupBrands()
                    setupVendorsList()
                    requireContext().toast("Selected city is ${city.name}")
                }
                binding.rcyCities.adapter = cityAdapter
            }

        })

    }

    private fun loadCategories() {

        CoroutineTask.ioThenMain({

            repository.getCategories()

        }, { categories ->

            if (_binding == null) return@ioThenMain

            if (categories != null) {

                categoriesList = categories

                // All Tab
                binding.category.addTab(
                    binding.category.newTab().setText("All")
                )

                // API categories
                categories.forEach {
                    binding.category.addTab(
                        binding.category.newTab().setText(it.name.toTitleCase())
                    )
                }

                binding.category.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        if (_binding == null) return

                        selectedTabPosition = tab.position

                        val typeface = TypeFaceUtils.get(requireContext(), FONT_BOLD)
                        (tab.view.getChildAt(1) as TextView).setTypeface(typeface)

                        val isVisible = if (tab.position == 0) View.VISIBLE else View.GONE

                        binding.lvCity.visibility = isVisible
                        binding.lvFeaturedVendor.visibility = isVisible
                        binding.rcyCities.visibility = isVisible
                        binding.rcyTopBrands.visibility = isVisible
                        binding.tvTopBrands.visibility = isVisible

                        if (tab.position == 0) {

                            // ✅ CLEAR SEARCH WHEN ALL CLICKED
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
                            loadVendorsByCategory(selectedCategoryId!!)
                        }
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        if (_binding == null) return
                        val typeface = TypeFaceUtils.get(requireContext(), FONT_REG)
                        (tab.view.getChildAt(1) as TextView).setTypeface(typeface)
                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {}
                })
            }
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

        binding.rcyVendors.layoutManager = LinearLayoutManager(requireContext())

        val key = Pair(cityId, null as Long?)
        val cachedVendors = vendorDataCache[key]
        if (cachedVendors != null) {
            vendorAdapter = createVendorAdapter(cachedVendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = vendorAdapter

            // Set first vendor as featured
            if (cachedVendors.isNotEmpty()) {
                updateFeaturedFromList(cachedVendors[0])
            }
            shimmerStopped()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val vendors = repository.getVendorsList(cityId = cityId) ?: emptyList()

            if (_binding == null) return@launch

            if (cachedBanners == null) {
                cachedBanners = repository.getBanners() ?: emptyList()
            }

            if (_binding == null) return@launch

            vendorDataCache[key] = vendors
            vendorAdapter = createVendorAdapter(vendors, cachedBanners!!)
            binding.rcyVendors.adapter = vendorAdapter

            // Set first vendor as featured
            if (vendors.isNotEmpty()) {
                updateFeaturedFromList(vendors[0])
            }
            shimmerStopped()
        }
    }

    private fun updateFeaturedFromList(vendor: com.discountworld.discovery.VendorSummary) {
        cachedFeaturedVendor = vendor
        cachedBannerUrl = cachedBanners?.firstOrNull { it.vendorId == vendor.id }?.imageUrl ?: vendor.logoUrl
        bindFeaturedVendor()
    }

    @SuppressLint("SetTextI18n")
    private fun bindFeaturedVendor() {
        if (_binding == null) return

        val vendor = cachedFeaturedVendor
        if (vendor != null) {
            binding.txtTitle.text = vendor.title ?: ""
            binding.txtSubtitle.text = vendor.shortDescription ?: ""

            Glide.with(requireContext())
                .load(cachedBannerUrl)
                .placeholder(R.drawable.dw_discovery_ic_banner)
                .error(R.drawable.dw_discovery_ic_banner)
                .centerCrop()
                .into(binding.imgBanner)

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
            binding.txtTitle.text = "No Data"
            binding.txtSubtitle.text = ""
            binding.imgBanner.setImageResource(R.drawable.dw_discovery_ic_banner)
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
                cityId = city.id
                txtLocation.text = city.name
                cityAdapter?.setSelectedCity(cityId)
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

        divider.setDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.dw_discovery_divider
            )!!
        )

        dialogBinding.rvAllCities.addItemDecoration(divider)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        dialog.show()

    }

    private fun loadVendorsByCategory(categoryId: Long) {

        val key = Pair(cityId, categoryId as Long?)
        val cachedVendors = vendorDataCache[key]
        if (cachedVendors != null) {
            val adapter = createVendorAdapter(cachedVendors, cachedBanners ?: emptyList())
            binding.rcyVendors.adapter = adapter
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            val vendors = repository.getVendorsList(
                categoryId = categoryId,
                cityId = cityId
            ) ?: emptyList()

            if (_binding == null) return@launch

            if (cachedBanners == null) {
                cachedBanners = repository.getBanners() ?: emptyList()
            }

            if (_binding == null) return@launch

            vendorDataCache[key] = vendors
            val adapter = createVendorAdapter(vendors, cachedBanners!!)
            binding.rcyVendors.adapter = adapter

            // Update featured from list even when category changes
            if (vendors.isNotEmpty()) {
                updateFeaturedFromList(vendors[0])
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

        // Restore featured vendor
        cachedFeaturedVendor?.let { vendor ->
            binding.txtTitle.text = vendor.title ?: ""
            binding.txtSubtitle.text = vendor.shortDescription ?: ""

            Glide.with(requireContext())
                .load(cachedBannerUrl)
                .placeholder(R.drawable.dw_discovery_ic_banner)
                .error(R.drawable.dw_discovery_ic_banner)
                .centerCrop()
                .into(binding.imgBanner)

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
            binding.category.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (_binding == null) return

                    selectedTabPosition = tab.position

                    val typeface = TypeFaceUtils.get(requireContext(), FONT_BOLD)
                    (tab.view.getChildAt(1) as TextView).setTypeface(typeface)

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
                        loadVendorsByCategory(selectedCategoryId!!)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    if (_binding == null) return
                    val typeface = TypeFaceUtils.get(requireContext(), FONT_REG)
                    (tab.view.getChildAt(1) as TextView).setTypeface(typeface)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            // Restore previously selected tab
            if (selectedTabPosition in 0 until binding.category.tabCount) {
                binding.category.getTabAt(selectedTabPosition)?.select()
            }
        }

        // Show content immediately — no shimmer
        shimmerStopped()
    }

    private fun shimmerStarted(){
        if (_binding == null) return
        binding.lvShimmer.startShimmer()
        binding.lvShimmer.visibility = View.VISIBLE
        binding.lvMain.visibility = View.GONE
    }

    private fun shimmerStopped(){
        if (_binding == null) return
        binding.lvShimmer.stopShimmer()
        binding.lvShimmer.visibility = View.GONE
        binding.lvMain.visibility = View.VISIBLE
    }

    private fun findVendor(text: String) {

        CoroutineTask.ioThenMain({

            val vendors = repository.getVendorsList(
                search = text,
                categoryId = selectedCategoryId,
                cityId = cityId
            )

            val banners = repository.getBanners()

            Pair(vendors, banners)

        }, { result ->

            if (_binding == null) return@ioThenMain

            val vendors = result?.first ?: emptyList()
            val banners = result?.second ?: emptyList()

            binding.rcyVendors.adapter = VendorAdapter(
                vendors = vendors,
                banners = banners
            ) { vendor ->

                if (_binding == null) return@VendorAdapter

                val action =
                    HomeFragmentDirections.actionHomeFragmentToUserDetailFragment(
                        bannerTitle = vendor.companyName,
                        bannerSubtitle = vendor.description,
                        bannerTerms = vendor.title,
                        vendorId = vendor.id,
                        cityId = cityId ?: 1
                    )

                findNavController().navigate(action)
            }
        })
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null

    }
}
fun String.toTitleCase(): String {
    return this.lowercase().replaceFirstChar { it.uppercaseChar() }
}