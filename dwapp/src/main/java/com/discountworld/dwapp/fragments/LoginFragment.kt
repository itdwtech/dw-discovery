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
        // CODE MEIN UNIQUE ID AUR TIER YAHAN SET KAREIN:
        const val UNIQUE_ID = "123456789012" // Must be a valid 12-digit Unique ID
        const val CUSTOMER_TIER = "Bronze" // Options: "Gold", "Silver", or "Bronze"
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

        val intentUniqueId = activity?.intent?.getStringExtra("unique_id")
        val intentTier = activity?.intent?.getStringExtra("customer_tier")

        val uniqueIdToUse = intentUniqueId ?: UNIQUE_ID
        val tierToUse = intentTier ?: CUSTOMER_TIER

        if (uniqueIdToUse.isEmpty()) {
            requireActivity().finish()
            return
        }

        // If saved session credentials don't match active code/intent, clear session to re-auth
        val currentSavedUniqueId = sessionManager.getUniqueId()

        if (!currentSavedUniqueId.equals(uniqueIdToUse, ignoreCase = true)) {
            sessionManager.clearSession()
        }

        // Check if already logged in with matching session
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.getAuthToken()!!
            RedemptionStubClient.setToken(token)
            navigateToHome()
            return
        }

        observeViewModel(uniqueIdToUse, tierToUse)

        // Login strictly based on Unique ID and Tier
        viewModel.authenticateByUniqueId(
            uniqueId = uniqueIdToUse,
            customerTier = tierToUse
        )
    }

    private fun navigateToHome() {
        if (isAdded && findNavController().currentDestination?.id == R.id.loginFragment) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    private fun observeViewModel(uniqueIdToUse: String, tierToUse: String) {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            val currentBinding = _binding ?: return@observe
            when (state) {
                is AuthState.Loading -> {
                    currentBinding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.SuccessUniqueId -> {
                    currentBinding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    sessionManager.saveUniqueId(uniqueIdToUse)
                    if (state.response.customer.phoneNumber.isNotEmpty()) {
                        sessionManager.savePhone(state.response.customer.phoneNumber)
                    }
                    val tier = state.response.customer.customerTier.ifEmpty { tierToUse }
                    sessionManager.saveCustomerTier(tier)
                    RedemptionStubClient.setToken(state.response.accessToken)
                    navigateToHome()
                }
                is AuthState.Error -> {
                    currentBinding.progressBar.visibility = View.VISIBLE
                    if (isAdded) {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                    // Auto retry after 4 seconds on error
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded && !sessionManager.isLoggedIn()) {
                            viewModel.authenticateByUniqueId(
                                uniqueId = uniqueIdToUse,
                                customerTier = tierToUse
                            )
                        }
                    }, 4000)
                }
                AuthState.Idle -> {
                    currentBinding.progressBar.visibility = View.VISIBLE
                }
                else -> {
                    // Ignore legacy auth responses
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
