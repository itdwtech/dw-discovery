package com.discountworld.easypaisasdk.fragments

import com.discountworld.easypaisasdk.variables.Constants.Companion.FONT_REG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.easypaisasdk.adapters.VendorAdapter
import com.discountworld.easypaisasdk.databinding.DwDiscoveryFragmentSearchBinding
import com.discountworld.easypaisasdk.managers.CoroutineTask
import com.discountworld.easypaisasdk.repositories.HomeRepository
import com.discountworld.easypaisasdk.utils.TypeFaceUtils

class SearchFragment : Fragment() {

    private var _binding: DwDiscoveryFragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val repository = HomeRepository()
    private val args: SearchFragmentArgs by navArgs()

    private var vendorAdapter: VendorAdapter? = null
    private var currentPage = 1
    private var isLoadingPage = false
    private var hasMorePages = true
    private var currentSearchText: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DwDiscoveryFragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.search.typeface = TypeFaceUtils.get(requireContext(), FONT_REG)
        binding.search.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s.toString()
                if (text.isNotEmpty()) {
                    currentSearchText = text
                    findVendor(text)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rcyVendors.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                if (layoutManager != null) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!isLoadingPage && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0
                        ) {
                            loadMoreVendors()
                        }
                    }
                }
            }
        })
    }

    private fun findVendor(text: String) {
        currentPage = 1
        isLoadingPage = false
        hasMorePages = true

        CoroutineTask.ioThenMain({

            val vendors = repository.getVendorsList(search = text, page = 1, pageSize = 10)
            val banners = repository.getBanners()

            Pair(vendors, banners)

        }, { result ->

            if (_binding == null) return@ioThenMain

            val vendors = result?.first ?: emptyList()
            val banners = result?.second ?: emptyList()

            hasMorePages = vendors.size >= 10

            if (vendors.isEmpty()) {
                Toast.makeText(requireContext(), "No vendors found", Toast.LENGTH_SHORT).show()
                binding.rcyVendors.adapter = null
            } else {
                vendorAdapter = VendorAdapter(
                    vendors = vendors,
                    banners = banners
                ) { vendor ->

                    val action = HomeFragmentDirections
                        .actionHomeFragmentToUserDetailFragment(
                            bannerTitle = vendor.companyName,
                            bannerSubtitle = vendor.description,
                            bannerTerms = vendor.title,
                            vendorId = vendor.id,
                            cityId = args.cityId
                        )

                    findNavController().navigate(action)
                }
                binding.rcyVendors.adapter = vendorAdapter
            }
        })
    }

    private fun loadMoreVendors() {
        if (isLoadingPage || !hasMorePages || currentSearchText.isEmpty()) return

        isLoadingPage = true
        val nextPage = currentPage + 1

        CoroutineTask.ioThenMain({
            repository.getVendorsList(search = currentSearchText, page = nextPage, pageSize = 10)
        }, { newVendors ->
            if (_binding == null) {
                isLoadingPage = false
                return@ioThenMain
            }

            if (!newVendors.isNullOrEmpty()) {
                currentPage = nextPage
                vendorAdapter?.addVendors(newVendors)
                if (newVendors.size < 10) {
                    hasMorePages = false
                }
            } else {
                hasMorePages = false
            }
            isLoadingPage = false
        })
    }

    private fun onBackPressed() {
        requireActivity().onBackPressed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
