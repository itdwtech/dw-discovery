package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.DeliveryDealsAdapter
import com.discountworld.dwapp.databinding.FragmentDeliveryBinding
import com.discountworld.dwapp.models.DeliveryDeal

class DeliveryFragment : Fragment() {

    private var _binding: FragmentDeliveryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeliveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val showSearch = arguments?.getBoolean("showSearch") ?: true
        if (!showSearch) {
            binding.etSearch.parent.let { (it as View).visibility = View.GONE }
            binding.tvTitle.text = "Search Results"
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvDeliveryDeals.layoutManager = LinearLayoutManager(requireContext())
        val dummyData = listOf(
            DeliveryDeal(R.drawable.ic_almasjewellers, R.drawable.ic_allurebeauty, "Subway", "E-commerce"),
            DeliveryDeal(R.drawable.ic_anamta_comfort, R.drawable.ic_allurebeauty, "Subway", "E-commerce"),
            DeliveryDeal(R.drawable.ic_arish_pk, R.drawable.ic_allurebeauty, "Subway", "E-commerce")
        )
        binding.rvDeliveryDeals.adapter = DeliveryDealsAdapter(dummyData)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
