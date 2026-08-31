package com.discountworld.dwapp.managers

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dw_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SELECTED_CITY_ID = "selected_city_id"
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        RedemptionStubClient.setToken(token)
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun saveSelectedCityId(cityId: Long) {
        prefs.edit().putLong(KEY_SELECTED_CITY_ID, cityId).apply()
    }

    fun getSelectedCityId(): Long? {
        val id = prefs.getLong(KEY_SELECTED_CITY_ID, -1L)
        return if (id != -1L) id else null
    }

    fun clearSession() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_SELECTED_CITY_ID).apply()
        RedemptionStubClient.setToken("")
    }

    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }
}
