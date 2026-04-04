package com.example.telemedicineapp.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Model hứng dữ liệu
data class UserEntity(
    val email: String = "",
    val role: String = "PATIENT",
    val doctorStatus: String = "NONE"
)

// Tạo Enum để định nghĩa rõ các trạng thái khi đăng ký
enum class RegisterResult {
    SUCCESS,
    EMAIL_EXISTS,
    ERROR
}

@Singleton
class AuthRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    // 1. Hàm Đăng nhập
    suspend fun login(emailInput: String, passInput: String): UserEntity? {
        return try {
            val snapshot = db.collection("Users")
                .whereEqualTo("email", emailInput)
                .whereEqualTo("password", passInput)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(UserEntity::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // 2. Hàm Đăng ký có kiểm tra trùng lặp email
    suspend fun register(email: String, pass: String): RegisterResult {
        return try {
            // Bước 1: Query database xem email này đã có ai dùng chưa
            val snapshot = db.collection("Users")
                .whereEqualTo("email", email)
                .get()
                .await()

            // Nếu danh sách trả về không rỗng => Email đã tồn tại
            if (!snapshot.isEmpty) {
                return RegisterResult.EMAIL_EXISTS
            }

            // Bước 2: Nếu chưa có, tiến hành tạo mới user
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
}