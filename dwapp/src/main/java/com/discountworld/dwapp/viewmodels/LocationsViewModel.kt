package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.RedemptionMapPin
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class LocationsUiState {
    object Idle : LocationsUiState()
    object Loading : LocationsUiState()
    data class Success(val pins: List<RedemptionMapPin>) : LocationsUiState()
    data class Error(val message: String) : LocationsUiState()
}

class LocationsViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<LocationsUiState>(LocationsUiState.Idle)
    val uiState: LiveData<LocationsUiState> get() = _uiState

    fun loadMapPins(cityId: Long, categoryId: Long? = null) {
        viewModelScope.launch {
            _uiState.value = LocationsUiState.Loading
            val catId = if (categoryId != null && categoryId != -1L) categoryId else null
            val pins = repository.listMapPins(cityId, catId) ?: emptyList()
            _uiState.value = LocationsUiState.Success(pins)
        }
    }
}
