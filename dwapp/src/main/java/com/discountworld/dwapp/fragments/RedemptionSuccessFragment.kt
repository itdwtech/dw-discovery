package com.discountworld.dwapp.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentRedemptionSuccessBinding
import com.discountworld.dwapp.viewmodels.RedemptionSuccessViewModel

class RedemptionSuccessFragment : Fragment() {

    private var _binding: FragmentRedemptionSuccessBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RedemptionSuccessViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRedemptionSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val redemptionCode = arguments?.getString("redemptionCode") ?: "6FF92FBB"
        val vendorName = arguments?.getString("vendorName") ?: "Merchant"
        val vendorPhone = arguments?.getString("vendorPhone") ?: "021111363636"
        val vendorWebsite = arguments?.getString("vendorWebsite")
        val isStoreRedemption = arguments?.getBoolean("isStoreRedemption") ?: false

        val customMessage1 = arguments?.getString("customMessage1")
        val customMessage2 = arguments?.getString("customMessage2")

        binding.tvRedemptionCode.text = redemptionCode

        if (!customMessage1.isNullOrEmpty() || !customMessage2.isNullOrEmpty()) {
            binding.tvCodeInstruction.text = customMessage1 ?: ""
            binding.tvCodeFooter.text = customMessage2 ?: ""
        } else {
            if (isStoreRedemption) {
                binding.tvCodeInstruction.text = "Merchant will use this code"
                binding.tvCodeFooter.text = "to redeem the offer"
            } else {
                binding.tvCodeInstruction.text = "Enter this code on $vendorName's website"
                binding.tvCodeFooter.text = "to avail this offer"
            }
        }

        if (vendorWebsite.isNullOrBlank()) {
            binding.cvWebsiteBtn.visibility = View.GONE
        } else {
            binding.cvWebsiteBtn.visibility = View.VISIBLE
        }

        val copyAction = View.OnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Redemption Code", redemptionCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Code $redemptionCode copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
        binding.ivCopyCode.setOnClickListener(copyAction)
        binding.llCodeBox.setOnClickListener(copyAction)

        binding.cvWebsiteBtn.setOnClickListener {
            if (!vendorWebsite.isNullOrBlank()) {
                var url = vendorWebsite
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        binding.cvHelplineBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$vendorPhone"))
            startActivity(intent)
        }

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnHome.setOnClickListener {
            findNavController().navigate(R.id.nav_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
