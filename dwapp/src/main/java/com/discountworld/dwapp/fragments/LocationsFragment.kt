package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.LocationCategoryAdapter
import com.discountworld.dwapp.databinding.FragmentLocationsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class LocationsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationsBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null

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

        setupCategories()
        
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupCategories() {
        val categories = listOf("Food", "Salon & Spa", "Leisure", "Fitness", "Retail", "Health", "Education")
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCategories.adapter = LocationCategoryAdapter(categories)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Coordinates for Karachi (as shown in image)
        val karachi = LatLng(24.8607, 67.0011)
        
        // Adding dummy markers
        googleMap?.addMarker(MarkerOptions().position(LatLng(24.87, 67.03)).title("SG"))
        googleMap?.addMarker(MarkerOptions().position(LatLng(24.82, 67.05)).title("K"))
        googleMap?.addMarker(MarkerOptions().position(LatLng(24.83, 67.02)).title("G"))
        googleMap?.addMarker(MarkerOptions().position(LatLng(24.81, 67.01)).title("ab"))
        googleMap?.addMarker(MarkerOptions().position(LatLng(24.82, 66.98)).title("CP"))
        
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(karachi, 12f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
