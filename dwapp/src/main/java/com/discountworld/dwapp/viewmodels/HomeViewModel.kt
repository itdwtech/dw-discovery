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

    private var popularCurrentPage = 1
    private val popularPageSize = 10
    private var popularIsLastPage = false
    private var isPopularLoading = false
    private val allPopularVendors = mutableListOf<RedemptionVendorSummary>()

    fun setSelectedCityId(cityId: Long) {
        selectedCityId = cityId
    }

    fun getSelectedCityId(): Long = selectedCityId

    fun loadHomeData(cityId: Long) {
        selectedCityId = cityId
        popularCurrentPage = 1
        popularIsLastPage = false
        isPopularLoading = false
        allPopularVendors.clear()

        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true) ?: HomeUiState(isLoading = true)

            val storiesDeferred = async { repository.listStories(cityId) }
            val popularVendorsDeferred = async { repository.listVendors(page = 1, pageSize = popularPageSize, cityId = cityId) }
            val featuredVendorsDeferred = async { repository.listVendors(page = 1, pageSize = 20, cityId = cityId, featured = true) }
            val bannersDeferred = async { repository.listBanners(cityId) }

            val stories = storiesDeferred.await() ?: emptyList()
            val featuredVendors = featuredVendorsDeferred.await()?.vendorsList ?: emptyList()
            val bannerResponse = bannersDeferred.await()
            val bannerItems = bannerResponse?.bannersList ?: emptyList()

            val popularResponse = popularVendorsDeferred.await()
            val initialPopularVendors = popularResponse?.vendorsList ?: emptyList()
            val totalCount = popularResponse?.totalCount ?: 0

            allPopularVendors.addAll(initialPopularVendors)
            popularIsLastPage = if (totalCount > 0) allPopularVendors.size >= totalCount else initialPopularVendors.size < popularPageSize

            _uiState.value = HomeUiState(
                isLoading = false,
                stories = stories,
                banners = bannerItems,
                featuredVendors = featuredVendors,
                popularVendors = allPopularVendors.toList()
            )
        }
    }

    fun loadNextPopularPage() {
        if (isPopularLoading || popularIsLastPage) return

        isPopularLoading = true

        viewModelScope.launch {
            val nextPage = popularCurrentPage + 1
            val popularResponse = repository.listVendors(page = nextPage, pageSize = popularPageSize, cityId = selectedCityId)
            val newVendors = popularResponse?.vendorsList ?: emptyList()
            val totalCount = popularResponse?.totalCount ?: 0

            if (newVendors.isNotEmpty()) {
                popularCurrentPage = nextPage
                allPopularVendors.addAll(newVendors)
                popularIsLastPage = if (totalCount > 0) allPopularVendors.size >= totalCount else newVendors.size < popularPageSize
            } else {
                popularIsLastPage = true
            }

            isPopularLoading = false
            _uiState.value = _uiState.value?.copy(popularVendors = allPopularVendors.toList())
        }
    }
}
