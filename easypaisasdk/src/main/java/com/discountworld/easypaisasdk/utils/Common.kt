package com.discountworld.easypaisasdk.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.Toast
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.google.android.material.snackbar.Snackbar
import java.io.InputStream
import java.util.Locale

fun isAtLeastVersion(version: Int): Boolean {
    return Build.VERSION.SDK_INT >= version
}

fun Context.nativeLocale() = Locale(getStringPreference(com.discountworld.easypaisasdk.variables.Constants.APP_LOCALE))

fun Context.toast(message: String){
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.snackbar(message: String){
    Snackbar.make(this, message, Snackbar.LENGTH_LONG).show()
}

fun ProgressBar.show(){
    visibility = View.VISIBLE
}

fun ProgressBar.hide(){
    visibility = View.GONE
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun Context.isOnline() : Boolean {
    val netInfo = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val capabilities = netInfo.getNetworkCapabilities(netInfo.activeNetwork)
        if(capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    return true
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    return true
                }
            }
        }
    } else {
        val activeNetworkInfo = netInfo.activeNetworkInfo
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
            return true
        }
    }
    return false
}

fun Context.isTablet(): Boolean {
    return this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
}

fun Context.loadJSONFromAsset(jsonPath: String?): String? {
    val istream : InputStream = this.assets.open(jsonPath!!)
    val size: Int = istream.available()
    val buffer = ByteArray(size)
    istream.read(buffer)
    istream.close()
    return String(buffer, charset("UTF-8"))
}


fun Int.toPx(): Int =
    (this * Resources.getSystem().displayMetrics.density).toInt()

fun getShimmerDrawable(): ShimmerDrawable {
    val shimmer = Shimmer.ColorHighlightBuilder()
        .setBaseColor(android.graphics.Color.parseColor("#E2E2E2"))
        .setHighlightColor(android.graphics.Color.parseColor("#F5F5F5"))
        .setDuration(1200L)
        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
        .setAutoStart(true)
        .build()

    return ShimmerDrawable().apply {
        setShimmer(shimmer)
    }
}

