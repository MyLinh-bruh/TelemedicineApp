package com.example.telemedicineapp.model

data class DoctorSchedule(
    val id: String = "",
    val doctorId: String = "",
    val date: String = "",
    val morningSlots: List<String> = emptyList(),
    val afternoonSlots: List<String> = emptyList(),
    val busySlots: List<String> = emptyList() // 👈 Những giờ bác sĩ tự đánh dấu bận
)