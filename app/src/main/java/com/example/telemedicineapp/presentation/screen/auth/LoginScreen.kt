package com.example.telemedicineapp.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.Role

@Composable
fun LoginScreen(
    onLoginSuccess: (Role) -> Unit,
    onGoToRegisterPatient: () -> Unit,
    onGoToRegisterDoctor: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val loginSuccess by viewModel.loginSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(loginSuccess) {
        loginSuccess?.let { role ->
            onLoginSuccess(role)
            viewModel.resetLoginStatus()
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("ĐÃ HIỂU")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Telemedicine", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Khám bệnh từ xa mọi lúc mọi nơi", fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email / Tài khoản") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            // 🌟 ĐÃ SỬA: Thêm .trim() để tự động xóa dấu cách thừa khi người dùng lỡ bấm
            onClick = { viewModel.login(email.trim(), password.trim()) },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("ĐĂNG NHẬP")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Bạn chưa có tài khoản?", color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onGoToRegisterPatient) {
                Text("Đăng ký Bệnh nhân", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onGoToRegisterDoctor) {
                Text("Đăng ký Bác sĩ", fontWeight = FontWeight.Bold)
            }
        }
    }
}