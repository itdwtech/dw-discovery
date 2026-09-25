package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.discountworld.discount.CustomerRedemptionItem
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.launch

sealed class HistoryUiState {
    object Idle : HistoryUiState()
    object Loading : HistoryUiState()
    object LoadingMore : HistoryUiState()
    data class Success(
        val history: List<CustomerRedemptionItem>,
        val totalCount: Long,
        val isLastPage: Boolean = false
    ) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<HistoryUiState>(HistoryUiState.Idle)
    val uiState: LiveData<HistoryUiState> get() = _uiState

    private var currentSearchQuery: String? = null
    private var currentPage = 0
    private val pageSize = 10
    private var isLastPage = false
    private var isLoading = false
    private val allHistoryItems = mutableListOf<CustomerRedemptionItem>()
    private var totalCount: Long = 0

    fun loadHistory(searchQuery: String? = null) {
        currentSearchQuery = searchQuery?.ifEmpty { null }
        currentPage = 0
        isLastPage = false
        isLoading = true
        allHistoryItems.clear()

        _uiState.value = HistoryUiState.Loading

        viewModelScope.launch {
            val response = repository.listCustomerRedemptions(
                page = currentPage,
                pageSize = pageSize,
                search = currentSearchQuery
            )

            val items = response?.itemsList ?: emptyList()
            val respTotalCount = response?.totalCount ?: 0L
            val paginationTotalCount = response?.pagination?.totalCount?.toLong() ?: 0L
            totalCount = if (respTotalCount > 0) respTotalCount else if (paginationTotalCount > 0) paginationTotalCount else items.size.toLong()

            allHistoryItems.addAll(items)
            isLastPage = if (totalCount > 0) allHistoryItems.size.toLong() >= totalCount else items.size < pageSize
            isLoading = false

            _uiState.value = HistoryUiState.Success(allHistoryItems.toList(), totalCount, isLastPage)
        }
    }

    fun loadNextPage() {
        if (isLoading || isLastPage) return

        isLoading = true
        _uiState.value = HistoryUiState.LoadingMore

        val nextPage = currentPage + 1

        viewModelScope.launch {
            val response = repository.listCustomerRedemptions(
                page = nextPage,
                pageSize = pageSize,
                search = currentSearchQuery
            )

            val items = response?.itemsList ?: emptyList()
            val respTotalCount = response?.totalCount ?: 0L
            val paginationTotalCount = response?.pagination?.totalCount?.toLong() ?: 0L
            val newTotalCount = if (respTotalCount > 0) respTotalCount else if (paginationTotalCount > 0) paginationTotalCount else totalCount

            if (items.isNotEmpty()) {
                currentPage = nextPage
                totalCount = newTotalCount
                allHistoryItems.addAll(items)
                isLastPage = if (totalCount > 0) allHistoryItems.size.toLong() >= totalCount else items.size < pageSize
            } else {
                isLastPage = true
            }

            isLoading = false
            _uiState.value = HistoryUiState.Success(allHistoryItems.toList(), totalCount, isLastPage)
        }
    }
}
