package com.example.telemedicineapp.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookingRepository {
    // Kết nối thẳng vào Máy chủ Firestore của Google
    private val db = FirebaseFirestore.getInstance()

    // Hàm đặt lịch khám - Sử dụng Transaction để khóa dữ liệu, chống Double-booking
    suspend fun bookAppointment(slotId: String, patientId: String): Boolean {
        // Trỏ tới khung giờ (slot) bệnh nhân muốn đặt
        val slotRef = db.collection("TimeSlots").document(slotId)
        // Tạo sẵn một chỗ trống để lưu Lịch hẹn mới
        val appointmentRef = db.collection("Appointments").document()

        return try {
            // BẮT ĐẦU GIAO DỊCH (TRANSACTION): Firebase sẽ khóa slot này lại
            db.runTransaction { transaction ->
                // 1. Kéo trạng thái mới nhất của slot này từ máy chủ về
                val snapshot = transaction.get(slotRef)

                // Nếu không tìm thấy hoặc isBooked = true (đã bị đặt)
                val isBooked = snapshot.getBoolean("isBooked") ?: true

                // 2. Kiểm tra xem có ai nhanh tay đặt trước chưa
                if (isBooked) {
                    // Nếu đã bị đặt, đánh sập giao dịch ngay lập tức
                    throw Exception("Rất tiếc! Khung giờ này vừa có người đặt.")
                } else {
                    // 3. Nếu còn trống, lập tức chuyển isBooked thành true (khóa lại)
                    transaction.update(slotRef, "isBooked", true)

                    // 4. Đồng thời tạo biên lai Lịch hẹn mới cho bệnh nhân
                    val newAppointment = hashMapOf(
                        "patientId" to patientId,
                        "slotId" to slotId,
                        "status" to "SCHEDULED"
                    )
                    transaction.set(appointmentRef, newAppointment)
                }
            }.await() // Đợi máy chủ Google xử lý xong

            true // Đặt lịch thành công!
        } catch (e: Exception) {
            println("❌ Lỗi đặt lịch: ${e.message}")
            false // Đặt lịch thất bại (do trùng lịch hoặc rớt mạng)
        }
    }
}