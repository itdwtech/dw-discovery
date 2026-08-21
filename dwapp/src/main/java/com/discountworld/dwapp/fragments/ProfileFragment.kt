package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.discountworld.dwapp.R
import com.discountworld.dwapp.databinding.FragmentProfileBinding
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val repository = RedemptionRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnSaveChanges.setOnClickListener {
            updateProfile()
        }

        binding.btnLogout.setOnClickListener {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Clear backstack
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            val profile = repository.getCustomerProfile()
            if (profile != null) {
                binding.etFullName.setText(profile.fullName)
                binding.etEmail.setText(profile.email)
                binding.etContact.setText(profile.phoneNumber)
                binding.tvProfileNameDisplay.text = profile.fullName
            } else {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfile() {
        val fullName = binding.etFullName.text.toString()
        val email = binding.etEmail.text.toString()
        val contact = binding.etContact.text.toString()

        if (fullName.isEmpty() || email.isEmpty() || contact.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.btnSaveChanges.isEnabled = false
            val updatedProfile = repository.updateCustomerProfile(email, fullName, contact)
            binding.btnSaveChanges.isEnabled = true

            if (updatedProfile != null) {
                binding.tvProfileNameDisplay.text = updatedProfile.fullName
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}