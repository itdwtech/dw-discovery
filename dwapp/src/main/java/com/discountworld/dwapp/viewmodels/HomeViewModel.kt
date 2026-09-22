package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.RedemptionBannerItem
import com.discountworld.discount.RedemptionStory
import com.discountworld.discount.RedemptionVendorSummary
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val stories: List<RedemptionStory> = emptyList(),
    val banners: List<RedemptionBannerItem> = emptyList(),
    val featuredVendors: List<RedemptionVendorSummary> = emptyList(),
    val popularVendors: List<RedemptionVendorSummary> = emptyList()
)

class HomeViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<HomeUiState>(HomeUiState())
    val uiState: LiveData<HomeUiState> get() = _uiState

    private var selectedCityId: Long = 1L

    fun setSelectedCityId(cityId: Long) {
        selectedCityId = cityId
    }

    fun getSelectedCityId(): Long = selectedCityId

    fun loadHomeData(cityId: Long) {
        selectedCityId = cityId
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true) ?: HomeUiState(isLoading = true)

            val storiesDeferred = async { repository.listStories(cityId) }
            val vendorsDeferred = async { repository.listVendors(page = 1, pageSize = 20, cityId = cityId) }
            val featuredVendorsDeferred = async { repository.listVendors(page = 1, pageSize = 20, cityId = cityId, featured = true) }
            val bannersDeferred = async { repository.listBanners(cityId) }

            val stories = storiesDeferred.await() ?: emptyList()
            val featuredVendors = featuredVendorsDeferred.await()?.vendorsList ?: emptyList()
            val bannerResponse = bannersDeferred.await()
            val bannerItems = bannerResponse?.bannersList ?: emptyList()
            val popularVendorsFromBanner = bannerResponse?.popularVendorsList ?: emptyList()
            val popularVendors = if (popularVendorsFromBanner.isNotEmpty()) {
                popularVendorsFromBanner
            } else {
                vendorsDeferred.await()?.vendorsList ?: emptyList()
            }

            _uiState.value = HomeUiState(
                isLoading = false,
                stories = stories,
                banners = bannerItems,
                featuredVendors = featuredVendors,
                popularVendors = popularVendors
            )
        }
    }
}
