package com.example.telemedicineapp.presentation.screen.auth

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.core.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context // Thêm context để xử lý ảnh
) : ViewModel() {

    var userState by mutableStateOf<User?>(null)
    var isLoading by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    // Biến tạm để hiển thị ảnh ngay khi vừa chọn từ thư viện
    var tempImageUri by mutableStateOf<Uri?>(null)

    fun loadProfile() {
        val email = tokenManager.getEmail() ?: return
        viewModelScope.launch {
            isLoading = true
            try {
                userState = repository.getUserProfile(email)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }


    fun saveProfile(user: User, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            // TRUYỀN CONTEXT VÀO ĐÂY
            val success = repository.updateUserProfile(context, user, imageUri)
            if (success) {
                userState = repository.getUserProfile(user.email)
                onSuccess()
            }
            isSaving = false
        }
    }
}