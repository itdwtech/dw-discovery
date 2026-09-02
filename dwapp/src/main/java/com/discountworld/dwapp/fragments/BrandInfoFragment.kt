package com.discountworld.dwapp.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.discountworld.discount.RedemptionBranch
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandBranchesAdapter
import com.discountworld.dwapp.databinding.FragmentBrandInfoBinding
import com.discountworld.dwapp.databinding.ItemMapInfoWindowBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.repositories.RedemptionRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class BrandInfoFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentBrandInfoBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager
    private var googleMap: GoogleMap? = null

    private var branchesList: List<RedemptionBranch> = emptyList()
    private val markerMap = mutableMapOf<Marker, RedemptionBranch>()

    private var targetLatLng = LatLng(24.8138, 67.0673) // Default DHA Phase 7, Karachi
    private var vendorTitle = "PizzaHut"
    private var vendorLogoUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        binding.cvBack.setOnClickListener {
            findNavController().navigateUp()
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapInfoFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        val vendorId = arguments?.getLong("vendor_id", -1L) ?: -1L
        val cityIdArg = arguments?.getLong("city_id", -1L) ?: -1L
        val selectedCityId = if (cityIdArg != -1L) cityIdArg else sessionManager.getSelectedCityId()

        if (vendorId != -1L) {
            loadVendorDetail(vendorId, selectedCityId)
        } else {
            setupFallbackData()
        }

        setupClickListeners()
    }

    private fun loadVendorDetail(vendorId: Long, cityId: Long?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val vendor = redemptionRepository.getVendorDetail(vendorId, cityId)
            if (vendor != null) {
                vendorTitle = vendor.title.ifEmpty { vendor.companyName.ifEmpty { "PizzaHut" } }
                binding.tvHeaderTitle.text = vendorTitle

                if (vendor.logoUrl.isNotEmpty()) {
                    vendorLogoUrl = vendor.logoUrl
                    Glide.with(requireContext())
                        .load(vendor.logoUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(binding.ivBrandLogo)
                }

                if (vendor.description.isNotEmpty()) {
                    binding.tvDescription.text = vendor.description
                }

                if (vendor.termsAndConditions.isNotEmpty()) {
                    binding.tvTermsHeader.visibility = View.VISIBLE
                    binding.tvTermsList.visibility = View.VISIBLE
                    binding.tvTermsList.text = vendor.termsAndConditions
                } else {
                    binding.tvTermsHeader.visibility = View.GONE
                    binding.tvTermsList.visibility = View.GONE
                }

                branchesList = vendor.branchesList
                if (branchesList.isNotEmpty()) {
                    setupBranchesRecyclerView(branchesList)
                    val primaryBranch = branchesList.first()
                    if (primaryBranch.latitude != 0.0 && primaryBranch.longitude != 0.0) {
                        targetLatLng = LatLng(primaryBranch.latitude, primaryBranch.longitude)
                    }
                    if (primaryBranch.phoneNumber.isNotEmpty()) {
                        binding.tvPhoneNumber.text = primaryBranch.phoneNumber
                    }
                } else if (vendor.headOfficeNumber.isNotEmpty()) {
                    binding.tvPhoneNumber.text = vendor.headOfficeNumber
                    setupFallbackSingleAddress()
                } else {
                    setupFallbackSingleAddress()
                }

                updateMapLocation()

                val websiteLink = vendor.socialLinksList.firstOrNull {
                    it.url.isNotBlank() && (it.platform.equals("website", ignoreCase = true) || it.url.startsWith("http") || it.url.startsWith("www"))
                }
                if (websiteLink != null) {
                    binding.tvWebsiteUrl.text = websiteLink.url
                    binding.tvWebsiteUrl.visibility = View.VISIBLE
                } else {
                    binding.tvWebsiteUrl.visibility = View.GONE
                }
            } else {
                setupFallbackData()
            }
        }
    }

    private fun setupBranchesRecyclerView(branches: List<RedemptionBranch>) {
        binding.rvBranches.visibility = View.VISIBLE
        binding.cvSingleAddressCard.visibility = View.GONE

        binding.rvBranches.layoutManager = LinearLayoutManager(requireContext())
        val adapter = BrandBranchesAdapter(branches) { branch ->
            val lat = if (branch.latitude != 0.0) branch.latitude else targetLatLng.latitude
            val lng = if (branch.longitude != 0.0) branch.longitude else targetLatLng.longitude
            val pos = LatLng(lat, lng)

            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))

            markerMap.entries.firstOrNull {
                it.value.id == branch.id || (branch.name.isNotEmpty() && it.value.name == branch.name)
            }?.key?.showInfoWindow()
        }
        binding.rvBranches.adapter = adapter
    }

    private fun setupFallbackSingleAddress() {
        binding.rvBranches.visibility = View.GONE
        binding.cvSingleAddressCard.visibility = View.VISIBLE
        binding.tvSingleBranchName.text = "$vendorTitle - DHA-7"
        binding.tvAddressText.text = "Khayaban-e-Ittehad Road, D.H.A. Phase 7, Karachi, 75500, Pakistan"
        binding.tvSinglePhone.text = "021-111-222-333"

        binding.btnSingleCall.setOnClickListener {
            val phone = binding.tvSinglePhone.text.toString().trim()
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            startActivity(intent)
        }

        binding.btnSingleDirections.setOnClickListener {
            val lat = targetLatLng.latitude
            val lng = targetLatLng.longitude
            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(vendorTitle)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                startActivity(browserIntent)
            }
        }
    }

    private fun setupFallbackData() {
        binding.tvHeaderTitle.text = "PizzaHut"
        binding.tvDescription.text = "pioneer of pizza in pakistan"
        binding.tvTermsHeader.visibility = View.GONE
        binding.tvTermsList.visibility = View.GONE

        binding.tvPhoneNumber.text = "021-111-222-333"
        binding.tvWebsiteUrl.text = "https://www.pizzahut.com.pk/"
        binding.tvWebsiteUrl.visibility = View.VISIBLE

        setupFallbackSingleAddress()
        updateMapLocation()
    }

    private fun setupClickListeners() {
        binding.tvPhoneNumber.setOnClickListener {
            val phone = binding.tvPhoneNumber.text.toString().trim()
            if (phone.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                startActivity(intent)
            }
        }

        binding.tvWebsiteUrl.setOnClickListener {
            var url = binding.tvWebsiteUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = false
        googleMap?.uiSettings?.isScrollGesturesEnabled = true

        setupCustomInfoWindow(map)
        updateMapLocation()
    }

    private fun setupCustomInfoWindow(map: GoogleMap) {
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                val ctx = context ?: return null
                val infoWindowBinding = ItemMapInfoWindowBinding.inflate(LayoutInflater.from(ctx))

                val branch = markerMap[marker]
                infoWindowBinding.tvVendorTitle.text = vendorTitle

                if (branch != null && branch.name.isNotEmpty()) {
                    infoWindowBinding.tvBranchName.visibility = View.VISIBLE
                    infoWindowBinding.tvBranchName.text = branch.name
                } else if (marker.title.orEmpty().contains("-")) {
                    val branchTitleName = marker.title.orEmpty().substringAfter("-").trim()
                    infoWindowBinding.tvBranchName.visibility = View.VISIBLE
                    infoWindowBinding.tvBranchName.text = branchTitleName
                } else {
                    infoWindowBinding.tvBranchName.visibility = View.GONE
                }

                val addressToShow = branch?.address?.ifEmpty { marker.snippet } ?: marker.snippet.orEmpty()
                infoWindowBinding.tvAddress.text = addressToShow

                if (!vendorLogoUrl.isNullOrEmpty()) {
                    Glide.with(ctx)
                        .asBitmap()
                        .load(vendorLogoUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                infoWindowBinding.ivVendorLogo.setImageBitmap(resource)
                                if (marker.isInfoWindowShown) {
                                    marker.showInfoWindow()
                                }
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                infoWindowBinding.ivVendorLogo.setImageDrawable(placeholder)
                            }
                        })
                }

                return infoWindowBinding.root
            }

            override fun getInfoContents(marker: Marker): View? = null
        })
    }

    private fun updateMapLocation() {
        val map = googleMap ?: return
        val ctx = context ?: return

        map.clear()
        markerMap.clear()

        if (branchesList.isEmpty()) {
            val markerOptions = MarkerOptions()
                .position(targetLatLng)
                .title("$vendorTitle - DHA-7")
                .snippet("Khayaban-e-Ittehad Road, D.H.A. Phase 7, Karachi")

            getCustomLocationMarkerIcon()?.let {
                markerOptions.icon(it)
            }

            map.addMarker(markerOptions)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15f))
            return
        }

        val builder = LatLngBounds.Builder()
        var validPinCount = 0

        for (branch in branchesList) {
            val lat = if (branch.latitude != 0.0) branch.latitude else targetLatLng.latitude
            val lng = if (branch.longitude != 0.0) branch.longitude else targetLatLng.longitude
            val pos = LatLng(lat, lng)

            val title = if (branch.name.isNotEmpty()) "$vendorTitle - ${branch.name}" else vendorTitle
            val markerOptions = MarkerOptions()
                .position(pos)
                .title(title)
                .snippet(branch.address)

            getCustomLocationMarkerIcon()?.let {
                markerOptions.icon(it)
            }

            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                markerMap[marker] = branch
            }

            builder.include(pos)
            validPinCount++
        }

        if (validPinCount == 1) {
            val singleBranch = branchesList.first()
            val lat = if (singleBranch.latitude != 0.0) singleBranch.latitude else targetLatLng.latitude
            val lng = if (singleBranch.longitude != 0.0) singleBranch.longitude else targetLatLng.longitude
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
        } else if (validPinCount > 1) {
            try {
                val bounds = builder.build()
                val padding = (60 * ctx.resources.displayMetrics.density).toInt()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            } catch (_: Exception) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 12f))
            }
        }
    }

    private fun getCustomLocationMarkerIcon(): BitmapDescriptor? {
        val ctx = context ?: return null
        val drawable = ContextCompat.getDrawable(ctx, R.drawable.ic_nav_location) ?: return null
        val tintedDrawable = DrawableCompat.wrap(drawable).mutate()
        DrawableCompat.setTint(tintedDrawable, ContextCompat.getColor(ctx, R.color.purple_primary))

        val width = (36 * ctx.resources.displayMetrics.density).toInt()
        val height = (36 * ctx.resources.displayMetrics.density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        tintedDrawable.setBounds(0, 0, canvas.width, canvas.height)
        tintedDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
