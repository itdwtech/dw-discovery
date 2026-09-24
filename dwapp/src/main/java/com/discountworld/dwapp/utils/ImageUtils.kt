package com.discountworld.dwapp.utils

import android.util.Log

fun String?.fixImageUrl(): String? {
    if (this.isNullOrEmpty()) return this

    var fixedUrl = this.replace("localhost", "192.168.0.106")

    // Agar admin portal poora URL bhejne ke bajaye sirf relative path bhej raha ho (jaise "/images/pic.png")
    if (fixedUrl.startsWith("/")) {
        // Yahan aapko apne image server ka port likhna parh sakta hai agar wo 80 nahi hai (e.g., :8080)
        fixedUrl = "http://192.168.0.106$fixedUrl"
    }

    // Is se aap Logcat mein dekh sakenge ke backend se URL kya aa raha hai aur hum kya bana rahe hain
    Log.d("ImageUtils", "Original: $this  --->  Fixed: $fixedUrl")

    return fixedUrl
}
