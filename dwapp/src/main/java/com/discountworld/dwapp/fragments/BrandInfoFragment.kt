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

class BrandInfoFragment : Fragment() {

    private var _binding: FragmentBrandInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrandInfoViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    private var branchesList: List<RedemptionBranch> = emptyList()
    private var vendorTitle: String = "PizzaHut"
    private var vendorLogoUrl: String = ""

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
    }

    private fun setupBranchesList(branches: List<RedemptionBranch>) {
        val adapter = BrandBranchesAdapter(
            branches = branches,
            onBranchClick = { _ -> }
        )
        binding.rvBranches.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBranches.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
