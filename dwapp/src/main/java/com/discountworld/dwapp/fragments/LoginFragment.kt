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
        const val CNIC = "" // Must be a valid 13-digit CNIC
        const val UNIQUE_ID = ""
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

        val intentCnic = activity?.intent?.getStringExtra("cnic")
            ?: activity?.intent?.getStringExtra("unique_id")
        val cnicToUse = intentCnic ?: CNIC.ifEmpty { UNIQUE_ID }

        if (cnicToUse.isEmpty()) {
            requireActivity().finish()
            return
        }

        // If saved session credentials don't match active code/intent, clear session to re-auth
        val currentSavedCnic = sessionManager.getCnic()

        if (!currentSavedCnic.equals(cnicToUse, ignoreCase = true)) {
            sessionManager.clearSession()
        }

        // Check if already logged in with matching session
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.getAuthToken()!!
            RedemptionStubClient.setToken(token)
            navigateToHome()
            return
        }

        observeViewModel(cnicToUse)

        // Login based on CNIC
        viewModel.authenticateByCnic(cnic = cnicToUse)
    }

    private fun observeViewModel(cnicToUse: String) {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is AuthState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    sessionManager.saveCnic(cnicToUse)
                    if (state.response.customer.phoneNumber.isNotEmpty()) {
                        sessionManager.savePhone(state.response.customer.phoneNumber)
                    }
                    RedemptionStubClient.setToken(state.response.accessToken)
                    navigateToHome()
                }
                is AuthState.SuccessUniqueId -> {
                    binding.progressBar.visibility = View.GONE
                    sessionManager.saveAuthToken(state.response.accessToken)
                    sessionManager.saveCnic(cnicToUse)
                    if (state.response.customer.phoneNumber.isNotEmpty()) {
                        sessionManager.savePhone(state.response.customer.phoneNumber)
                    }
                    RedemptionStubClient.setToken(state.response.accessToken)
                    navigateToHome()
                }
                is AuthState.Error -> {
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    // Auto retry after 4 seconds on error
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded && !sessionManager.isLoggedIn()) {
                            viewModel.authenticateByCnic(cnic = cnicToUse)
                        }
                    }, 4000)
                }
                AuthState.Idle -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                else -> {
                    // Ignore legacy auth responses
                }
            }
        }
    }

    private fun navigateToHome() {
        if (isAdded && findNavController().currentDestination?.id == R.id.loginFragment) {
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
