package com.example.easypaisasdk.fragments

import Constants.Companion.FONT_BOLD
import Constants.Companion.FONT_REG
import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
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
import com.example.easypaisasdk.R
import com.example.easypaisasdk.adapters.*
import com.example.easypaisasdk.databinding.BottomSheetCityBinding
import com.example.easypaisasdk.databinding.FragmentHomeBinding
import com.example.easypaisasdk.managers.CoroutineTask
import com.example.easypaisasdk.models.Brand
import com.example.easypaisasdk.models.City
import com.example.easypaisasdk.repositories.HomeRepository
import com.example.easypaisasdk.utils.LocationUtility
import com.example.easypaisasdk.utils.TypeFaceUtils
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import toPx
import toast

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var txtLocation: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var categoriesList: List<com.discountworld.discovery.Category> = emptyList()

    private val repository = HomeRepository()

    private var cityId: Long? = null


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
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

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)

        txtLocation = binding.txtLocation

        sharedPreferences =
            requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)


        shimmerStarted()
        checkLocationPermissionAndFetch()
        setupStatusBar()
        loadCategories()
        loadCities()
        setupBrands()
        loadFeaturedVendor()
        setupVendorsList()
//      setupBanners()

        binding.seeAllCities.setOnClickListener {
            showCityPopup()
        }

        binding.card.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_userDetailFragment)
        }

        binding.search.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.back.setOnClickListener {
            requireActivity().finishAffinity()
        }

    }

    private fun setupStatusBar() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            requireActivity().window.statusBarColor = Color.WHITE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requireActivity().window.decorView.systemUiVisibility = 0
            }
        }

    }

    private fun loadCities() {

        binding.rcyCities.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        CoroutineTask.ioThenMain({

            repository.getListOfCities(true)

        }, { cities ->

            if (!cities.isNullOrEmpty()) {
                binding.rcyCities.adapter = CityAdapter(cities){ city ->
                    cityId = city.id
                    setupBrands()
                    loadFeaturedVendor()
                    setupVendorsList()
                    requireContext().toast("Selected city is ${city.name}")
                }
            }

        })

    }

    private fun loadCategories() {

        CoroutineTask.ioThenMain({

            repository.getCategories()

        }, { categories ->

            if (categories != null) {

                categoriesList = categories

                // All Tab
                binding.category.addTab(
                    binding.category.newTab().setText("All")
                )

                // API categories
                categories.forEach {
                    binding.category.addTab(
                        binding.category.newTab().setText(it.name)
                    )
                }

                binding.category.addOnTabSelectedListener(object :
                    TabLayout.OnTabSelectedListener {

                    override fun onTabSelected(tab: TabLayout.Tab) {

                        val typeface = TypeFaceUtils.get(requireContext(),FONT_BOLD)
                        (tab.view.getChildAt(1) as TextView).setTypeface(typeface)

                        val isVisible: Int = if(tab.position == 0) View.VISIBLE else View.GONE
                        binding.lvCity.visibility = isVisible
                        binding.lvFeaturedVendor.visibility = isVisible
                        binding.rcyCities.visibility = isVisible
                        binding.rcyTopBrands.visibility = isVisible
                        binding.tvTopBrands.visibility = isVisible
                        if(tab.position == 0)
                            addMargin(binding.rcyVendors, 0, 0, 0, 0)
                        else addMargin(binding.rcyVendors, 16, 0, 0, 0)

                        if (tab.position == 0) {
                            setupVendorsList()
                        } else {
                            val categoryId = categoriesList[tab.position - 1].id
                            loadVendorsByCategory(categoryId)
                        }
                    }

                    override fun onTabUnselected(tab: TabLayout.Tab) {
                        val typeface = TypeFaceUtils.get(requireContext(),FONT_REG)
                        (tab.view.getChildAt(1) as TextView)
                            .setTypeface(typeface)

                    }

                    override fun onTabReselected(tab: TabLayout.Tab) {}

                })
            }
        })
    }

    private fun setupBrands() {

        binding.rcyTopBrands.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        CoroutineTask.ioThenMain({

            repository.getListOfVendors(featured = true, cityId = cityId)

        }, { vendors ->

            if(!vendors.isNullOrEmpty()){
                binding.rcyTopBrands.adapter = BrandAdapter(vendors) { vendor ->
                    val action = HomeFragmentDirections
                        .actionHomeFragmentToUserDetailFragment(
                            bannerTitle = vendor.companyName,
                            bannerSubtitle = vendor.description,
                            bannerTerms = vendor.title,
                            vendorId = vendor.id
                        )

                    findNavController().navigate(action)
                }
            }
        })
    }
    private fun setupVendorsList() {

        binding.rcyVendors.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {

            val vendors = repository.getListOfVendors(cityId = cityId) ?: emptyList()
            val banners = repository.getBanners() ?: emptyList()   // ✅ ADD THIS

            binding.rcyVendors.adapter = VendorAdapter(
                vendors = vendors,
                banners = banners   // ✅ PASS HERE
            ) { vendor ->

                val action = HomeFragmentDirections
                    .actionHomeFragmentToUserDetailFragment(
                        bannerTitle = vendor.companyName,
                        bannerSubtitle = vendor.description,
                        bannerTerms = vendor.title,
                        vendorId = vendor.id
                    )

                findNavController().navigate(action)
            }
        }
    }

    private fun loadFeaturedVendor() {

        CoroutineTask.ioThenMain({

            val vendors = repository.getListOfVendors(
                featured = true,
                cityId = cityId
            )

            val banners = repository.getBanners()

            Pair(vendors, banners)

        }, { result ->

            val vendors = result?.first
            val banners = result?.second

            val vendor = vendors?.firstOrNull()
            val banner = banners?.firstOrNull()

            if (vendor != null) {

                // ✅ Vendor Data (Title + Description)
                binding.txtTitle.text = vendor.title ?: ""
                binding.txtSubtitle.text = vendor.description ?: ""

                // ✅ Banner Image (from Banner API)
                Glide.with(requireContext())
                    .load(banner?.imageUrl ?: vendor.logoUrl) // fallback added
                    .placeholder(R.drawable.ic_banner)
                    .error(R.drawable.ic_banner)
                    .centerCrop()
                    .into(binding.imgBanner)

                // ✅ Click Navigation (Vendor data)
                binding.card.setOnClickListener {

                    val action =
                        HomeFragmentDirections.actionHomeFragmentToUserDetailFragment(
                            bannerTitle = vendor.companyName ?: "",
                            bannerSubtitle = vendor.description ?: "",
                            bannerTerms = vendor.title ?: "",
                            vendorId = vendor.id
                        )

                    findNavController().navigate(action)
                }

            } else {
                // Optional empty state
                binding.txtTitle.text = "No Data"
                binding.txtSubtitle.text = ""
                binding.imgBanner.setImageResource(R.drawable.ic_banner)
            }

            shimmerStopped()
        })
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

                        sharedPreferences.edit()
                            .putFloat("latitude", latitude.toFloat())
                            .putFloat("longitude", longitude.toFloat())
                            .apply()

                        resolveCity(latitude, longitude)

                    }

                    override fun onFailure(message: String) {

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

            if (city != null) {
                cityId = city.id
                txtLocation.text = city.name
                Log.d("grpc", "Nearest City: ${city.name}")

            }

        })

    }

    private fun showCityPopup() {

        val dialog = Dialog(requireContext(), R.style.CenterDialogTheme)

        val dialogBinding = BottomSheetCityBinding.inflate(layoutInflater)

        dialog.setContentView(dialogBinding.root)

        dialog.window?.setGravity(Gravity.CENTER)

        dialogBinding.rvAllCities.layoutManager =
            LinearLayoutManager(requireContext())

        CoroutineTask.ioThenMain({

            repository.getListOfCities(false)

        }, { cities ->

            if (!cities.isNullOrEmpty()) {
                dialogBinding.rvAllCities.adapter = CitiesAdapter(cities){ city ->
                    cityId = city.id
                    setupBrands()
                    loadFeaturedVendor()
                    setupVendorsList()
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
                R.drawable.divider
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

        lifecycleScope.launch {

            val vendors = repository.getListOfVendors(
                categoryId = categoryId
            ) ?: emptyList()

            val banners = repository.getBanners() ?: emptyList()   // ✅ ADD THIS

            binding.rcyVendors.adapter = VendorAdapter(
                vendors = vendors,
                banners = banners   // ✅ PASS HERE
            ) { vendor ->

                val action =
                    HomeFragmentDirections.actionHomeFragmentToUserDetailFragment(
                        bannerTitle = vendor.companyName,
                        bannerSubtitle = vendor.description,
                        bannerTerms = vendor.title,
                        vendorId = vendor.id
                    )

                findNavController().navigate(action)
            }
        }
    }

    private fun addMargin(view: View, top: Int, bottom: Int, left: Int, right: Int){
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(left.toPx(), top.toPx(), right.toPx(), bottom.toPx())
        view.layoutParams = params
    }

    private fun shimmerStarted(){
        binding.lvShimmer.startShimmer()
        binding.lvShimmer.visibility = View.VISIBLE
        binding.lvMain.visibility = View.GONE
    }

    private fun shimmerStopped(){
        binding.lvShimmer.stopShimmer()
        binding.lvShimmer.visibility = View.GONE
        binding.lvMain.visibility = View.VISIBLE
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null

    }

}