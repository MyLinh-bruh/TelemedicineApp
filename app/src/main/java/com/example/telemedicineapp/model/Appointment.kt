package com.example.telemedicineapp.model

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val dateTimeUtc: String = "", // 👈 Lưu giờ UTC chuẩn ISO-8601
    val reason: String = "",
    val status: String = "PENDING"
)