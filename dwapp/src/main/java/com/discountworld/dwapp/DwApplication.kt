package com.discountworld.dwapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class DwApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force Light Mode globally across the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
