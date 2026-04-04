package com.example.telemedicineapp.core

import android.content.Context
import android.content.SharedPreferences
import com.example.telemedicineapp.model.DoctorStatus
import com.example.telemedicineapp.model.Role
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("telemedicine_prefs", Context.MODE_PRIVATE)

    // Lưu toàn bộ thông tin đăng nhập
    fun saveSession(token: String, role: String, status: String) {
        prefs.edit().apply {
            putString("JWT_TOKEN", token)
            putString("USER_ROLE", role)
            putString("DOCTOR_STATUS", status)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString("JWT_TOKEN", null)

    fun getRole(): Role {
        val roleStr = prefs.getString("USER_ROLE", Role.PATIENT.name)
        return try { Role.valueOf(roleStr!!) } catch (e: Exception) { Role.PATIENT }
    }

    fun getDoctorStatus(): DoctorStatus {
        val statusStr = prefs.getString("DOCTOR_STATUS", DoctorStatus.NONE.name)
        return try { DoctorStatus.valueOf(statusStr!!) } catch (e: Exception) { DoctorStatus.NONE }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}