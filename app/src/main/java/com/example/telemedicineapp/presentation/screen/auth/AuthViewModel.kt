package com.example.telemedicineapp.presentation.screens.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.data.RegisterResult
import com.example.telemedicineapp.model.Role
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

    private val _isRejected = MutableStateFlow(false)
    val isRejected: StateFlow<Boolean> = _isRejected

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

    init {
        // KHÔI PHỤC TRẠNG THÁI: Kiểm tra xem bộ nhớ máy có email nào đang treo không
        val pendingEmail = tokenManager.getPendingEmail()
        if (!pendingEmail.isNullOrEmpty()) {
            _isWaitingApproval.value = true
            startListeningStatus(pendingEmail)
        }
    }

    // Hàm lắng nghe trạng thái từ Firebase
    // Hàm lắng nghe trạng thái từ Firebase
    // Trong presentation/screens/auth/AuthViewModel.kt
    private fun startListeningStatus(email: String) {
        authRepo.listenToDoctorStatus(email).onEach { status ->
            when (status) {
                "APPROVED" -> {
                    _isWaitingApproval.value = false
                    _isApproved.value = true
                    tokenManager.clearPendingEmail()
                }
                "DELETED" -> { // 🌟 Admin đã xóa đơn trên DB
                    _isWaitingApproval.value = false
                    _isRejected.value = true // Kích hoạt Popup thông báo bị từ chối
                    tokenManager.clearPendingEmail() // Xóa email treo trong máy bác sĩ
                }
            }
        }.launchIn(viewModelScope)
    }

    // ĐĂNG KÝ BÁC SĨ
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
                // 1. Lưu email vào bộ nhớ máy ngay lập tức
                tokenManager.savePendingEmail(email)
                // 2. Hiện Popup chờ
                _isWaitingApproval.value = true
                // 3. Bắt đầu lắng nghe
                startListeningStatus(email)
            } else {
                _errorMessage.value = "Email đã tồn tại hoặc lỗi hệ thống!"
            }
            _isLoading.value = false
        }
    }

    // HÀM HỦY: Xóa sạch dữ liệu Firebase và bộ nhớ máy
    fun cancelRegistration() {
        val emailToDelete = tokenManager.getPendingEmail() ?: ""
        if (emailToDelete.isEmpty()) {
            _isWaitingApproval.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            // Gọi Repo để xóa trên Firebase
            val isDeleted = authRepo.deleteDoctorRequest(emailToDelete)
            if (isDeleted) {
                tokenManager.clearPendingEmail() // Xóa ở máy
                _isWaitingApproval.value = false // Tắt Popup
                _errorMessage.value = "Đã hủy yêu cầu và xóa sạch dữ liệu."
            } else {
                _errorMessage.value = "Lỗi khi xóa dữ liệu trên Server!"
            }
            _isLoading.value = false
        }
    }

    // ĐĂNG NHẬP
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepo.login(email, pass)
            if (user != null) {
                if (user.role == "DOCTOR" && user.doctorStatus == "PENDING") {
                    _errorMessage.value = "Tài khoản đang chờ duyệt!"
                } else {
                    tokenManager.saveSession("Token_${UUID.randomUUID()}", user.role, user.doctorStatus)
                    _loginSuccess.value = Role.valueOf(user.role.uppercase())
                }
            } else _errorMessage.value = "Sai tài khoản hoặc mật khẩu!"
            _isLoading.value = false
        }
    }

    // ĐĂNG KÝ BỆNH NHÂN
    fun register(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepo.register(emailInput, passInput)
            if (result == RegisterResult.SUCCESS) {
                _errorMessage.value = "Đăng ký thành công! Mời bạn đăng nhập."
            } else _errorMessage.value = "Email đã tồn tại!"
            _isLoading.value = false
        }
    }

    // CÁC HÀM HỖ TRỢ UI
    fun showError(message: String) { _errorMessage.value = message }
    fun clearError() { _errorMessage.value = null }
    fun resetApprovalState() { _isWaitingApproval.value = false; _isApproved.value = false ; _isRejected.value = false}
    fun resetLoginStatus() { _loginSuccess.value = null }
}