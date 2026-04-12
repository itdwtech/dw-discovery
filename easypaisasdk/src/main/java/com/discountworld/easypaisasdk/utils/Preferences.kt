package com.discountworld.easypaisasdk.utils

import android.content.Context
import android.content.SharedPreferences

private val PREFS_NAME = "dw_discovery_prefs"

fun Context.setPreference(key : String , value : String) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.putString(key,value)
    editor.apply()
    return editor.commit()
}

fun Context.setPreference(key : String , value : Long) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.putLong(key,value)
    editor.apply()
    return editor.commit()
}

fun Context.setPreference(key : String , value : Int) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.putInt(key,value)
    editor.apply()
    return editor.commit()
}

fun Context.setPreference(key : String , value : Float) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.putFloat(key,value)
    editor.apply()
    return editor.commit()
}

fun Context.setPreference(key : String , value : Boolean) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.putBoolean(key,value)
    editor.apply()
    return editor.commit()
}

fun Context.removePreference(key : String) {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    val editor:SharedPreferences.Editor =  sharedPreferences.edit()
    editor.remove(key).commit()
}

fun Context.isPreference(key : String) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.contains(key)
}

fun Context.getStringPreference(key : String) : String {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getString(key,"").toString()
}

fun Context.getLongPreference(key : String) : Long {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getLong(key,0)
}

fun Context.getIntPreference(key : String) : Int {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getInt(key,0)
}

fun Context.getIntPreference(key : String,default : Int) : Int {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getInt(key,default)
}

fun Context.getFloatPreference(key : String) : Float {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getFloat(key, 0.0f)
}

fun Context.getBooleanPreference(key : String) : Boolean {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(key,false)
}

