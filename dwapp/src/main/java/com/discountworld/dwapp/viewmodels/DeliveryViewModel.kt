package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.RedemptionVendorSummary
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class DeliveryUiState {
    object Idle : DeliveryUiState()
    object Loading : DeliveryUiState()
    data class Success(val vendors: List<RedemptionVendorSummary>) : DeliveryUiState()
    data class Error(val message: String) : DeliveryUiState()
}

class DeliveryViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<DeliveryUiState>(DeliveryUiState.Idle)
    val uiState: LiveData<DeliveryUiState> get() = _uiState

    fun loadVendors(
        cityId: Long,
        searchQuery: String? = null,
        categoryId: Long? = null,
        inStore: Boolean? = null,
        delivery: Boolean? = null,
        ecommerce: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.value = DeliveryUiState.Loading
            val query = searchQuery?.ifEmpty { null }
            val response = repository.listVendors(
                page = 1,
                pageSize = 20,
                cityId = cityId,
                search = query,
                categoryId = if (categoryId != null && categoryId != -1L) categoryId else null,
                inStore = inStore,
                delivery = delivery,
                ecommerce = ecommerce
            )
            val vendors = response?.vendorsList ?: emptyList()
            _uiState.value = DeliveryUiState.Success(vendors)
        }
    }
}
