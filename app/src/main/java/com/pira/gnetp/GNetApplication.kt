package com.pira.gnetp

import android.app.Application
import com.pira.gnetp.utils.PreferenceManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GNetApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Clear the selected IP address when the app starts
        val preferenceManager = PreferenceManager.getInstance(this)
        preferenceManager.clearSelectedIpAddress()
    }
}