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
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentRedemptionSuccessBinding

class RedemptionSuccessFragment : Fragment() {

    private var _binding: FragmentRedemptionSuccessBinding? = null
    private val binding get() = _binding!!

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
        var vendorWebsite = arguments?.getString("vendorWebsite") ?: "https://www.14thstreetpizza.com/"
        val isStoreRedemption = arguments?.getBoolean("isStoreRedemption") ?: false

        binding.tvRedemptionCode.text = redemptionCode

        if (isStoreRedemption) {
            binding.tvCodeInstruction.text = "Merchant will use this code"
            binding.tvCodeFooter.text = "to redeem the offer"
            binding.cvWebsiteBtn.visibility = View.GONE
        } else {
            binding.tvCodeInstruction.text = "Enter this code on $vendorName's website"
            binding.tvCodeFooter.text = "to avail this offer"
            binding.cvWebsiteBtn.visibility = View.VISIBLE
        }

        // Copy Code action
        val copyAction = View.OnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Redemption Code", redemptionCode)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Code $redemptionCode copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
        binding.ivCopyCode.setOnClickListener(copyAction)
        binding.llCodeBox.setOnClickListener(copyAction)

        // Website Button
        binding.cvWebsiteBtn.setOnClickListener {
            if (vendorWebsite.isNotEmpty()) {
                if (!vendorWebsite.startsWith("http://") && !vendorWebsite.startsWith("https://")) {
                    vendorWebsite = "https://$vendorWebsite"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(vendorWebsite))
                startActivity(intent)
            }
        }

        // Helpline Button
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
