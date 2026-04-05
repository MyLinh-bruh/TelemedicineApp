package com.example.telemedicineapp.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Cập nhật Entity để khớp với Key "e-mail" trên Firebase
data class UserEntity(
    val id: String = "",
    val name: String = "",
    var email: String = "",
    val role: String = "PATIENT",
    val doctorStatus: String = "NONE",
    val specialty: String = "",
    val hospitalName: String = "",
    val imageUrl: String = ""
)

enum class RegisterResult { SUCCESS, EMAIL_EXISTS, ERROR }

@Singleton
class AuthRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    // --- 1. ĐĂNG NHẬP (Dùng e-mail) ---
    suspend fun login(emailInput: String, passInput: String): UserEntity? {
        return try {
            val snapshot = db.collection("Users")
                .whereEqualTo("email", emailInput)
                .whereEqualTo("password", passInput)
                .get().await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(UserEntity::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // --- 2. ĐĂNG KÝ BỆNH NHÂN (Dùng e-mail) ---
    suspend fun register(email: String, pass: String): RegisterResult {
        return try {
            val snapshot = db.collection("Users").whereEqualTo("email", email).get().await()
            if (!snapshot.isEmpty) return RegisterResult.EMAIL_EXISTS

            val newUser = hashMapOf(
                "email" to email,
                "password" to pass,
                "role" to "PATIENT",
                "doctorStatus" to "NONE"
            )
            db.collection("Users").add(newUser).await()
            RegisterResult.SUCCESS
        } catch (e: Exception) {
            RegisterResult.ERROR
        }
    }

    // --- 3. ĐĂNG KÝ BÁC SĨ (CHỈ LƯU VÀO COLLECTION USERS) ---
    suspend fun registerDoctorRequest(
        name: String,
        email: String,
        pass: String,
        specialty: String,
        hospitalName: String,
        certificateUri: Uri
    ): RegisterResult {
        return try {
            // Kiểm tra email tồn tại bằng key "e-mail"
            val userSnapshot = db.collection("Users").whereEqualTo("email", email).get().await()
            if (!userSnapshot.isEmpty) return RegisterResult.EMAIL_EXISTS

            // Tạo Document ID mới trong Users
            val newUserDoc = db.collection("Users").document()

            // Gom tất cả thông tin bác sĩ vào 1 nơi duy nhất
            val doctorData = hashMapOf(
                "id" to newUserDoc.id,
                "name" to name,
                "email" to email, // Key đồng bộ với DB của bạn
                "password" to pass,
                "role" to "DOCTOR",
                "doctorStatus" to "PENDING",
                "specialty" to specialty,
                "hospitalName" to hospitalName,
                "imageUrl" to certificateUri.toString()
            )

            // Lưu dữ liệu
            newUserDoc.set(doctorData).await()

            RegisterResult.SUCCESS
        } catch (e: Exception) {
            RegisterResult.ERROR
        }
    }

    // --- 4. HÀM XÓA DỮ LIỆU (CHỈ XÓA TRONG USERS) ---
    suspend fun deleteDoctorRequest(email: String): Boolean {
        return try {
            // Tìm tất cả các document có e-mail này trong collection Users
            val users = db.collection("Users")
                .whereEqualTo("email", email)
                .get().await()

            if (users.isEmpty) return false

            for (doc in users.documents) {
                doc.reference.delete().await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- 5. LẮNG NGHE TRẠNG THÁI REALTIME ---
    // Mở file data/AuthRepository.kt, tìm hàm listenToDoctorStatus và sửa lại như sau:
    // Trong data/AuthRepository.kt
    fun listenToDoctorStatus(email: String): Flow<String> = callbackFlow {
        val listener = db.collection("Users").whereEqualTo("email", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (!snapshot.isEmpty) {
                        // Nếu vẫn còn dữ liệu (đang chờ hoặc đã duyệt)
                        val status = snapshot.documents[0].getString("doctorStatus") ?: "PENDING"
                        trySend(status)
                    } else {
                        // 🌟 QUAN TRỌNG: Nếu snapshot rỗng nghĩa là Admin đã XÓA ĐƠN
                        trySend("DELETED")
                    }
                }
            }
        awaitClose { listener.remove() }
    }
}