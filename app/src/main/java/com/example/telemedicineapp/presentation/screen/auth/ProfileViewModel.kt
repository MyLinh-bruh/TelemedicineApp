// Vị trí: presentation/screen/auth/ProfileViewModel.kt
package com.example.telemedicineapp.presentation.screen.auth

import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.core.TokenManager // Giả sử bạn lưu email ở đây
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    var userState by mutableStateOf<User?>(null)
    var isLoading by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    // Trong ProfileViewModel.kt
    var tempImageUri by mutableStateOf<Uri?>(null)

    fun onImageSelected(uri: Uri) {
        tempImageUri = uri // Hiện ảnh lên UI ngay khi vừa chọn xong
    }

    fun saveProfile(user: User, imageUri: Uri?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            // Upload và lưu vào DB...
            val success = repository.updateUserProfile(user, tempImageUri)
            if (success) {
                userState = repository.getUserProfile(user.email)
                tempImageUri = null // Reset sau khi đã lưu xong vào DB
                onSuccess()
            }
            isSaving = false
        }
    }
    // Kiểm tra trong ProfileViewModel.kt xem có giống thế này không:
    fun loadProfile() {
        val email = tokenManager.getEmail() ?: return
        viewModelScope.launch {
            isLoading = true
            userState = repository.getUserProfile(email)
            isLoading = false
        }
    }
}