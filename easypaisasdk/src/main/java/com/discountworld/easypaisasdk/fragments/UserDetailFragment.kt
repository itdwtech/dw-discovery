package com.discountworld.easypaisasdk.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.discountworld.discovery.VendorFullDetail
import com.discountworld.easypaisasdk.R
import com.discountworld.easypaisasdk.adapters.CardsAdapter
import com.discountworld.easypaisasdk.adapters.OutletsAdapter
import com.discountworld.easypaisasdk.databinding.DwDiscoveryFragmentUserDetailBinding
import com.discountworld.easypaisasdk.repositories.DetailRepository
import kotlinx.coroutines.launch

class UserDetailFragment : Fragment() {

    private var _binding: DwDiscoveryFragmentUserDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = DetailRepository()
    private val args: UserDetailFragmentArgs by navArgs()

    private var vendorDetail: VendorFullDetail? = null

    private val outletsAdapter = OutletsAdapter(emptyList())
    private val cardsAdapter = CardsAdapter(emptyList())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DwDiscoveryFragmentUserDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recycler setup
        binding.rvOutlets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = outletsAdapter
        }

        binding.rvCards.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = cardsAdapter
        }

        // Load data
        loadVendorDetail()


        // Terms button
        binding.btnTerms.setOnClickListener {
            val terms = vendorDetail?.termsAndConditions ?: "No terms available"
            showTermsPopup(terms)
        }

        // Back button
        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun loadBranches() {
        viewLifecycleOwner.lifecycleScope.launch {
            val branches = vendorDetail?.branchesList

            if (_binding == null) return@launch

            branches?.let {
                if (it.isNotEmpty()) {
                    binding.tvOutlets.visibility = View.VISIBLE
                    outletsAdapter.updateData(it)
                } else {
                    binding.tvOutlets.visibility = View.GONE
                }
            }
        }
    }

    private fun loadCards() {
        viewLifecycleOwner.lifecycleScope.launch {
            val cardDiscounts = vendorDetail?.cardDiscountsList

            if (_binding == null) return@launch

            cardDiscounts?.let {
                if (it.isNotEmpty()) {
                    binding.tvDiscountCard.visibility = View.VISIBLE
                    cardsAdapter.updateData(it)
                } else {
                    binding.tvDiscountCard.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadVendorDetail() {

        viewLifecycleOwner.lifecycleScope.launch {
            if (_binding == null) return@launch
            binding.loaderLayout.visibility = View.VISIBLE

            try {
                val vendor = repository.getFullVendor(args.vendorId, args.cityId)
                val banners = repository.getBanners() ?: emptyList()

                if (_binding == null) return@launch

                vendor?.let {

                    vendorDetail = it



                    binding.title.text = it.companyName
                    binding.offerTitle.text = it.title
                    binding.offerDesc.text = it.description
                    loadCards()
                    loadBranches()

                    val num = if (it.branchesCount == 0) "0"
                    else String.format("%02d", it.branchesCount)

                    binding.outletCount.text = "$num Outlets"

                    // Outlets adapter image
                    outletsAdapter.updateVendorUrl(it.logoUrl)

                    // Find banner for vendor
                    val banner = banners.firstOrNull { b ->
                        b.vendorId == it.id
                    }

                    // ✅ ROUNDED IMAGE LOADING
                    Glide.with(requireContext())
                        .load(banner?.imageUrl ?: it.logoUrl)
                        .apply(
                            RequestOptions()
                                .transform(
                                    CenterCrop(),
                                    RoundedCorners(24) // 👈 Rounded corners
                                )
                        )
                        .placeholder(R.drawable.dw_discovery_ic_banner)
                        .error(R.drawable.dw_discovery_ic_banner)
                        .into(binding.bannerImage)
                }
            } catch (e: Exception) {
                println("Error loading vendor detail: ${e.message}")
            } finally {
                _binding?.loaderLayout?.visibility = View.GONE
            }
        }
    }

    private fun showTermsPopup(terms: String) {

        val dialog = Dialog(requireContext(), R.style.DwDiscovery_CenterDialogTheme)
        dialog.setContentView(R.layout.dw_discovery_dialog_terms_conditions)

        val tvTerms = dialog.findViewById<TextView>(R.id.tvTerms)

        // Just show text normally without bullets
        tvTerms.text = terms

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.CENTER)

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}