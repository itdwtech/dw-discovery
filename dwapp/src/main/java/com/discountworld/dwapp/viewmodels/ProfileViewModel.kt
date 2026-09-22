package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.Customer
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val profile: Customer) : ProfileState()
    data class UpdateSuccess(val profile: Customer) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _profileState = MutableLiveData<ProfileState>(ProfileState.Idle)
    val profileState: LiveData<ProfileState> get() = _profileState

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val profile = repository.getCustomerProfile()
            if (profile != null) {
                _profileState.value = ProfileState.Success(profile)
            } else {
                _profileState.value = ProfileState.Error("Failed to load profile")
            }
        }
    }

    fun updateProfile(fullName: String, email: String, contact: String) {
        if (fullName.isEmpty() || email.isEmpty() || contact.isEmpty()) {
            _profileState.value = ProfileState.Error("Please fill all fields")
            return
        }

        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val updatedProfile = repository.updateCustomerProfile(email, fullName, contact)
            if (updatedProfile != null) {
                _profileState.value = ProfileState.UpdateSuccess(updatedProfile)
            } else {
                _profileState.value = ProfileState.Error("Failed to update profile")
            }
        }
    }
}
