package com.example.telemedicineapp.model

// Định nghĩa 3 loại người dùng
enum class Role {
    PATIENT, DOCTOR, ADMIN
}

// Định nghĩa trạng thái của bác sĩ
enum class DoctorStatus {
    NONE,       // Người thường (Bệnh nhân/Admin)
    PENDING,    // Bác sĩ đang chờ Admin duyệt
    APPROVED,   // Bác sĩ đã được duyệt, cho phép khám
    REJECTED    // Bác sĩ bị từ chối
}