package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentLoginBinding
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.viewmodels.AuthState
import com.discountworld.dwapp.viewmodels.LoginViewModel

class LoginFragment : Fragment() {

    companion object {
        // CODE MEIN PHONE NUMBER AUR TIER YAHAN SET KAREIN:
        const val PHONE_NUMBER = "12345678923"
        const val CUSTOMER_TIER = "Silver" // Options: "Gold", "Silver", or "Bronze"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        val intentPhone = activity?.intent?.getStringExtra("phone_number")
        val intentTier = activity?.intent?.getStringExtra("customer_tier")

        val phoneToUse = intentPhone?.ifEmpty { null } ?: PHONE_NUMBER
        val tierToUse = intentTier?.ifEmpty { null } ?: CUSTOMER_TIER

        // If tier in SessionManager doesn't match the new code/intent tier, clear session to re-auth
        if (!intentPhone.isNullOrEmpty() || !intentTier.isNullOrEmpty() || !sessionManager.getCustomerTier().equals(tierToUse, ignoreCase = true)) {
            sessionManager.clearSession()
        }

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.getAuthToken()!!
            RedemptionStubClient.setToken(token)
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        observeViewModel()

        // Auto authenticate immediately on launch using code or intent parameters
        viewModel.authenticateByPhone(phoneToUse, tierToUse)

        binding.btnSignIn.setOnClickListener {
            viewModel.authenticateByPhone(phoneToUse, tierToUse)
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    val tier = state.response.customer.customerTier.ifEmpty { CUSTOMER_TIER }
                    sessionManager.saveCustomerTier(tier)
                    RedemptionStubClient.setToken(state.response.accessToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.SuccessPhone -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    val tier = state.response.customer.customerTier.ifEmpty { CUSTOMER_TIER }
                    sessionManager.saveCustomerTier(tier)
                    RedemptionStubClient.setToken(state.response.accessToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.llWelcome.visibility = View.VISIBLE
                    binding.btnSignIn.visibility = View.VISIBLE
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
