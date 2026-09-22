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

data class PromosUiState(
    val isLoading: Boolean = false,
    val banners: List<RedemptionBannerItem> = emptyList(),
    val featuredVendors: List<RedemptionVendorSummary> = emptyList(),
    val stories: List<RedemptionStory> = emptyList()
)

class PromosViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<PromosUiState>(PromosUiState())
    val uiState: LiveData<PromosUiState> get() = _uiState

    fun loadPromosData(cityId: Long) {
        viewModelScope.launch {
            _uiState.value = PromosUiState(isLoading = true)

            val bannersDeferred = async { repository.listBanners(cityId) }
            val featuredVendorsDeferred = async { repository.listVendors(page = 1, pageSize = 20, cityId = cityId, featured = true) }
            val storiesDeferred = async { repository.listStories(cityId) }

            val bannerResponse = bannersDeferred.await()
            val bannerItems = bannerResponse?.bannersList ?: emptyList()
            val featuredVendors = featuredVendorsDeferred.await()?.vendorsList ?: emptyList()
            val stories = storiesDeferred.await() ?: emptyList()

            _uiState.value = PromosUiState(
                isLoading = false,
                banners = bannerItems,
                featuredVendors = featuredVendors,
                stories = stories
            )
        }
    }
}
