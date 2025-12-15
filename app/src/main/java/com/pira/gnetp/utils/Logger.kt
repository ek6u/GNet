package com.pira.gnetp.utils

import android.util.Log

object Logger {
    private const val TAG = "GNet"
    
    fun d(tag: String, message: String) {
        Log.d("$TAG:$tag", message)
    }
    
    fun i(tag: String, message: String) {
        Log.i("$TAG:$tag", message)
    }
    
    fun w(tag: String, message: String) {
        Log.w("$TAG:$tag", message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG:$tag", message, throwable)
        } else {
            Log.e("$TAG:$tag", message)
        }
    }
}