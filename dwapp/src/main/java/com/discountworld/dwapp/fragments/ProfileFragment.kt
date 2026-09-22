package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentProfileBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.viewmodels.ProfileState
import com.discountworld.dwapp.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        observeViewModel()
        viewModel.loadProfile()

        binding.btnSaveChanges.setOnClickListener {
            val fullName = binding.etFullName.text.toString()
            val email = binding.etEmail.text.toString()
            val contact = binding.etContact.text.toString()
            viewModel.updateProfile(fullName, email, contact)
        }

        binding.btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Clear backstack
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
    }

    private fun observeViewModel() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProfileState.Loading -> {
                    binding.btnSaveChanges.isEnabled = false
                }
                is ProfileState.Success -> {
                    binding.btnSaveChanges.isEnabled = true
                    val profile = state.profile
                    binding.etFullName.setText(profile.fullName)
                    binding.etEmail.setText(profile.email)
                    binding.etContact.setText(profile.phoneNumber)
                    binding.tvProfileNameDisplay.text = profile.fullName
                }
                is ProfileState.UpdateSuccess -> {
                    binding.btnSaveChanges.isEnabled = true
                    binding.tvProfileNameDisplay.text = state.profile.fullName
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                is ProfileState.Error -> {
                    binding.btnSaveChanges.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                ProfileState.Idle -> {
                    binding.btnSaveChanges.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
