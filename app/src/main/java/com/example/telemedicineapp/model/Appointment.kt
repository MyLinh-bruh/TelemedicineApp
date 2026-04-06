package com.example.telemedicineapp.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val doctorImageUrl: String = "", // 🌟 THÊM TRƯỜNG NÀY ĐỂ LƯU ẢNH BÁC SĨ
    val dateTimeUtc: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)