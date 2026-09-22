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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentLoginBinding
import com.discountworld.dwapp.managers.RedemptionStubClient
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.viewmodels.AuthState
import com.discountworld.dwapp.viewmodels.LoginViewModel

class LoginFragment : Fragment() {

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

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            val token = sessionManager.getAuthToken()!!
            RedemptionStubClient.setToken(token)
            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            return
        }

        setupCnicFormatting()
        observeViewModel()

        binding.btnSignIn.setOnClickListener {
            val cnic = binding.etCnic.text.toString()
            viewModel.authenticateByCnic(cnic)
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {
                    binding.btnSignIn.isEnabled = false
                }
                is AuthState.Success -> {
                    binding.btnSignIn.isEnabled = true
                    sessionManager.saveAuthToken(state.response.accessToken)
                    Toast.makeText(requireContext(), "Welcome ${state.response.customer.fullName}", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                is AuthState.Error -> {
                    binding.btnSignIn.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }
                AuthState.Idle -> {
                    binding.btnSignIn.isEnabled = true
                }
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
