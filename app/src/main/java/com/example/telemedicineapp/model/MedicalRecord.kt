package com.example.telemedicineapp.model

data class MedicalRecord(
    val id: String = "",
    val appointmentId: String = "", // 🌟 THÊM MỚI: Liên kết với Lịch hẹn
    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String = "",

    val age: String = "",
    val phone: String = "",
    val identityCard: String = "",
    val healthInsurance: String = "",

    val dateOfBirth: String = "",
    val gender: String = "Khác",
    val bloodType: String = "",
    val height: String = "",
    val weight: String = "",

    val bloodPressure: String = "",
    val heartRate: String = "",
    val temperature: String = "",

    val allergies: String = "",
    val chronicDiseases: String = "",
    val pastSurgeries: String = "",
    val familyMedicalHistory: String = "",

    val currentSymptoms: String = "",
    val diagnosis: String = "",
    val prescription: String = "",

    val lastUpdated: String = ""
)