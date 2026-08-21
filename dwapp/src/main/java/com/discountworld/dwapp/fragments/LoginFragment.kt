package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentLoginBinding
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val repository = RedemptionRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCnicFormatting()

        binding.btnSignIn.setOnClickListener {
            val cnic = binding.etCnic.text.toString()
            
            if (cnic.length < 15) {
                Toast.makeText(requireContext(), "Please enter a valid CNIC", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(cnic)
        }
    }

    private fun performLogin(cnic: String) {
        lifecycleScope.launch {
            binding.btnSignIn.isEnabled = false
            // You might want to show a progress bar here if you have one
            
            val response = repository.authenticateByCnic(cnic)
            
            binding.btnSignIn.isEnabled = true
            
            if (response != null) {
                // Login successful
                RedemptionStubClient.setToken(response.accessToken)
                Toast.makeText(requireContext(), "Welcome ${response.customer.fullName}", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } else {
                // Login failed
                Toast.makeText(requireContext(), "Authentication failed. Check Logcat for 'Auth' or 'gRPC' tags.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupCnicFormatting() {
        binding.etCnic.filters = arrayOf(InputFilter.LengthFilter(15))

        binding.etCnic.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val input = s.toString().replace("-", "")
                val formatted = StringBuilder()

                for (i in input.indices) {
                    formatted.append(input[i])
                    if ((i == 4 || i == 11) && i != input.length - 1) {
                        formatted.append("-")
                    }
                }

                s?.replace(0, s.length, formatted.toString())
                isFormatting = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
