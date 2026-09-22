package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.CustomerCnicAuthResponse
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: CustomerCnicAuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

class LoginViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> get() = _authState

    fun authenticateByCnic(cnic: String) {
        if (cnic.length < 15) {
            _authState.value = AuthState.Error("Please enter a valid CNIC")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val response = repository.authenticateByCnic(cnic)
            if (response != null) {
                _authState.value = AuthState.Success(response)
            } else {
                _authState.value = AuthState.Error("Authentication failed. Check Logcat for 'Auth' or 'gRPC' tags.")
            }
        }
    }
}
