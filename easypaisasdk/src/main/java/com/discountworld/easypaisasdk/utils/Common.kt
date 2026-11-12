package com.discountworld.easypaisasdk.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.discountworld.easypaisasdk.R
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
    if (jsonPath.isNullOrEmpty()) return null
    return try {
        val istream: InputStream = this.assets.open(jsonPath)
        val size: Int = istream.available()
        val buffer = ByteArray(size)
        istream.read(buffer)
        istream.close()
        String(buffer, charset("UTF-8"))
    } catch (_: Exception) {
        null
    }
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

/**
 * Loads a banner image with disk+resource caching, explicit target dimensions to avoid
 * waiting for layout measurement, and configurable Glide request priority.
 */
fun ImageView.loadBannerImage(
    url: String?,
    priority: Priority = Priority.NORMAL,
    requestManager: RequestManager = Glide.with(this)
) {
    val fallbackWidth = (resources.displayMetrics.widthPixels - 32.toPx()).coerceAtLeast(1)
    val fallbackHeight = 200.toPx().coerceAtLeast(1)
    loadOptimizedImage(
        url = url,
        fallbackWidthPx = fallbackWidth,
        fallbackHeightPx = fallbackHeight,
        priority = priority,
        centerCrop = true,
        requestManager = requestManager
    )
}

/**
 * Loads an image through Glide backed by a pooled HTTP/2 OkHttpClient, skipping duplicate
 * requests when the view is already displaying or loading the exact same URL.
 */
fun ImageView.loadOptimizedImage(
    url: String?,
    fallbackWidthPx: Int,
    fallbackHeightPx: Int,
    priority: Priority = Priority.NORMAL,
    centerCrop: Boolean = false,
    errorResId: Int? = null,
    requestManager: RequestManager = Glide.with(this)
) {
    GlideImageLoader.ensureConfigured(context)

    if (url.isNullOrBlank()) {
        setTag(R.id.dw_discovery_glide_url_tag, null)
        requestManager.clear(this)
        if (errorResId != null) {
            setImageResource(errorResId)
        } else {
            setImageDrawable(getShimmerDrawable())
        }
        return
    }

    val existingUrl = getTag(R.id.dw_discovery_glide_url_tag) as? String
    if (existingUrl == url && drawable != null) {
        return
    }

    setTag(R.id.dw_discovery_glide_url_tag, url)

    val shimmer = getShimmerDrawable()
    val targetWidth = if (width > 0) width else fallbackWidthPx.coerceAtLeast(1)
    val targetHeight = if (height > 0) height else fallbackHeightPx.coerceAtLeast(1)

    var builder = requestManager
        .load(url)
        .priority(priority)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .override(targetWidth, targetHeight)
        .placeholder(shimmer)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean
            ): Boolean {
                if (getTag(R.id.dw_discovery_glide_url_tag) == url) {
                    setTag(R.id.dw_discovery_glide_url_tag, null)
                }
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>?,
                dataSource: DataSource,
                isFirstResource: Boolean
            ): Boolean = false
        })

    if (centerCrop) {
        builder = builder.centerCrop()
    }

    builder = if (errorResId != null) {
        builder.error(errorResId)
    } else {
        builder.error(shimmer)
    }

    builder.into(this)
}

