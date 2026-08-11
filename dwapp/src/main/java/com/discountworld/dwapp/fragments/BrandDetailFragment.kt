package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.adapters.OffersAdapter
import com.discountworld.dwapp.databinding.FragmentBrandDetailBinding
import com.discountworld.dwapp.models.Offer

class BrandDetailFragment : Fragment() {

    private var _binding: FragmentBrandDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrandDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvOffers.layoutManager = LinearLayoutManager(requireContext())
        val dummyOffers = listOf(
            Offer("Buy 1 Get 1 Free on any Pizza", "50%"),
            Offer("Flat 30% OFF on all Side Orders", "30%"),
            Offer("Get a free drink with any Large Pizza", "Free"),
            Offer("Buy 2 Medium Pizzas for Rs. 1500", "Rs.1500")
        )
        binding.rvOffers.adapter = OffersAdapter(dummyOffers)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
