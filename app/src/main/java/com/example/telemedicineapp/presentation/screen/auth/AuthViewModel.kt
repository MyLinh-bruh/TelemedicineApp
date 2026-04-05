package com.example.telemedicineapp.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.data.RegisterResult
import com.example.telemedicineapp.model.DoctorStatus
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.model.User // 🌟 Cần thêm import này
import com.google.firebase.auth.FirebaseAuth // 🌟 Cần thêm import này
import com.google.firebase.firestore.FirebaseFirestore // 🌟 Cần thêm import này
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

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginSuccess = MutableStateFlow<Role?>(null)
    val loginSuccess: StateFlow<Role?> = _loginSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 🌟 1. Thông tin người dùng hiện tại (Dùng cho BookingScreen)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    init {
        // 🌟 Tự động lắng nghe trạng thái đăng nhập và lấy User Profile
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                // Lắng nghe Realtime từ Firestore
                db.collection("Users").document(uid).addSnapshotListener { snapshot, _ ->
                    _currentUser.value = snapshot?.toObject(User::class.java)?.copy(id = snapshot.id)
                }
            } else {
                _currentUser.value = null
            }
        }
    }

    // --- 2. LOGIC ĐĂNG NHẬP ---
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

    // --- 3. LOGIC ĐĂNG KÝ ---
    fun register(emailInput: String, passInput: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authRepo.register(emailInput, passInput)

            when (result) {
                RegisterResult.SUCCESS -> {
                    val fakeToken = "Bearer_${UUID.randomUUID()}"
                    tokenManager.saveSession(fakeToken, Role.PATIENT.name, "NONE")
                    _loginSuccess.value = Role.PATIENT
                }
                RegisterResult.EMAIL_EXISTS -> {
                    _errorMessage.value = "Email đã tồn tại. Vui lòng sử dụng một email khác!"
                }
                RegisterResult.ERROR -> {
                    _errorMessage.value = "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau!"
                }
            }
            _isLoading.value = false
        }
    }

    // --- 4. CÁC HÀM HỖ TRỢ ---
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
        auth.signOut() // Đăng xuất khỏi Firebase
        tokenManager.clearSession()
        _loginSuccess.value = null
        _currentUser.value = null
    }
}