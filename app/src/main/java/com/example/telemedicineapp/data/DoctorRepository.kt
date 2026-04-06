package com.example.telemedicineapp.data
import kotlinx.coroutines.tasks.await

import android.util.Log
import com.example.telemedicineapp.model.Role
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

    // 1. Luồng Lấy TẤT CẢ bác sĩ (Dùng cho Admin duyệt)
    fun getDoctorsStream(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("Users")
            .whereEqualTo("role", Role.DOCTOR.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DoctorRepository", "Lỗi real-time: ", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val doctors = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    trySend(doctors).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    // 2. Luồng CHỈ LẤY BÁC SĨ ĐÃ DUYỆT (Dùng cho màn hình Bệnh Nhân)
    fun getApprovedDoctorsStream(): Flow<List<User>> = callbackFlow {
        val listener = db.collection("Users")
            .whereEqualTo("role", Role.DOCTOR.name)
            .whereEqualTo("doctorStatus", "APPROVED") // Chỉ lấy người đã duyệt
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val doctors = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    trySend(doctors).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun rejectAndRemoveDoctor(doctorId: String): Boolean {
        return try {
            // 🌟 XÓA VĨNH VIỄN tài liệu bác sĩ khỏi collection Users
            db.collection("Users").document(doctorId).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}