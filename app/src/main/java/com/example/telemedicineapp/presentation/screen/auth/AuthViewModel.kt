package com.example.telemedicineapp.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.data.RegisterResult
import com.example.telemedicineapp.model.DoctorStatus
import com.example.telemedicineapp.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginSuccess = MutableStateFlow<Role?>(null)
    val loginSuccess: StateFlow<Role?> = _loginSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // --- 1. LOGIC ĐĂNG NHẬP ---
    fun login(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val user = authRepo.login(emailInput, passInput)

            if (user != null) {
                val role = try {
                    Role.valueOf(user.role.uppercase())
                } catch (e: Exception) {
                    Role.PATIENT
                }

                val status = try {
                    DoctorStatus.valueOf(user.doctorStatus.uppercase())
                } catch (e: Exception) {
                    DoctorStatus.NONE
                }

                val fakeToken = "Bearer_${UUID.randomUUID()}"
                tokenManager.saveSession(fakeToken, role.name, status.name)

                _loginSuccess.value = role
            } else {
                _errorMessage.value = "Tài khoản hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại!"
            }

            _isLoading.value = false
        }
    }

    // --- 2. LOGIC ĐĂNG KÝ ---
    fun register(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Nhận kết quả RegisterResult từ Repository
            val result = authRepo.register(emailInput, passInput)

            when (result) {
                RegisterResult.SUCCESS -> {
                    // Đăng ký thành công
                    val fakeToken = "Bearer_${UUID.randomUUID()}"
                    tokenManager.saveSession(fakeToken, Role.PATIENT.name, "NONE")
                    _loginSuccess.value = Role.PATIENT
                }
                RegisterResult.EMAIL_EXISTS -> {
                    // Đẩy thông báo ra UI bằng biến errorMessage có sẵn
                    _errorMessage.value = "Email đã tồn tại. Vui lòng sử dụng một email khác!"
                }
                RegisterResult.ERROR -> {
                    // Lỗi kết nối hoặc lỗi từ Firebase
                    _errorMessage.value = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau!"
                }
            }

            _isLoading.value = false
        }
    }

    // --- 3. CÁC HÀM HỖ TRỢ ---
    fun showError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun resetLoginStatus() {
        _loginSuccess.value = null
    }

    fun logout() {
        tokenManager.clearSession()
        _loginSuccess.value = null
    }
}