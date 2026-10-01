package com.ridesync

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

class RideSyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.w("RideSyncApp", "Firebase initialization deferred/optional: ${e.message}")
        }
    }

    companion object {
        lateinit var instance: RideSyncApplication
            private set
        val appContext: Context
            get() = instance.applicationContext
    }
}
