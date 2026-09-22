package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.RedemptionVendorDetail
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class BrandInfoState {
    object Idle : BrandInfoState()
    object Loading : BrandInfoState()
    data class Success(val vendorDetail: RedemptionVendorDetail) : BrandInfoState()
    data class Error(val message: String) : BrandInfoState()
}

class BrandInfoViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _brandInfoState = MutableLiveData<BrandInfoState>(BrandInfoState.Idle)
    val brandInfoState: LiveData<BrandInfoState> get() = _brandInfoState

    fun loadBrandInfo(vendorId: Long, cityId: Long) {
        viewModelScope.launch {
            _brandInfoState.value = BrandInfoState.Loading
            val vendor = repository.getVendorDetail(vendorId, cityId)
            if (vendor != null) {
                _brandInfoState.value = BrandInfoState.Success(vendor)
            } else {
                _brandInfoState.value = BrandInfoState.Error("Failed to load brand info")
            }
        }
    }
}
