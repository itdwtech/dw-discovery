package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.adapters.DeliveryDealsAdapter
import com.discountworld.dwapp.databinding.FragmentDeliveryBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DeliveryFragment : Fragment() {

    private var _binding: FragmentDeliveryBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager
    private val dealsAdapter = DeliveryDealsAdapter()
    private var searchJob: Job? = null

    private var selectedCityId: Long? = null
    private var selectedCategoryId: Long? = null

    private var isEcommerce: Boolean = false
    private var isDelivery: Boolean = false

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

        sessionManager = SessionManager(requireContext())

        val cityIdArg = arguments?.getLong("cityId", -1L) ?: -1L
        selectedCityId = if (cityIdArg != -1L) cityIdArg else sessionManager.getSelectedCityId()

        val categoryName = arguments?.getString("categoryName")
        val categoryIdArg = arguments?.getLong("categoryId", -1L) ?: -1L
        if (categoryIdArg != -1L) {
            selectedCategoryId = categoryIdArg
        }

        if (!categoryName.isNullOrEmpty()) {
            binding.tvTitle.text = categoryName
            if (categoryName.contains("ecommerce", ignoreCase = true) || categoryName.contains("e-commerce", ignoreCase = true)) {
                isEcommerce = true
            } else if (categoryName.contains("delivery", ignoreCase = true)) {
                isDelivery = true
            }
        }

        val initialSearchQuery = arguments?.getString("searchQuery")
        if (!initialSearchQuery.isNullOrEmpty()) {
            binding.etSearch.setText(initialSearchQuery)
        }

        val showSearch = arguments?.getBoolean("showSearch") ?: true
        if (!showSearch) {
            binding.etSearch.parent.let { (it as View).visibility = View.GONE }
            if (categoryName.isNullOrEmpty()) {
                binding.tvTitle.text = "Search Results"
            }
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupSearch()
        loadVendors(initialSearchQuery)
    }

    private fun setupRecyclerView() {
        binding.rvDeliveryDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeliveryDeals.adapter = dealsAdapter
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val query = binding.etSearch.text?.toString()?.trim()
            loadVendors(query)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                val query = binding.etSearch.text?.toString()?.trim()
                loadVendors(query)
                true
            } else {
                false
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(400)
                    val query = s?.toString()?.trim()
                    loadVendors(query)
                }
            }
        })
    }

    private fun loadVendors(searchQuery: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.GONE

            val query = if (searchQuery.isNullOrEmpty()) null else searchQuery

            val response = redemptionRepository.listVendors(
                cityId = selectedCityId,
                delivery = if (isDelivery) true else null,
                ecommerce = if (isEcommerce) true else null,
                categoryId = selectedCategoryId,
                search = query
            )

            val vendors = response?.vendorsList ?: emptyList()

            binding.progressBar.visibility = View.GONE

            if (vendors.isNotEmpty()) {
                binding.rvDeliveryDeals.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE
                dealsAdapter.updateData(vendors)
            } else {
                binding.rvDeliveryDeals.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
                binding.tvNoData.text = when {
                    isEcommerce -> "No E-Commerce vendors found"
                    isDelivery -> "No Delivery vendors found"
                    else -> "No vendors found"
                }
                dealsAdapter.updateData(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
