package com.discountworld.dwapp.managers

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dw_app_prefs", Context.MODE_PRIVATE)

    init {
        getAuthToken()?.let { token ->
            if (token.isNotEmpty()) {
                RedemptionStubClient.setToken(token)
            }
        }
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SELECTED_CITY_ID = "selected_city_id"
        private const val KEY_REDEEMED_DEALS = "redeemed_deals"
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

    fun markDealAsRedeemed(dealId: Long) {
        val currentSet = prefs.getStringSet(KEY_REDEEMED_DEALS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(dealId.toString())
        prefs.edit().putStringSet(KEY_REDEEMED_DEALS, currentSet).apply()
    }

    fun markOfferAsRedeemed(key: String) {
        val currentSet = prefs.getStringSet(KEY_REDEEMED_DEALS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(key)
        prefs.edit().putStringSet(KEY_REDEEMED_DEALS, currentSet).apply()
    }

    fun isDealRedeemed(dealId: Long): Boolean {
        val currentSet = prefs.getStringSet(KEY_REDEEMED_DEALS, emptySet()) ?: emptySet()
        return currentSet.contains(dealId.toString())
    }

    fun isOfferRedeemed(key: String): Boolean {
        val currentSet = prefs.getStringSet(KEY_REDEEMED_DEALS, emptySet()) ?: emptySet()
        return currentSet.contains(key)
    }

    fun clearSession() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_SELECTED_CITY_ID).remove(KEY_REDEEMED_DEALS).apply()
        RedemptionStubClient.setToken("")
    }

    fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }
}
