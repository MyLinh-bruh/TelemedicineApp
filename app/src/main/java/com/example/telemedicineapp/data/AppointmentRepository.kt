package com.example.telemedicineapp.data

import android.util.Log
import com.example.telemedicineapp.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    // Hàm tạo lịch hẹn cơ bản (Không kiểm tra)
    suspend fun createAppointment(appointment: Appointment): Boolean {
        return try {
            val docRef = db.collection("Appointments").document()
            val newAppointment = appointment.copy(id = docRef.id)

            docRef.set(newAppointment).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Hàm tạo lịch hẹn CÓ KIỂM TRA CHỐNG TRÙNG LỊCH (Chuẩn Mobile)
    suspend fun createAppointmentWithCheck(appointment: Appointment): Result<String> {
        return try {
            // BƯỚC 1: Thực hiện lệnh Read (Query) để quét toàn bộ lịch của Bác sĩ này tại thời điểm đó
            val checkSnapshot = db.collection("Appointments")
                .whereEqualTo("doctorId", appointment.doctorId)
                .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                .whereIn("status", listOf("PENDING", "PAID")) // Chỉ quét những trạng thái đang "giữ chỗ"
                .get()
                .await()

            // BƯỚC 2: Kiểm tra kết quả
            if (!checkSnapshot.isEmpty) {
                // Nếu danh sách không rỗng -> Có người đã đặt -> Trả về lỗi
                Log.w("AppointmentRepo", "Phát hiện trùng lịch!")
                return Result.failure(Exception("Rất tiếc, khung giờ này vừa có người đặt. Vui lòng chọn khung giờ khác."))
            }

            // BƯỚC 3: Nếu an toàn (rỗng), tiến hành khởi tạo Document và lưu dữ liệu
            val docRef = db.collection("Appointments").document()

            // Gắn cái ID thực tế của Firebase vào Object trước khi lưu
            val newAppointment = appointment.copy(id = docRef.id)

            docRef.set(newAppointment).await()

            Log.d("AppointmentRepo", "Tạo lịch hẹn thành công: ${docRef.id}")

            // Trả về ID của lịch hẹn vừa tạo để ViewModel mang đi xử lý tiếp (thanh toán, v.v.)
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e("AppointmentRepo", "Lỗi tạo lịch hẹn: ${e.message}")
            Result.failure(e)
        }
    }
}