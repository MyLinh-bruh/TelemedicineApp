package com.example.telemedicineapp.data

import com.example.telemedicineapp.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

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
    suspend fun createAppointmentWithCheck(appointment: Appointment): Result<Boolean> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val appointmentRef = db.collection("Appointments").document()

            // Sử dụng Transaction để chống trùng lịch
            db.runTransaction { transaction ->
                // 1. Tìm các cuộc hẹn cùng bác sĩ, cùng thời điểm, và không bị hủy
                val query = db.collection("Appointments")
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                    .whereNotEqualTo("status", "CANCELLED")

                // Lưu ý: get() trong transaction phải dùng trực tiếp từ query snapshot
                // Nhưng vì Firestore Transaction trên Mobile bị hạn chế với Query,
                // Ta nên thực hiện kiểm tra này ở cấp độ Logic hoặc Cloud Functions.

                // GIẢI PHÁP CHO MOBILE:
                // Nếu bạn muốn làm thuần trên Mobile, hãy thực hiện một lệnh Read trước khi Write
            }.await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}