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
                        val doc = snapshot.documents[0]
                        val user = doc.toObject(User::class.java)
                        _currentUser.value = user?.copy(id = doc.id)
                    } else {
                        Log.d("AUTH_DEBUG", "Không tìm thấy user với Email: $savedEmail")
                        _currentUser.value = null
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

    fun registerDoctorRequest(
        name: String, email: String, pass: String,
        specialty: String, hospitalName: String, certificateUri: Uri?
    ) {
        viewModelScope.launch {
            if (certificateUri == null) {
                _errorMessage.value = "Vui lòng chọn ảnh chứng chỉ!"
                return@launch
            }
            _isLoading.value = true
            val result = authRepo.registerDoctorRequest(name, email, pass, specialty, hospitalName, certificateUri)

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
                            certificateUrl = userEntity.certificateUrl // 🌟 ĐÃ THÊM trường chứng chỉ
                        )

                        _currentUser.value = userModel

                        tokenManager.saveEmail(emailInput)
                        tokenManager.saveSession("Token_${UUID.randomUUID()}", role.name, status.name)

                        // 🌟 KÍCH HOẠT ĐỒNG BỘ REALTIME NGAY LẬP TỨC
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