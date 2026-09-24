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
    object LoadingMore : DeliveryUiState()
    data class Success(val vendors: List<RedemptionVendorSummary>, val isLastPage: Boolean = false) : DeliveryUiState()
    data class Error(val message: String) : DeliveryUiState()
}

class DeliveryViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<DeliveryUiState>(DeliveryUiState.Idle)
    val uiState: LiveData<DeliveryUiState> get() = _uiState

    private var currentCityId: Long = 1L
    private var currentSearchQuery: String? = null
    private var currentCategoryId: Long? = null
    private var currentInStore: Boolean? = null
    private var currentDelivery: Boolean? = null
    private var currentEcommerce: Boolean? = null

    private var currentPage = 1
    private val pageSize = 10
    private var isLastPage = false
    private var isLoading = false
    private val allVendors = mutableListOf<RedemptionVendorSummary>()

    fun loadVendors(
        cityId: Long,
        searchQuery: String? = null,
        categoryId: Long? = null,
        inStore: Boolean? = null,
        delivery: Boolean? = null,
        ecommerce: Boolean? = null
    ) {
        currentCityId = cityId
        currentSearchQuery = searchQuery?.ifEmpty { null }
        currentCategoryId = if (categoryId != null && categoryId != -1L) categoryId else null
        currentInStore = inStore
        currentDelivery = delivery
        currentEcommerce = ecommerce

        currentPage = 1
        isLastPage = false
        isLoading = true
        allVendors.clear()

        _uiState.value = DeliveryUiState.Loading

        viewModelScope.launch {
            val response = repository.listVendors(
                page = currentPage,
                pageSize = pageSize,
                cityId = currentCityId,
                search = currentSearchQuery,
                categoryId = currentCategoryId,
                inStore = currentInStore,
                delivery = currentDelivery,
                ecommerce = currentEcommerce
            )

            val vendors = response?.vendorsList ?: emptyList()
            val totalCount = response?.totalCount ?: 0

            allVendors.addAll(vendors)
            isLastPage = if (totalCount > 0) allVendors.size >= totalCount else vendors.size < pageSize
            isLoading = false

            _uiState.value = DeliveryUiState.Success(allVendors.toList(), isLastPage)
        }
    }

    fun loadNextPage() {
        if (isLoading || isLastPage) return

        isLoading = true
        _uiState.value = DeliveryUiState.LoadingMore

        val nextPage = currentPage + 1

        viewModelScope.launch {
            val response = repository.listVendors(
                page = nextPage,
                pageSize = pageSize,
                cityId = currentCityId,
                search = currentSearchQuery,
                categoryId = currentCategoryId,
                inStore = currentInStore,
                delivery = currentDelivery,
                ecommerce = currentEcommerce
            )

            val vendors = response?.vendorsList ?: emptyList()
            val totalCount = response?.totalCount ?: 0

            if (vendors.isNotEmpty()) {
                currentPage = nextPage
                allVendors.addAll(vendors)
                isLastPage = if (totalCount > 0) allVendors.size >= totalCount else vendors.size < pageSize
            } else {
                isLastPage = true
            }

            isLoading = false
            _uiState.value = DeliveryUiState.Success(allVendors.toList(), isLastPage)
        }
    }
}
