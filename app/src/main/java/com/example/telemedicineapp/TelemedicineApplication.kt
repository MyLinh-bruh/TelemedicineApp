package com.example.telemedicineapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TelemedicineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sau này có thể khởi tạo Timber logging hoặc notification channel ở đây
    }
}
