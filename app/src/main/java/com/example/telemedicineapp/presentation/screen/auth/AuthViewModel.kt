package com.example.telemedicineapp.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1500) // Giả lập mạng chậm 1.5 giây

            if (email.isNotEmpty() && pass.isNotEmpty()) {
                tokenManager.saveToken("eyJhbGciOiJIUzI1NiIsInR5c...")
                _loginSuccess.value = true
            }
            _isLoading.value = false
        }
    }
}