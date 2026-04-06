package com.example.telemedicineapp.model

data class MedicalRecord(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val patientName: String = "",

    // 1. Chỉ số cơ thể & Máu
    val dateOfBirth: String = "",
    val gender: String = "Khác", // Nam / Nữ / Khác
    val bloodType: String = "", // A, B, AB, O
    val height: String = "", // cm
    val weight: String = "", // kg

    // 2. Chỉ số sinh tồn (Vital Signs)
    val bloodPressure: String = "", // mmHg (VD: 120/80)
    val heartRate: String = "", // bpm (Nhịp/phút)
    val temperature: String = "", // °C

    // 3. Tiền sử bệnh
    val allergies: String = "", // Dị ứng thuốc, thức ăn...
    val chronicDiseases: String = "", // Bệnh mãn tính (Tiểu đường, Huyết áp...)
    val pastSurgeries: String = "", // Tiền sử phẫu thuật
    val familyMedicalHistory: String = "", // Di truyền gia đình

    // 4. Lần khám hiện tại
    val currentSymptoms: String = "", // Triệu chứng lâm sàng
    val diagnosis: String = "", // Chẩn đoán của bác sĩ
    val prescription: String = "", // Kê đơn thuốc / Hướng dẫn điều trị

    val lastUpdated: String = "", // Thời gian cập nhật cuối
    val appointmentId: String = ""
)