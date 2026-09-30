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
        } catch (e: Throwable) {
            Log.w("RideSyncApp", "Firebase initialization deferred/optional: ${e.message}")
        }
    }

    companion object {
        @Volatile
        var instance: RideSyncApplication? = null
            private set

        val appContext: Context?
            get() = instance?.applicationContext
    }
}
