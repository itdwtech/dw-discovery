package com.example.easypaisasdk.utils
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

object LocationUtility {

    interface LocationResultCallback {
        fun onLocationSuccess(latitude: Double, longitude: Double)
        fun onFailure(message: String)
    }

    fun getCurrentLocation(
        activity: Activity,
        callback: LocationResultCallback
    ) {

        if (!isInternetAvailable(activity)) {
            callback.onFailure("Internet not available")
            return
        }

        if (!isGPSEnabled(activity)) {
            callback.onFailure("GPS is disabled")
            return
        }

        if (!hasLocationPermission(activity)) {
            callback.onFailure("Location permission not granted")
            return
        }

        try {

            val fusedClient =
                LocationServices.getFusedLocationProviderClient(activity)

            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000
            )
                .setMaxUpdates(1)
                .build()

            fusedClient.requestLocationUpdates(
                locationRequest,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)

                        val location: Location? = result.lastLocation

                        if (location != null) {
                            callback.onLocationSuccess(
                                location.latitude,
                                location.longitude
                            )
                        } else {
                            callback.onFailure("Location null")
                        }
                    }
                },
                Looper.getMainLooper()
            )

        } catch (e: SecurityException) {
            callback.onFailure("Location permission error: ${e.message}")
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGPSEnabled(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}