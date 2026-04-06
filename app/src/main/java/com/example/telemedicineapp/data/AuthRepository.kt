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


data class UserEntity(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "PATIENT",
    val imageUrl: String = "",
    val certificateUrl: String = "",
    val phone: String = "",
    val address: String = "",
    val gender: String = "",
    val specialty: String = "",
    val description: String = "",
    val hospitalName: String = "",
    val doctorStatus: String = "NONE",
    val bankAccountNumber: String = "",
    val bankName: String = "",
    val bloodType: String = "",
    val medicalHistory: String = ""
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
    // 🌟 ĐÃ SỬA: Đổi tham số cuối thành certificateImage kiểu String
    suspend fun registerDoctorRequest(
        name: String,
        email: String,
        pass: String,
        specialty: String,
        hospitalName: String,
        address: String,
        certificateImage: String
    ): RegisterResult {
        return try {
            // Kiểm tra email tồn tại
            val userSnapshot = db.collection("Users").whereEqualTo("email", email).get().await()
            if (!userSnapshot.isEmpty) return RegisterResult.EMAIL_EXISTS

            // Tạo Document ID mới trong Users
            val newUserDoc = db.collection("Users").document()

            // 🌟 ĐÃ SỬA: Gán chuỗi Base64 vào cả imageUrl để màn hình Admin đọc được
            val doctorData = hashMapOf(
                "id" to newUserDoc.id,
                "name" to name,
                "email" to email,
                "password" to pass,
                "role" to "DOCTOR",
                "doctorStatus" to "PENDING",
                "specialty" to specialty,
                "hospitalName" to hospitalName,
                "address" to address,
                "imageUrl" to certificateImage,
                "certificateUrl" to certificateImage
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

    // Thêm Context vào để đọc Uri
    suspend fun updateUserProfile(context: android.content.Context, user: User, imageUri: Uri?): Boolean {
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

            // XỬ LÝ ẢNH: Nếu có Uri mới, nén thành Base64. Nếu không, lấy từ user.imageUrl (đã xử lý ở Screen)
            val finalImage = if (imageUri != null) {
                uriToBase64(context, imageUri)
            } else {
                user.imageUrl
            }

            if (finalImage.isNotEmpty()) {
                updates["imageUrl"] = finalImage
            }

            docRef.update(updates).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Hàm nén ảnh "vô đối" để không bị quá dung lượng
    private fun uriToBase64(context: android.content.Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            val outputStream = java.io.ByteArrayOutputStream()

            // Nén xuống 25% chất lượng để chuỗi Base64 không quá 1MB
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 25, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            ""
        }
    }

    // --- 8. LẤY HỒ SƠ BỆNH ÁN CỦA BỆNH NHÂN (Đã chuyển vào trong class) ---
    fun getMedicalRecordsForPatient(patientId: String): Flow<List<com.example.telemedicineapp.model.MedicalRecord>> = callbackFlow {
        val listener = db.collection("MedicalRecords")
            .whereEqualTo("patientId", patientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.toObjects(com.example.telemedicineapp.model.MedicalRecord::class.java)
                    trySend(records)
                }
            }
        awaitClose { listener.remove() }
    }
}