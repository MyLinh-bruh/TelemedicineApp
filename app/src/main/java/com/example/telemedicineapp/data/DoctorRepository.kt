package com.example.telemedicineapp.data

import com.example.telemedicineapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * 1. Lấy LUỒNG DỮ LIỆU BÁC SĨ CHO ADMIN (Dùng để Duyệt đơn)
     * Lấy tất cả người dùng có role là DOCTOR, không phân biệt trạng thái.
     */
    fun getDoctorsStream(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("Users")
            .whereEqualTo("role", "DOCTOR") // Chỉ lấy những người đăng ký làm bác sĩ
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    // Chuyển đổi dữ liệu từ Firebase Document sang List<User>
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * 2. LẤY DANH SÁCH BÁC SĨ CHO BỆNH NHÂN (Chỉ những người đã được duyệt)
     * Bộ lọc: role == "DOCTOR" AND doctorStatus == "APPROVED"
     */
    fun getApprovedDoctors(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("Users")
            .whereEqualTo("role", "DOCTOR")
            .whereEqualTo("doctorStatus", "APPROVED") // 🌟 Quan trọng: Chỉ hiện bác sĩ đã duyệt
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)
                    }
                    trySend(list)
                }
            }
        awaitClose { listener.remove() }
    }
}