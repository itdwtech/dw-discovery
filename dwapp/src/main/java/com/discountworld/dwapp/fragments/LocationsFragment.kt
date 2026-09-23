package com.discountworld.dwapp.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
import com.discountworld.dwapp.utils.fixImageUrl
import com.discountworld.dwapp.utils.makeDraggable
import com.discountworld.dwapp.viewmodels.LocationsUiState
import com.discountworld.dwapp.viewmodels.LocationsViewModel
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

    private val viewModel: LocationsViewModel by viewModels()
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
        observeViewModel()

        binding.fabCallClick.makeDraggable()
        binding.fabCallClick.setOnClickListener {
            // Add your call click logic here
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LocationsUiState.Loading -> { }
                is LocationsUiState.Success -> {
                    renderMapPins(state.pins)
                }
                is LocationsUiState.Error -> { }
                LocationsUiState.Idle -> { }
            }
        }
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
            if (categories.isNotEmpty()) {
                selectedCategoryId = categories.first().id
            }
            setupCategoryRecyclerView(categories)
        }
    }

    private fun setupCategoryRecyclerView(categories: List<RedemptionCategory>) {
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = LocationCategoryAdapter(categories) { category ->
            selectedCategoryId = category.id
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

                val fullTitle = if (pin.branchName.isNotEmpty()) "${pin.vendorTitle} ${pin.branchName}" else pin.vendorTitle
                infoWindowBinding.tvVendorTitle.text = fullTitle
                infoWindowBinding.tvAddress.text = pin.address

                return infoWindowBinding.root
            }

            override fun getInfoContents(marker: Marker): View? = null
        })

        map.setOnInfoWindowClickListener { marker ->
            val pin = marker.tag as? RedemptionMapPin
            if (pin != null) {
                val bundle = Bundle().apply {
                    putLong("vendor_id", pin.vendorId)
                    putLong("city_id", selectedCityId)
                }
                findNavController().navigate(R.id.action_nav_locations_to_nav_brand_detail, bundle)
            }
        }
    }

    private fun loadMapPins() {
        viewModel.loadMapPins(selectedCityId, selectedCategoryId)
    }

    private fun renderMapPins(pins: List<RedemptionMapPin>) {
        val map = googleMap ?: return
        val ctx = context ?: return

        map.clear()

        if (pins.isEmpty()) {
            val karachi = LatLng(24.8607, 67.0011)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(karachi, 12f))
            return
        }

        val defaultIcon = createCustomPinWithImage(ctx, null)
        val builder = LatLngBounds.Builder()

        for (pin in pins) {
            val position = LatLng(pin.latitude, pin.longitude)
            val title = if (pin.branchName.isNotEmpty()) "${pin.vendorTitle} - ${pin.branchName}" else pin.vendorTitle

            val markerOptions = MarkerOptions()
                .position(position)
                .title(title)
                .snippet(pin.address)

            defaultIcon?.let {
                markerOptions.icon(it)
            }

            val marker = map.addMarker(markerOptions)
            marker?.tag = pin
            builder.include(position)

            if (pin.vendorLogoUrl.isNotEmpty()) {
                Glide.with(ctx)
                    .asBitmap()
                    .load(pin.vendorLogoUrl.fixImageUrl())
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val customPinWithLogo = createCustomPinWithImage(ctx, resource)
                            customPinWithLogo?.let {
                                marker?.setIcon(it)
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
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

    private fun createCustomPinWithImage(context: Context, logoBitmap: Bitmap?): BitmapDescriptor? {
        val density = context.resources.displayMetrics.density
        val width = (46 * density).toInt()
        val height = (56 * density).toInt()
        val pinColor = ContextCompat.getColor(context, R.color.purple_primary)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val radius = width / 2f - (2 * density)
        val centerY = radius + (2 * density)

        val path = Path()
        val angleRad = Math.toRadians(40.0)
        val startX = (centerX + radius * Math.cos(angleRad)).toFloat()
        val startY = (centerY + radius * Math.sin(angleRad)).toFloat()
        val endX = (centerX - radius * Math.cos(angleRad)).toFloat()

        path.moveTo(endX, startY)
        path.lineTo(centerX, height.toFloat())
        path.lineTo(startX, startY)
        path.arcTo(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            40f,
            -260f,
            false
        )
        path.close()

        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, pinPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
        }
        canvas.drawPath(path, borderPaint)

        val whiteCircleRadius = radius * 0.72f
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, whiteCircleRadius, whitePaint)

        val innerCircleRadius = whiteCircleRadius * 0.88f
        val logoToDraw: Bitmap? = logoBitmap ?: run {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_placeholder)
            drawable?.let {
                val size = (innerCircleRadius * 2).toInt()
                drawableToBitmap(it, size, size)
            }
        }

        if (logoToDraw != null) {
            drawCircularLogoOnCanvas(canvas, logoToDraw, centerX, centerY, innerCircleRadius)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun drawCircularLogoOnCanvas(
        canvas: Canvas,
        logoBitmap: Bitmap,
        centerX: Float,
        centerY: Float,
        targetRadius: Float
    ) {
        val diameter = (targetRadius * 2).toInt()
        if (diameter <= 0) return

        val scaled = Bitmap.createScaledBitmap(logoBitmap, diameter, diameter, true)
        val circularBitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val circleCanvas = Canvas(circularBitmap)
        val clipPath = Path().apply {
            addCircle(diameter / 2f, diameter / 2f, targetRadius, Path.Direction.CW)
        }
        circleCanvas.clipPath(clipPath)
        circleCanvas.drawBitmap(scaled, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        canvas.drawBitmap(circularBitmap, centerX - targetRadius, centerY - targetRadius, null)
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
