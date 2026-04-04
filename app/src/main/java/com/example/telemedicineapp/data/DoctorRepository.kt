package com.example.telemedicineapp.data

import android.util.Log
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.model.User // Nhập model User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    // Lấy dữ liệu Realtime (Tải ảnh thật)
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
                        // Firebase sẽ tự động map và load imageUrl
                        doc.toObject(User::class.java)?.copy(id = doc.id)
                    }
                    trySend(doctors).isSuccess
                }
            }
        awaitClose { listener.remove() }
    }
}