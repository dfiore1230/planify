package com.planify.app.util

import android.util.Log

object DebugLogger {
    private const val TAG = "Planify"

    fun log(message: String) {
        Log.d(TAG, message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun network(message: String) {
        Log.d("$TAG-NET", message)
    }
}
