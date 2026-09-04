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
        _binding = FragmentBrandDetailBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val vendorId =
            arguments?.getLong("vendor_id", -1L) ?: -1L

        val cityIdArg =
            arguments?.getLong("city_id", -1L) ?: -1L

        val selectedCityId =
            if (cityIdArg != -1L) {
                cityIdArg
            } else {
                sessionManager.getSelectedCityId()
            }

        if (vendorId != -1L) {
            loadVendorDetail(
                vendorId = vendorId,
                cityId = selectedCityId
            )
        } else {
            setupOffersRecyclerView(emptyList())
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.llInfo.setOnClickListener {

            val bundle = Bundle().apply {

                putLong(
                    "vendor_id",
                    vendorId
                )

                selectedCityId?.let {
                    putLong(
                        "city_id",
                        it
                    )
                }
            }

            findNavController().navigate(
                R.id.nav_brand_info,
                bundle
            )
        }
    }

    private fun loadVendorDetail(
        vendorId: Long,
        cityId: Long?
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val vendorDetailDeferred =
                    async {
                        redemptionRepository.getVendorDetail(
                            vendorId,
                            cityId
                        )
                    }

                val dealsDeferred =
                    async {
                        redemptionRepository.listVendorDeals(
                            vendorId
                        )
                    }

                val vendorDetail =
                    vendorDetailDeferred.await()

                currentVendorDetail =
                    vendorDetail

                vendorDetail?.let {

                    binding.tvBrandName.text =
                        it.title.ifEmpty {
                            it.companyName
                        }

                    vendorLogoUrl =
                        it.logoUrl

                    if (it.categoriesList.isNotEmpty()) {

                        binding.tvCategory.text =
                            it.categoriesList.joinToString(", ") { cat ->
                                cat.name
                            }
                    }

                    if (it.galleryImagesList.isNotEmpty()) {

                        Glide.with(requireContext())
                            .load(
                                it.galleryImagesList.first()
                            )
                            .placeholder(
                                R.drawable.ic_placeholder
                            )
                            .error(
                                R.drawable.ic_placeholder
                            )
                            .into(
                                binding.ivBanner
                            )

                    } else if (it.logoUrl.isNotEmpty()) {

                        Glide.with(requireContext())
                            .load(it.logoUrl)
                            .placeholder(
                                R.drawable.ic_placeholder
                            )
                            .error(
                                R.drawable.ic_placeholder
                            )
                            .into(
                                binding.ivBanner
                            )
                    }
                }

                val deals =
                    dealsDeferred.await()
                        ?: emptyList()

                setupOffersRecyclerView(
                    deals
                )

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    e.message
                        ?: "Failed to load vendor details",
                    Toast.LENGTH_SHORT
                ).show()

                setupOffersRecyclerView(
                    emptyList()
                )
            }
        }
    }

    private fun setupOffersRecyclerView(
        deals: List<RedemptionDealSummary>
    ) {

        binding.rvOffers.layoutManager =
            LinearLayoutManager(requireContext())

        if (deals.isNotEmpty()) {

            binding.rvOffers.adapter =
                OffersAdapter(
                    dealsList = deals,
                    isDealRedeemed = { id -> sessionManager.isDealRedeemed(id) }
                ) { deal, _ ->

                    handleOfferSelected(
                        deal,
                        null
                    )
                }

        } else {

            val dummyOffers =
                listOf(
                    Offer(
                        "4Regular Burgers combo + 4Hot Wings + 1Fries + 4Drinks + 1loaded Fries Beef/Chicken in Rs. 3449 (Original Price: Rs. 4865)",
                        "Deal 4",
                        isRedeemed = true
                    ),
                    Offer(
                        "Tripple Beef Burger with Drink in Rs. 1349 (Original Price: Rs. 2035)",
                        "Deal 3",
                        isRedeemed = false
                    ),
                    Offer(
                        "Any Classic Burger with Fries & Drink in Rs. 1099 (Orignal Price: Rs. 1469)",
                        "Deal 2",
                        isRedeemed = false
                    )
                )

            binding.rvOffers.adapter =
                OffersAdapter(
                    dummyOffers = dummyOffers,
                    isOfferRedeemed = { key -> sessionManager.isOfferRedeemed(key) }
                ) { _, offer ->

                    handleOfferSelected(
                        null,
                        offer
                    )
                }
        }
    }

    private fun handleOfferSelected(
        deal: RedemptionDealSummary?,
        offer: Offer?
    ) {

        val isInStore =
            currentVendorDetail?.inStore
                ?: false

        if (isInStore) {

            showInStoreOtpDialog(
                deal,
                offer
            )

        } else {

            showDeliveryConfirmationDialog(
                deal,
                offer
            )
        }
    }

    // ============================================================
    // IN-STORE REDEMPTION
    // ============================================================

    private fun showInStoreOtpDialog(
        deal: RedemptionDealSummary?,
        offer: Offer?
    ) {

        val ctx =
            requireContext()

        val dialog =
            Dialog(ctx)

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        val dialogBinding =
            DialogOfferRedemptionBinding.inflate(
                layoutInflater
            )

        dialog.setContentView(
            dialogBinding.root
        )

        dialog.setCancelable(false)

        dialog.setCanceledOnTouchOutside(
            false
        )

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // --------------------------------------------------------
        // Offer title
        // --------------------------------------------------------

        val offerTitleText =
            deal?.title?.ifEmpty {
                offer?.discount
            }
                ?: offer?.discount
                ?: "Buy 1 Get 1"

        dialogBinding.tvOfferTitle.text =
            offerTitleText

        // --------------------------------------------------------
        // Merchant logo
        // --------------------------------------------------------

        if (!vendorLogoUrl.isNullOrEmpty()) {

            Glide.with(ctx)
                .load(vendorLogoUrl)
                .placeholder(
                    R.drawable.ic_placeholder
                )
                .error(
                    R.drawable.ic_placeholder
                )
                .into(
                    dialogBinding.ivMerchantLogo
                )
        }

        // --------------------------------------------------------
        // PIN inputs
        // --------------------------------------------------------

        setupPinInputs(
            dialogBinding
        )

        // --------------------------------------------------------
        // Close
        // --------------------------------------------------------

        dialogBinding.cvClose.setOnClickListener {

            dialog.dismiss()
        }

        // --------------------------------------------------------
        // REDEEM
        // --------------------------------------------------------

        dialogBinding.btnRedeemNow.setOnClickListener {

            val pin =
                buildString {

                    append(
                        dialogBinding.etPin1.text.toString()
                    )

                    append(
                        dialogBinding.etPin2.text.toString()
                    )

                    append(
                        dialogBinding.etPin3.text.toString()
                    )

                    append(
                        dialogBinding.etPin4.text.toString()
                    )
                }

            // ----------------------------------------------------
            // Validate PIN length
            // ----------------------------------------------------

            if (pin.length != 4) {

                Toast.makeText(
                    ctx,
                    "Please enter 4-digit merchant code",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // ----------------------------------------------------
            // Prevent multiple API calls
            // ----------------------------------------------------

            dialogBinding.btnRedeemNow.isEnabled =
                false

            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    val dealId = deal?.id ?: 1L
                    val selectedCityId = sessionManager.getSelectedCityId()

                    val result = redemptionRepository.redeemDeal(
                        dealId = dealId,
                        redeemPin = pin,
                        cityId = selectedCityId
                    )

                    result.onSuccess { response ->
                        if (response.success || response.redemptionCode.isNotBlank()) {
                            if (deal != null) {
                                sessionManager.markDealAsRedeemed(deal.id)
                            } else if (offer != null) {
                                sessionManager.markOfferAsRedeemed(offer.discount)
                            }
                            val redemptionCode = response.redemptionCode
                            dialog.dismiss()

                            val vendorName = currentVendorDetail?.title?.ifEmpty {
                                currentVendorDetail?.companyName
                            } ?: "Merchant"

                            val bundle = Bundle().apply {
                                putString("redemptionCode", redemptionCode)
                                putString("vendorName", vendorName)
                                putString("vendorPhone", currentVendorDetail?.headOfficeNumber ?: "")
                                putString("vendorWebsite", "https://www.14thstreetpizza.com/")
                                putBoolean("isStoreRedemption", true)
                            }

                            findNavController().navigate(
                                R.id.action_nav_brand_detail_to_nav_redemption_success,
                                bundle
                            )
                        } else {
                            val msg = response.message.ifBlank { "Invalid merchant code. Please try again." }
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                            dialogBinding.btnRedeemNow.isEnabled = true
                        }
                    }.onFailure { exception ->
                        val errorMsg = when {
                            exception.message?.contains("UNAUTHENTICATED", ignoreCase = true) == true ->
                                "Session expired or not logged in. Please log in again."
                            exception.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                                "Cannot connect to server. Check server IP or network."
                            else -> exception.localizedMessage ?: "Redemption failed"
                        }
                        Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                        dialogBinding.btnRedeemNow.isEnabled = true
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        ctx,
                        e.message ?: "Redemption failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialogBinding.btnRedeemNow.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    // ============================================================
    // DELIVERY REDEMPTION
    // ============================================================

    private fun showDeliveryConfirmationDialog(
        deal: RedemptionDealSummary?,
        offer: Offer?
    ) {

        val ctx =
            requireContext()

        val dialog =
            Dialog(ctx)

        dialog.requestWindowFeature(
            Window.FEATURE_NO_TITLE
        )

        val dialogBinding =
            DialogDeliveryConfirmationBinding.inflate(
                layoutInflater
            )

        dialog.setContentView(
            dialogBinding.root
        )

        dialog.setCancelable(false)

        dialog.setCanceledOnTouchOutside(
            false
        )

        dialog.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // --------------------------------------------------------
        // Offer title
        // --------------------------------------------------------

        val offerTitleText =
            deal?.title?.ifEmpty {
                offer?.discount
            }
                ?: offer?.discount
                ?: "Flat 20% Off"

        dialogBinding.tvOfferTitle.text =
            offerTitleText

        // --------------------------------------------------------
        // Merchant logo
        // --------------------------------------------------------

        if (!vendorLogoUrl.isNullOrEmpty()) {

            Glide.with(ctx)
                .load(vendorLogoUrl)
                .placeholder(
                    R.drawable.ic_placeholder
                )
                .error(
                    R.drawable.ic_placeholder
                )
                .into(
                    dialogBinding.ivMerchantLogo
                )
        }

        // --------------------------------------------------------
        // Question
        // --------------------------------------------------------

        val isEcommerce =
            currentVendorDetail?.ecommerce
                ?: false

        val questionText =
            if (isEcommerce) {

                "Would you like to redeem our exclusive E-Commerce Deals?"

            } else {

                "Would you like to redeem our exclusive Delivery Deals?"
            }

        dialogBinding.tvQuestion.text =
            questionText

        // --------------------------------------------------------
        // Close
        // --------------------------------------------------------

        dialogBinding.cvClose.setOnClickListener {

            dialog.dismiss()
        }

        // --------------------------------------------------------
        // Cancel
        // --------------------------------------------------------

        dialogBinding.btnCancel.setOnClickListener {

            dialog.dismiss()
        }

        // --------------------------------------------------------
        // REDEEM NOW
        // --------------------------------------------------------

        dialogBinding.btnRedeemNow.setOnClickListener {

            // Prevent multiple clicks
            dialogBinding.btnRedeemNow.isEnabled =
                false

            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    val dealId = deal?.id ?: 1L
                    val selectedCityId = sessionManager.getSelectedCityId()

                    val result = redemptionRepository.redeemDeal(
                        dealId = dealId,
                        cityId = selectedCityId
                    )

                    result.onSuccess { response ->
                        if (response.success || response.redemptionCode.isNotBlank()) {
                            if (deal != null) {
                                sessionManager.markDealAsRedeemed(deal.id)
                            } else if (offer != null) {
                                sessionManager.markOfferAsRedeemed(offer.discount)
                            }
                            val redemptionCode = response.redemptionCode
                            dialog.dismiss()

                            val vendorName = currentVendorDetail?.title?.ifEmpty {
                                currentVendorDetail?.companyName
                            } ?: "Merchant"

                            val websiteLink = currentVendorDetail?.socialLinksList?.firstOrNull {
                                it.url.isNotBlank() &&
                                        (it.platform.equals("website", ignoreCase = true) || it.url.startsWith("http"))
                            }?.url ?: "https://www.14thstreetpizza.com/"

                            val bundle = Bundle().apply {
                                putString("redemptionCode", redemptionCode)
                                putString("vendorName", vendorName)
                                putString("vendorPhone", currentVendorDetail?.headOfficeNumber ?: "")
                                putString("vendorWebsite", websiteLink)
                                putBoolean("isStoreRedemption", false)
                            }

                            findNavController().navigate(
                                R.id.action_nav_brand_detail_to_nav_redemption_success,
                                bundle
                            )
                        } else {
                            val msg = response.message.ifBlank { "Redemption failed. Please try again." }
                            Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                            dialogBinding.btnRedeemNow.isEnabled = true
                        }
                    }.onFailure { exception ->
                        val errorMsg = when {
                            exception.message?.contains("UNAUTHENTICATED", ignoreCase = true) == true ->
                                "Session expired or not logged in. Please log in again."
                            exception.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
                                "Cannot connect to server. Check server IP or network."
                            else -> exception.localizedMessage ?: "Redemption failed"
                        }
                        Toast.makeText(ctx, errorMsg, Toast.LENGTH_LONG).show()
                        dialogBinding.btnRedeemNow.isEnabled = true
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        ctx,
                        e.message ?: "Redemption failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialogBinding.btnRedeemNow.isEnabled = true
                }
            }
        }

        dialog.show()
    }

    // ============================================================
    // PIN INPUTS
    // ============================================================

    private fun setupPinInputs(
        binding: DialogOfferRedemptionBinding
    ) {

        val pins =
            arrayOf(
                binding.etPin1,
                binding.etPin2,
                binding.etPin3,
                binding.etPin4
            )

        for (i in pins.indices) {

            // ----------------------------------------------------
            // Text watcher
            // ----------------------------------------------------

            pins[i].addTextChangedListener(
                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        if (
                            s?.length == 1 &&
                            i < pins.size - 1
                        ) {

                            pins[i + 1]
                                .requestFocus()
                        }
                    }

                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                    }
                }
            )

            // ----------------------------------------------------
            // Backspace handling
            // ----------------------------------------------------

            pins[i].setOnKeyListener {
                    _,
                    keyCode,
                    event ->

                if (
                    keyCode ==
                    KeyEvent.KEYCODE_DEL &&
                    event.action ==
                    KeyEvent.ACTION_DOWN
                ) {

                    if (
                        pins[i].text.isNullOrEmpty() &&
                        i > 0
                    ) {

                        pins[i - 1]
                            .requestFocus()
                    }
                }

                false
            }
        }
    }

    // ============================================================
    // DESTROY VIEW
    // ============================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}