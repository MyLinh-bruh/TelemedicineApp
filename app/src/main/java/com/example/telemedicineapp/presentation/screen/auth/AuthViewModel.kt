package com.example.telemedicineapp.presentation.screens.auth

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.data.RegisterResult
import com.example.telemedicineapp.model.DoctorStatus
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- CÁC TRẠNG THÁI FLOW ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginSuccess = MutableStateFlow<Role?>(null)
    val loginSuccess: StateFlow<Role?> = _loginSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isWaitingApproval = MutableStateFlow(false)
    val isWaitingApproval: StateFlow<Boolean> = _isWaitingApproval

    private val _isApproved = MutableStateFlow(false)
    val isApproved: StateFlow<Boolean> = _isApproved

    private val _isRejected = MutableStateFlow(false)
    val isRejected: StateFlow<Boolean> = _isRejected

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        observeCurrentUser()

        val pendingEmail = tokenManager.getPendingEmail()
        if (!pendingEmail.isNullOrEmpty()) {
            _isWaitingApproval.value = true
            startListeningStatus(pendingEmail)
        }
    }

    // 🌟 ĐÃ SỬA: Hàm này giờ nằm gọn gàng BÊN TRONG class và convert dữ liệu an toàn
    private fun observeCurrentUser() {
        val savedEmail = tokenManager.getEmail()

        if (!savedEmail.isNullOrEmpty()) {
            db.collection("Users").whereEqualTo("email", savedEmail)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AUTH_ERROR", "Firestore Error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        try {
                            val doc = snapshot.documents[0]
                            // Dùng UserEntity để hứng dữ liệu String từ Firebase
                            val userEntity = doc.toObject(com.example.telemedicineapp.data.UserEntity::class.java)

                            if (userEntity != null) {
                                // Ép kiểu an toàn từ String sang Enum
                                val role = try { Role.valueOf(userEntity.role.uppercase()) } catch (e: Exception) { Role.PATIENT }
                                val status = try { DoctorStatus.valueOf(userEntity.doctorStatus.uppercase()) } catch (e: Exception) { DoctorStatus.NONE }

                                // Cập nhật lại currentUser
                                _currentUser.value = User(
                                    id = doc.id, // Lấy ID chuẩn từ Document
                                    email = userEntity.email,
                                    name = userEntity.name,
                                    role = role,
                                    doctorStatus = status,
                                    specialty = userEntity.specialty,
                                    hospitalName = userEntity.hospitalName,
                                    imageUrl = userEntity.imageUrl,
                                    certificateUrl = userEntity.certificateUrl
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("AUTH_ERROR", "Lỗi convert dữ liệu Firebase: ${e.message}")
                        }
                    } else {
                        Log.d("AUTH_DEBUG", "Không tìm thấy user với Email: $savedEmail")
                        // KHÔNG gán _currentUser.value = null ở đây nữa để tránh mất dữ liệu do cache mạng
                    }
                }
        } else {
            _currentUser.value = null
        }
    }

    private fun startListeningStatus(email: String) {
        authRepo.listenToDoctorStatus(email).onEach { status ->
            when (status) {
                "APPROVED" -> {
                    _isWaitingApproval.value = false
                    _isApproved.value = true
                    tokenManager.clearPendingEmail()
                }
                "DELETED" -> {
                    _isWaitingApproval.value = false
                    _isRejected.value = true
                    tokenManager.clearPendingEmail()
                }
            }
        }.launchIn(viewModelScope)
    }

    // 🌟 ĐÃ SỬA: Thay Uri? thành String để nhận chuỗi Base64
    fun registerDoctorRequest(
        name: String, email: String, pass: String,
        specialty: String, hospitalName: String, certificateImage: String
    ) {
        viewModelScope.launch {
            if (certificateImage.isBlank()) {
                _errorMessage.value = "Vui lòng chọn ảnh chứng chỉ!"
                return@launch
            }
            _isLoading.value = true
            // Cập nhật tham số gọi authRepo
            val result = authRepo.registerDoctorRequest(name, email, pass, specialty, hospitalName, certificateImage)

            if (result == RegisterResult.SUCCESS) {
                tokenManager.savePendingEmail(email)
                _isWaitingApproval.value = true
                startListeningStatus(email)
            } else {
                _errorMessage.value = "Email đã tồn tại hoặc lỗi hệ thống!"
            }
            _isLoading.value = false
        }
    }

    fun cancelRegistration() {
        val emailToDelete = tokenManager.getPendingEmail() ?: ""
        if (emailToDelete.isEmpty()) {
            _isWaitingApproval.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            if (authRepo.deleteDoctorRequest(emailToDelete)) {
                tokenManager.clearPendingEmail()
                _isWaitingApproval.value = false
                _errorMessage.value = "Đã hủy yêu cầu thành công."
            } else {
                _errorMessage.value = "Lỗi khi xóa dữ liệu trên Server!"
            }
            _isLoading.value = false
        }
    }

    fun login(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val userEntity = authRepo.login(emailInput, passInput)

                if (userEntity != null) {
                    val role = try {
                        Role.valueOf(userEntity.role.uppercase())
                    } catch (e: Exception) { Role.PATIENT }

                    val status = try {
                        DoctorStatus.valueOf(userEntity.doctorStatus.uppercase())
                    } catch (e: Exception) { DoctorStatus.NONE }

                    if (role == Role.DOCTOR && status == DoctorStatus.PENDING) {
                        _errorMessage.value = "Tài khoản đang chờ duyệt!"
                    } else {
                        val userModel = User(
                            id = userEntity.id,
                            email = userEntity.email,
                            name = userEntity.name,
                            role = role,
                            doctorStatus = status,
                            specialty = userEntity.specialty,
                            hospitalName = userEntity.hospitalName,
                            imageUrl = userEntity.imageUrl,
                            certificateUrl = userEntity.certificateUrl
                        )

                        _currentUser.value = userModel

                        tokenManager.saveEmail(emailInput)
                        tokenManager.saveSession("Token_${UUID.randomUUID()}", role.name, status.name)

                        // Đồng bộ Realtime
                        observeCurrentUser()

                        _loginSuccess.value = role
                    }
                } else {
                    _errorMessage.value = "Sai tài khoản hoặc mật khẩu!"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi hệ thống: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = authRepo.register(emailInput, passInput)

            when (result) {
                RegisterResult.SUCCESS -> _errorMessage.value = "Đăng ký thành công! Mời bạn đăng nhập."
                RegisterResult.EMAIL_EXISTS -> _errorMessage.value = "Email đã tồn tại!"
                RegisterResult.ERROR -> _errorMessage.value = "Lỗi hệ thống!"
            }
            _isLoading.value = false
        }
    }

    // 🌟 MỚI: HÀM XÓA HÀNG LOẠT BÁC SĨ TỪ FIRESTORE
    fun deleteSelectedDoctors(doctorIds: List<String>) {
        if (doctorIds.isEmpty()) {
            _errorMessage.value = "Vui lòng chọn ít nhất 1 bác sĩ để xóa!"
            return
        }

        _isLoading.value = true

        // Sử dụng Firebase Batch Write để xóa cùng lúc nhiều dòng
        val batch = db.batch()

        doctorIds.forEach { id ->
            val doctorRef = db.collection("Users").document(id)
            batch.delete(doctorRef)
        }

        // Thực thi việc xóa
        batch.commit()
            .addOnSuccessListener {
                _isLoading.value = false
                _errorMessage.value = "Đã xóa thành công ${doctorIds.size} bác sĩ!"
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _errorMessage.value = "Lỗi khi xóa: ${exception.message}"
            }
    }

    fun logout() {
        auth.signOut()
        tokenManager.clearSession()
        _loginSuccess.value = null
        _currentUser.value = null
    }

    fun showError(message: String) { _errorMessage.value = message }
    fun clearError() { _errorMessage.value = null }
    fun resetApprovalState() {
        _isWaitingApproval.value = false
        _isApproved.value = false
        _isRejected.value = false
    }
    fun resetLoginStatus() { _loginSuccess.value = null }
}