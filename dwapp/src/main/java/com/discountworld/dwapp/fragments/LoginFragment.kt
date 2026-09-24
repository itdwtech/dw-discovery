package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        const val PHONE_NUMBER = "03336688999"
        const val CUSTOMER_TIER = "Gold" // Options: "Gold", "Silver", or "Bronze"
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

        // If saved phone OR tier in SessionManager doesn't match active code/intent, clear session to re-auth
        val currentSavedPhone = sessionManager.getPhone()
        val currentSavedTier = sessionManager.getCustomerTier()

        if (!currentSavedPhone.equals(phoneToUse, ignoreCase = true) ||
            !currentSavedTier.equals(tierToUse, ignoreCase = true)
        ) {
            sessionManager.clearSession()
        }

        // Check if already logged in with matching session
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.getAuthToken()!!
            RedemptionStubClient.setToken(token)
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        observeViewModel(phoneToUse, tierToUse)

        // Direct code auto-authentication on launch
        viewModel.authenticateByPhone(phoneToUse, tierToUse)
    }

    private fun observeViewModel(phoneToUse: String, tierToUse: String) {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    sessionManager.savePhone(phoneToUse)
                    val tier = state.response.customer.customerTier.ifEmpty { tierToUse }
                    sessionManager.saveCustomerTier(tier)
                    RedemptionStubClient.setToken(state.response.accessToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.SuccessPhone -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    sessionManager.savePhone(phoneToUse)
                    val tier = state.response.customer.customerTier.ifEmpty { tierToUse }
                    sessionManager.saveCustomerTier(tier)
                    RedemptionStubClient.setToken(state.response.accessToken)
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    // Auto retry after 4 seconds on error
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded && !sessionManager.isLoggedIn()) {
                            viewModel.authenticateByPhone(phoneToUse, tierToUse)
                        }
                    }, 4000)
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
