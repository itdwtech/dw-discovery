package com.discountworld.dwapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _isBottomNavVisible = MutableLiveData<Boolean>(true)
    val isBottomNavVisible: LiveData<Boolean> get() = _isBottomNavVisible

    fun updateBottomNavVisibility(destinationId: Int, hideBottomNavArg: Boolean = false) {
        when (destinationId) {
            com.discountworld.dwapp.R.id.nav_home,
            com.discountworld.dwapp.R.id.nav_locations,
            com.discountworld.dwapp.R.id.nav_promos,
            com.discountworld.dwapp.R.id.nav_history,
            com.discountworld.dwapp.R.id.nav_profile -> {
                _isBottomNavVisible.value = true
            }
            com.discountworld.dwapp.R.id.nav_delivery -> {
                _isBottomNavVisible.value = !hideBottomNavArg
            }
            else -> {
                _isBottomNavVisible.value = false
            }
        }
    }
}
