package com.discountworld.easypaisasdk.fragments

import Constants.Companion.FONT_REG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.discountworld.easypaisasdk.adapters.VendorAdapter
import com.discountworld.easypaisasdk.databinding.FragmentSearchBinding
import com.discountworld.easypaisasdk.managers.CoroutineTask
import com.discountworld.easypaisasdk.repositories.HomeRepository
import com.discountworld.easypaisasdk.utils.TypeFaceUtils

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val repository = HomeRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
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
                if(text.isNotEmpty()){
                    findVendor(text)
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

    }

    private fun findVendor(text: String) {

        CoroutineTask.ioThenMain({

            val vendors = repository.getListOfVendors(search = text)
            val banners = repository.getBanners()

            Pair(vendors, banners)

        }, { result ->

            val vendors = result?.first
            val banners = result?.second ?: emptyList()

            if (vendors.isNullOrEmpty()) {

                Toast.makeText(requireContext(), "No vendors found", Toast.LENGTH_SHORT).show()

            } else {

                binding.rcyVendors.adapter = VendorAdapter(
                    vendors = vendors,
                    banners = banners   // ✅ IMPORTANT
                ) { vendor ->

                    val action = HomeFragmentDirections
                        .actionHomeFragmentToUserDetailFragment(
                            bannerTitle = vendor.companyName,
                            bannerSubtitle = vendor.description,
                            bannerTerms = vendor.title,
                            vendorId = vendor.id
                        )

                    findNavController().navigate(action)
                }
            }
        })
    }

    private fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}