package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.BrandLogosAdapter
import com.discountworld.dwapp.adapters.SliderAdapter
import com.discountworld.dwapp.adapters.TopPicksAdapter
import com.discountworld.dwapp.databinding.FragmentPromosBinding
import com.discountworld.dwapp.models.TopPick
import com.google.android.material.tabs.TabLayoutMediator

class PromosFragment : Fragment() {

    private var _binding: FragmentPromosBinding? = null
    private val binding get() = _binding!!

    private val sliderHandler = Handler(Looper.getMainLooper())
    private lateinit var sliderRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPromosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPromoSlider()
        setupDiscountsList()
        setupLogosList()
    }

    private fun setupPromoSlider() {
        val sliderImages = listOf(R.drawable.ic_arish_pk, R.drawable.ic_anamta_comfort, R.drawable.ic_almasjewellers)
        binding.promoPager.adapter = SliderAdapter(sliderImages)
        TabLayoutMediator(binding.promoTabIndicator, binding.promoPager) { _, _ -> }.attach()

        sliderRunnable = Runnable {
            if (_binding != null) {
                val currentItem = binding.promoPager.currentItem
                val nextItem = if (currentItem == sliderImages.size - 1) 0 else currentItem + 1
                binding.promoPager.setCurrentItem(nextItem, true)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        }
        sliderHandler.postDelayed(sliderRunnable, 3000)
    }

    private fun setupDiscountsList() {
        binding.rvAmazingDiscounts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val list = listOf(
            TopPick(R.drawable.ic_arish_pk, "Allure Beauty"),
            TopPick(R.drawable.ic_anamta_comfort, "Allure Beauty"),
            TopPick(R.drawable.ic_almasjewellers, "Allure Beauty")
        )
        // Reusing TopPicksAdapter as it matches the design (Banner + Brand Name)
        binding.rvAmazingDiscounts.adapter = TopPicksAdapter(list)
    }

    private fun setupLogosList() {
        binding.rvBrandLogos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val list = listOf(
            TopPick(R.drawable.ic_allurebeauty, "Allure Beauty"),
            TopPick(R.drawable.ic_allurebeauty, "Allure Beauty"),
            TopPick(R.drawable.ic_allurebeauty, "Allure Beauty"),
            TopPick(R.drawable.ic_allurebeauty, "Allure Beauty"),
            TopPick(R.drawable.ic_allurebeauty, "Allure Beauty")
        )
        binding.rvBrandLogos.adapter = BrandLogosAdapter(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sliderHandler.removeCallbacks(sliderRunnable)
        _binding = null
    }
}
