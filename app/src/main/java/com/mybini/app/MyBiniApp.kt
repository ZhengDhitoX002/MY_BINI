package com.mybini.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyBiniApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Tempat inisialisasi awal seperti WorkManager, Firebase, dll.
    }
}
