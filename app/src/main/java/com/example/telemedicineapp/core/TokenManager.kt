package com.example.telemedicineapp.core

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("telemedicine_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit().putString("JWT_TOKEN", token).apply()
    }

    fun getToken(): String? {
        return prefs.getString("JWT_TOKEN", null)
    }

    fun clearToken() {
        prefs.edit().remove("JWT_TOKEN").apply()
    }
}
