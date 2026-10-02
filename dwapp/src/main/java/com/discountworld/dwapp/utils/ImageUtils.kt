package com.discountworld.dwapp.utils

import android.util.Log

fun String?.fixImageUrl(): String? {
    if (this.isNullOrEmpty()) return this

    var fixedUrl = this.replace("localhost", "192.168.0.101")

    // Agar admin portal poora URL bhejne ke bajaye sirf relative path bhej raha ho (jaise "/images/pic.png")
    if (fixedUrl.startsWith("/")) {
        // Stage environment par point karein
        fixedUrl = "https://stg-api.appbiance.com$fixedUrl"
    }

    // Is se aap Logcat mein dekh sakenge ke backend se URL kya aa raha hai aur hum kya bana rahe hain
    Log.d("ImageUtils", "Original: $this  --->  Fixed: $fixedUrl")

    return fixedUrl
}
