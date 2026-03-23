package com.example.easypaisasdk.utils

import android.content.Context
import android.graphics.Typeface
import java.util.*


class TypeFaceUtils {

    companion object{
        private val fontCache: Hashtable<String, Typeface> = Hashtable<String, Typeface>()
        private var typeFaceError = false

        fun get(context: Context, name: String): Typeface? {
            if (typeFaceError) return null
            var tf = fontCache[name]
            if (tf == null) {
                try {
                    tf = Typeface.createFromAsset(context.assets, "fonts/$name")
                } catch (e: Exception) { }
                fontCache.put(name, tf)
            }
            return tf
        }

        fun initializeFontCache(mContext: Context?) {
            val thread = Thread {
                try {
                    if (mContext != null) {
                        get(mContext, "")
                    }
                } catch (e: java.lang.Exception) {
                    typeFaceError = true
                }
            }
            thread.start()
        }
    }

}