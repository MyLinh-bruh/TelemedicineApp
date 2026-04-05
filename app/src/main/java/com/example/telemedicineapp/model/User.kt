package com.example.telemedicineapp.model

import com.google.firebase.firestore.PropertyName

enum class Role {
    PATIENT, DOCTOR, ADMIN
}

enum class DoctorStatus {
    NONE, PENDING, APPROVED, REJECTED
}

data class User(
    // --- THÔNG TIN CƠ BẢN ---
    val id: String = "",
    val email: String = "",
    @get:PropertyName("e-mail")
    val name: String = "",
    val role: Role = Role.PATIENT,

    // TRƯỜNG LƯU URI ẢNH TẢI LÊN
    val imageUrl: String = "",

    // --- THÔNG TIN LIÊN HỆ ---
    val phone: String = "",
    val address: String = "",
    val gender: String = "",

    // --- DÀNH RIÊNG CHO BÁC SĨ ---
    val specialty: String = "",
    val description: String = "",
    val hospitalName: String = "",
    val doctorStatus: String = "NONE",
    val bankAccountNumber: String = "",
    val bankName: String = "",

    // --- DÀNH RIÊNG CHO BỆNH NHÂN ---
    val bloodType: String = "",
    val medicalHistory: String = ""
)