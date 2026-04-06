package com.example.telemedicineapp.data

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.example.telemedicineapp.model.User

// 🌟 ĐÃ CẬP NHẬT: Thêm certificateUrl để khớp với Model
data class UserEntity(
    val id: String = "",
    val name: String = "",
    var email: String = "",
    val role: String = "PATIENT",
    val doctorStatus: String = "NONE",
    val specialty: String = "",
    val hospitalName: String = "",
    val imageUrl: String = "",
    val certificateUrl: String = ""
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
                val doc = snapshot.documents[0]
                // 🌟 FIX LỖI LAG: Ép lấy Document ID thật trên Firebase gán vào data class
                doc.toObject(UserEntity::class.java)?.copy(id = doc.id)
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

    // --- 3. ĐĂNG KÝ BÁC SĨ ---
    suspend fun registerDoctorRequest(
        name: String,
        email: String,
        pass: String,
        specialty: String,
        hospitalName: String,
        certificateUri: Uri
    ): RegisterResult {
        return try {
            // Kiểm tra email tồn tại
            val userSnapshot = db.collection("Users").whereEqualTo("email", email).get().await()
            if (!userSnapshot.isEmpty) return RegisterResult.EMAIL_EXISTS

            // Tạo Document ID mới trong Users
            val newUserDoc = db.collection("Users").document()

            // 🌟 ĐÃ SỬA: Gom thông tin và TÁCH BIỆT 2 LOẠI ẢNH NGAY TẠI ĐÂY
            val doctorData = hashMapOf(
                "id" to newUserDoc.id,
                "name" to name,
                "email" to email,
                "password" to pass,
                "role" to "DOCTOR",
                "doctorStatus" to "PENDING",
                "specialty" to specialty,
                "hospitalName" to hospitalName,
                "imageUrl" to "", // Ảnh đại diện (Avatar) lúc mới đăng ký để trống
                "certificateUrl" to certificateUri.toString() // Lưu chứng chỉ hành nghề vào đây
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
    fun listenToDoctorStatus(email: String): Flow<String> = callbackFlow {
        val listener = db.collection("Users").whereEqualTo("email", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (!snapshot.isEmpty) {
                        val status = snapshot.documents[0].getString("doctorStatus") ?: "PENDING"
                        trySend(status)
                    } else {
                        // QUAN TRỌNG: Nếu snapshot rỗng nghĩa là Admin đã XÓA ĐƠN
                        trySend("DELETED")
                    }
                }
            }
        awaitClose { listener.remove() }
    }

    // --- 6. Lấy thông tin chi tiết User từ Firestore bằng Email ---
    suspend fun getUserProfile(email: String): User? {
        return try {
            val snapshot = db.collection("Users")
                .whereEqualTo("email", email)
                .get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(User::class.java)
            } else null
        } catch (e: Exception) { null }
    }

    // --- 7. Cập nhật hồ sơ (Update các field cụ thể) ---
    suspend fun updateUserProfile(user: User, imageUri: Uri?): Boolean {
        return try {
            val snapshot = db.collection("Users")
                .whereEqualTo("email", user.email)
                .get().await()

            if (snapshot.isEmpty) return false
            val docRef = snapshot.documents[0].reference

            val updates = mutableMapOf<String, Any>(
                "name" to user.name,
                "phone" to user.phone,
                "address" to user.address,
                "gender" to user.gender,
                "bloodType" to user.bloodType,
                "medicalHistory" to user.medicalHistory
            )

            // Nếu có ảnh mới, lưu tạm URI
            imageUri?.let { updates["imageUrl"] = it.toString() }

            docRef.update(updates).await()
            true
        } catch (e: Exception) { false }
    }
}