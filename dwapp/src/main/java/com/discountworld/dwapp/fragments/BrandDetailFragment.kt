package com.discountworld.dwapp.fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.discountworld.discount.RedemptionDealSummary
import com.discountworld.discount.RedemptionVendorDetail
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.OffersAdapter
import com.discountworld.dwapp.databinding.DialogDeliveryConfirmationBinding
import com.discountworld.dwapp.databinding.DialogOfferRedemptionBinding
import com.discountworld.dwapp.databinding.FragmentBrandDetailBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.models.Offer
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class BrandDetailFragment : Fragment() {

    private var _binding: FragmentBrandDetailBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager

    private var currentVendorDetail: RedemptionVendorDetail? = null
    private var vendorLogoUrl: String? = null

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

        sessionManager = SessionManager(requireContext())

        val vendorId = arguments?.getLong("vendor_id", -1L) ?: -1L
        val cityIdArg = arguments?.getLong("city_id", -1L) ?: -1L
        val selectedCityId = if (cityIdArg != -1L) cityIdArg else sessionManager.getSelectedCityId()

        if (vendorId != -1L) {
            loadVendorDetail(vendorId, selectedCityId)
        } else {
            setupOffersRecyclerView(emptyList())
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.llInfo.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("vendor_id", vendorId)
                selectedCityId?.let { putLong("city_id", it) }
            }
            findNavController().navigate(R.id.nav_brand_info, bundle)
        }
    }

    private fun loadVendorDetail(vendorId: Long, cityId: Long?) {
        viewLifecycleOwner.lifecycleScope.launch {
            val vendorDetailDeferred = async { redemptionRepository.getVendorDetail(vendorId, cityId) }
            val dealsDeferred = async { redemptionRepository.listVendorDeals(vendorId) }

            val vendorDetail = vendorDetailDeferred.await()
            currentVendorDetail = vendorDetail
            vendorDetail?.let {
                binding.tvBrandName.text = it.title.ifEmpty { it.companyName }
                vendorLogoUrl = it.logoUrl
                if (it.categoriesList.isNotEmpty()) {
                    binding.tvCategory.text = it.categoriesList.joinToString(", ") { cat -> cat.name }
                }
                if (it.galleryImagesList.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.galleryImagesList.first())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(binding.ivBanner)
                } else if (it.logoUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.logoUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(binding.ivBanner)
                }
            }

            val deals = dealsDeferred.await() ?: emptyList()
            setupOffersRecyclerView(deals)
        }
    }

    private fun setupOffersRecyclerView(deals: List<RedemptionDealSummary>) {
        binding.rvOffers.layoutManager = LinearLayoutManager(requireContext())
        if (deals.isNotEmpty()) {
            binding.rvOffers.adapter = OffersAdapter(dealsList = deals) { deal, _ ->
                handleOfferSelected(deal, null)
            }
        } else {
            val dummyOffers = listOf(
                Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
                Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
                Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1"),
                Offer("Buy 1 Whitening Facial Get 1 Haircut & Khat Free", "Buy 1 Get 1")
            )
            binding.rvOffers.adapter = OffersAdapter(dummyOffers = dummyOffers) { _, offer ->
                handleOfferSelected(null, offer)
            }
        }
    }

    private fun handleOfferSelected(deal: RedemptionDealSummary?, offer: Offer?) {
        val isInStore = currentVendorDetail?.inStore ?: false
        if (isInStore) {
            showInStoreOtpDialog(deal, offer)
        } else {
            showDeliveryConfirmationDialog(deal, offer)
        }
    }

    private fun showInStoreOtpDialog(deal: RedemptionDealSummary?, offer: Offer?) {
        val ctx = requireContext()
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogOfferRedemptionBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val offerTitleText = deal?.title?.ifEmpty { offer?.discount } ?: offer?.discount ?: "Buy 1 Get 1"
        dialogBinding.tvOfferTitle.text = offerTitleText

        if (!vendorLogoUrl.isNullOrEmpty()) {
            Glide.with(ctx)
                .load(vendorLogoUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(dialogBinding.ivMerchantLogo)
        }

        setupPinInputs(dialogBinding)

        dialogBinding.cvClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnRedeemNow.setOnClickListener {
            val pin = "${dialogBinding.etPin1.text}${dialogBinding.etPin2.text}${dialogBinding.etPin3.text}${dialogBinding.etPin4.text}"
            if (pin.length < 4) {
                Toast.makeText(ctx, "Please enter 4-digit merchant code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val dealId = deal?.id ?: 1L
                val selectedCityId = sessionManager.getSelectedCityId()
                val response = redemptionRepository.redeemDeal(dealId, redeemPin = pin, cityId = selectedCityId)

                val redemptionCode = response?.redemptionCode?.ifEmpty { "7SI1XZU7" } ?: "7SI1XZU7"
                dialog.dismiss()

                val vendorName = currentVendorDetail?.title?.ifEmpty { currentVendorDetail?.companyName } ?: "Merchant"
                val bundle = Bundle().apply {
                    putString("redemptionCode", redemptionCode)
                    putString("vendorName", vendorName)
                    putString("vendorPhone", currentVendorDetail?.headOfficeNumber ?: "021111363636")
                    putString("vendorWebsite", "https://www.14thstreetpizza.com/")
                    putBoolean("isStoreRedemption", true)
                }
                findNavController().navigate(R.id.action_nav_brand_detail_to_nav_redemption_success, bundle)
            }
        }

        dialog.show()
    }

    private fun showDeliveryConfirmationDialog(deal: RedemptionDealSummary?, offer: Offer?) {
        val ctx = requireContext()
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogBinding = DialogDeliveryConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val offerTitleText = deal?.title?.ifEmpty { offer?.discount } ?: offer?.discount ?: "Flat 20% Off"
        dialogBinding.tvOfferTitle.text = offerTitleText

        if (!vendorLogoUrl.isNullOrEmpty()) {
            Glide.with(ctx)
                .load(vendorLogoUrl)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .into(dialogBinding.ivMerchantLogo)
        }

        val isEcommerce = currentVendorDetail?.ecommerce ?: false
        val questionText = if (isEcommerce) {
            "Would you like to redeem our exclusive E-Commerce Deals?"
        } else {
            "Would you like to redeem our exclusive Delivery Deals?"
        }
        dialogBinding.tvQuestion.text = questionText

        dialogBinding.cvClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnRedeemNow.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val dealId = deal?.id ?: 1L
                val selectedCityId = sessionManager.getSelectedCityId()
                val response = redemptionRepository.redeemDeal(dealId, cityId = selectedCityId)

                val redemptionCode = response?.redemptionCode?.ifEmpty { "6FF92FBB" } ?: "6FF92FBB"
                dialog.dismiss()

                val vendorName = currentVendorDetail?.title?.ifEmpty { currentVendorDetail?.companyName } ?: "Merchant"
                val websiteLink = currentVendorDetail?.socialLinksList?.firstOrNull {
                    it.url.isNotBlank() && (it.platform.equals("website", ignoreCase = true) || it.url.startsWith("http"))
                }?.url ?: "https://www.14thstreetpizza.com/"

                val bundle = Bundle().apply {
                    putString("redemptionCode", redemptionCode)
                    putString("vendorName", vendorName)
                    putString("vendorPhone", currentVendorDetail?.headOfficeNumber ?: "021111363636")
                    putString("vendorWebsite", websiteLink)
                    putBoolean("isStoreRedemption", false)
                }
                findNavController().navigate(R.id.action_nav_brand_detail_to_nav_redemption_success, bundle)
            }
        }

        dialog.show()
    }

    private fun setupPinInputs(binding: DialogOfferRedemptionBinding) {
        val pins = arrayOf<EditText>(binding.etPin1, binding.etPin2, binding.etPin3, binding.etPin4)
        for (i in pins.indices) {
            pins[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < pins.size - 1) {
                        pins[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            pins[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (pins[i].text.isNullOrEmpty() && i > 0) {
                        pins[i - 1].requestFocus()
                    }
                }
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
