package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionBranch
import com.discountworld.discount.RedemptionVendorDetail
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandBranchesAdapter
import com.discountworld.dwapp.databinding.FragmentBrandInfoBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.utils.fixImageUrl
import com.discountworld.dwapp.viewmodels.BrandInfoState
import com.discountworld.dwapp.viewmodels.BrandInfoViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker

class BrandInfoFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentBrandInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrandInfoViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    private var branchesList: List<RedemptionBranch> = emptyList()
    private var vendorTitle: String = "PizzaHut"
    private var vendorLogoUrl: String = ""
    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val vendorId = arguments?.getLong("vendor_id", -1L) ?: -1L
        val cityIdArg = arguments?.getLong("city_id", -1L) ?: -1L
        val selectedCityId = if (cityIdArg != -1L) cityIdArg else sessionManager.getSelectedCityId()

        observeViewModel()

        if (vendorId != -1L) {
            val effectiveCityId = selectedCityId ?: 1L
            viewModel.loadBrandInfo(vendorId, effectiveCityId)
        } else {
            setupFallbackData()
        }

        setupClickListeners()
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapInfoFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun observeViewModel() {
        viewModel.brandInfoState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is BrandInfoState.Loading -> { }
                is BrandInfoState.Success -> {
                    bindVendorData(state.vendorDetail)
                }
                is BrandInfoState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    setupFallbackData()
                }
                BrandInfoState.Idle -> { }
            }
        }
    }

    private fun bindVendorData(vendor: RedemptionVendorDetail) {
        vendorTitle = vendor.title.ifEmpty { vendor.companyName.ifEmpty { "PizzaHut" } }
        binding.tvHeaderTitle.text = vendorTitle

        if (vendor.logoUrl.isNotEmpty()) {
            vendorLogoUrl = vendor.logoUrl
            Glide.with(requireContext())
                .load(vendor.logoUrl.fixImageUrl())
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

        if (vendor.branchesList.isNotEmpty()) {
            branchesList = vendor.branchesList
            setupBranchesList(branchesList)
            showBranchesOnMap()
        } else {
            setupFallbackBranches()
        }
    }

    private fun setupFallbackData() {
        binding.tvHeaderTitle.text = "PizzaHut"
        binding.tvDescription.text = "Pizza Hut is an American multinational restaurant chain and international franchise founded in 1958 in Wichita, Kansas by Dan and Frank Carney."
        setupFallbackBranches()
    }

    private fun setupFallbackBranches() {
        val dummyBranches = listOf(
            RedemptionBranch.newBuilder()
                .setId(1)
                .setName("Main Branch - Clifton")
                .setAddress("Clifton Block 2, Karachi")
                .setPhoneNumber("021-111241111")
                .setLatitude(24.8138)
                .setLongitude(67.0300)
                .build(),
            RedemptionBranch.newBuilder()
                .setId(2)
                .setName("Gulshan Branch")
                .setAddress("Block 13-C, Gulshan-e-Iqbal, Karachi")
                .setPhoneNumber("021-111241111")
                .setLatitude(24.9180)
                .setLongitude(67.0971)
                .build()
        )
        branchesList = dummyBranches
        setupBranchesList(dummyBranches)
        showBranchesOnMap()
    }

    private fun setupBranchesList(branches: List<RedemptionBranch>) {
        val adapter = BrandBranchesAdapter(
            branches = branches,
            onBranchClick = { branch ->
                if (branch.latitude != 0.0 && branch.longitude != 0.0) {
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(branch.latitude, branch.longitude),
                            16f
                        )
                    )
                }
            }
        )
        binding.rvBranches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBranches.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        showBranchesOnMap()
    }

    private fun showBranchesOnMap() {
        val map = googleMap ?: return
        val ctx = context ?: return
        map.clear()

        if (branchesList.isEmpty()) return

        val defaultIcon = createCustomPinWithImage(ctx, null)
        val builder = LatLngBounds.Builder()
        var hasValidLocation = false
        val markers = mutableListOf<Marker>()

        for (branch in branchesList) {
            if (branch.latitude != 0.0 && branch.longitude != 0.0) {
                val position = LatLng(branch.latitude, branch.longitude)
                val title = if (branch.name.isNotEmpty()) "${vendorTitle} - ${branch.name}" else vendorTitle

                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(branch.address)
                
                defaultIcon?.let {
                    markerOptions.icon(it)
                }

                val marker = map.addMarker(markerOptions)
                if (marker != null) {
                    markers.add(marker)
                }

                builder.include(position)
                hasValidLocation = true
            }
        }

        if (vendorLogoUrl.isNotEmpty()) {
            Glide.with(ctx)
                .asBitmap()
                .load(vendorLogoUrl.fixImageUrl())
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val customPinWithLogo = createCustomPinWithImage(ctx, resource)
                        customPinWithLogo?.let {
                            for (marker in markers) {
                                marker.setIcon(it)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        if (hasValidLocation) {
            try {
                if (branchesList.size == 1) {
                    val branch = branchesList.first { it.latitude != 0.0 && it.longitude != 0.0 }
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(branch.latitude, branch.longitude), 14f))
                } else {
                    val bounds = builder.build()
                    val padding = 100
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
