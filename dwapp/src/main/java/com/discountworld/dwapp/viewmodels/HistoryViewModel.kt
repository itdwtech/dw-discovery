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
    data class Success(val history: List<CustomerRedemptionItem>, val totalCount: Long) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

class HistoryViewModel : ViewModel() {

    private val repository = RedemptionRepository()

    private val _uiState = MutableLiveData<HistoryUiState>(HistoryUiState.Idle)
    val uiState: LiveData<HistoryUiState> get() = _uiState

    fun loadHistory(searchQuery: String? = null) {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading
            val query = searchQuery?.ifEmpty { null }
            val response = repository.listCustomerRedemptions(search = query)
            val items = response?.itemsList ?: emptyList()
            val totalCount = response?.totalCount ?: items.size.toLong()
            _uiState.value = HistoryUiState.Success(items, totalCount)
        }
    }
}
