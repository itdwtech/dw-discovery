package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.RedeemDealResponse
import com.discountworld.discount.RedemptionDealSummary
import com.discountworld.discount.RedemptionVendorDetail
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

sealed class BrandDetailState {
    object Idle : BrandDetailState()
    object Loading : BrandDetailState()
    data class Success(
        val vendorDetail: RedemptionVendorDetail?,
        val deals: List<RedemptionDealSummary>
    ) : BrandDetailState()
    data class Error(val message: String) : BrandDetailState()
}

sealed class RedemptionState {
    object Idle : RedemptionState()
    object Loading : RedemptionState()
    data class Success(val response: RedeemDealResponse) : RedemptionState()
    data class Error(val message: String) : RedemptionState()
}

class BrandDetailViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _detailState = MutableLiveData<BrandDetailState>(BrandDetailState.Idle)
    val detailState: LiveData<BrandDetailState> get() = _detailState

    private val _redemptionState = MutableLiveData<RedemptionState>(RedemptionState.Idle)
    val redemptionState: LiveData<RedemptionState> get() = _redemptionState

    fun loadBrandDetail(vendorId: Long, cityId: Long) {
        viewModelScope.launch {
            _detailState.value = BrandDetailState.Loading

            val vendorDetailDeferred = async { repository.getVendorDetail(vendorId, cityId) }
            val dealsDeferred = async { repository.listVendorDeals(vendorId) }

            val vendorDetail = vendorDetailDeferred.await()
            val deals = dealsDeferred.await() ?: emptyList()

            if (vendorDetail != null || deals.isNotEmpty()) {
                _detailState.value = BrandDetailState.Success(vendorDetail, deals)
            } else {
                _detailState.value = BrandDetailState.Error("Failed to load vendor details")
            }
        }
    }

    fun redeemDeal(dealId: Long, cityId: Long, pinCode: String = "") {
        viewModelScope.launch {
            _redemptionState.value = RedemptionState.Loading
            val result = repository.redeemDeal(dealId = dealId, redeemPin = pinCode, cityId = cityId)
            result.onSuccess { response ->
                _redemptionState.value = RedemptionState.Success(response)
            }.onFailure { throwable ->
                _redemptionState.value = RedemptionState.Error(throwable.message ?: "Redemption failed")
            }
        }
    }

    fun resetRedemptionState() {
        _redemptionState.value = RedemptionState.Idle
    }
}
