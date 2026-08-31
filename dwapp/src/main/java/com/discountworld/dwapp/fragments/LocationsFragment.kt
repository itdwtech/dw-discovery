package com.discountworld.dwapp.fragments

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.discountworld.discount.RedemptionCategory
import com.discountworld.discount.RedemptionMapPin
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.LocationCategoryAdapter
import com.discountworld.dwapp.databinding.FragmentLocationsBinding
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

class LocationsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager
    private var googleMap: GoogleMap? = null

    private var selectedCityId: Long = 1L
    private var selectedCategoryId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        loadInitialCityAndCategories()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun loadInitialCityAndCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            val savedCityId = sessionManager.getSelectedCityId()
            if (savedCityId != null) {
                selectedCityId = savedCityId
            } else {
                val cities = redemptionRepository.listCities()
                cities?.firstOrNull()?.let {
                    selectedCityId = it.id
                    sessionManager.saveSelectedCityId(selectedCityId)
                }
            }

            val categories = redemptionRepository.listCategories() ?: emptyList()
            setupCategoryRecyclerView(categories)
        }
    }

    private fun setupCategoryRecyclerView(categories: List<RedemptionCategory>) {
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = LocationCategoryAdapter(categories) { category ->
            selectedCategoryId = category?.id
            loadMapPins()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        setupCustomInfoWindow(map)
        loadMapPins()
    }

    private fun setupCustomInfoWindow(map: GoogleMap) {
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                val pin = marker.tag as? RedemptionMapPin ?: return null
                val ctx = context ?: return null
                val infoWindowBinding = ItemMapInfoWindowBinding.inflate(LayoutInflater.from(ctx))

                infoWindowBinding.tvVendorTitle.text = pin.vendorTitle
                if (pin.branchName.isNotEmpty()) {
                    infoWindowBinding.tvBranchName.visibility = View.VISIBLE
                    infoWindowBinding.tvBranchName.text = pin.branchName
                } else {
                    infoWindowBinding.tvBranchName.visibility = View.GONE
                }
                infoWindowBinding.tvAddress.text = pin.address

                if (pin.vendorLogoUrl.isNotEmpty()) {
                    Glide.with(ctx)
                        .asBitmap()
                        .load(pin.vendorLogoUrl)
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

    private fun loadMapPins() {
        val map = googleMap ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val pins = redemptionRepository.listMapPins(selectedCityId, selectedCategoryId) ?: emptyList()
            map.clear()

            if (pins.isEmpty()) {
                val karachi = LatLng(24.8607, 67.0011)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(karachi, 12f))
                return@launch
            }

            val customMarkerIcon = getCustomLocationMarkerIcon()
            val builder = LatLngBounds.Builder()

            for (pin in pins) {
                val position = LatLng(pin.latitude, pin.longitude)
                val title = if (pin.branchName.isNotEmpty()) "${pin.vendorTitle} - ${pin.branchName}" else pin.vendorTitle

                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(pin.address)

                customMarkerIcon?.let {
                    markerOptions.icon(it)
                }

                val marker = map.addMarker(markerOptions)
                marker?.tag = pin
                builder.include(position)
            }

            if (pins.size == 1) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(pins[0].latitude, pins[0].longitude), 14f))
            } else {
                try {
                    val bounds = builder.build()
                    val padding = 120
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                } catch (_: Exception) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(pins[0].latitude, pins[0].longitude), 12f))
                }
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
