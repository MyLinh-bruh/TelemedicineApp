package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.model.Doctor
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Dùng Theme của Thảo để UI hiển thị đúng màu sắc
            TelemedicineAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Khởi tạo bộ điều hướng của Trưởng nhóm
                    val navController = rememberNavController()

                    // NavHost quản lý danh sách các màn hình
                    NavHost(navController = navController, startDestination = "login_screen") {

                        // 1. Màn hình Đăng nhập (Của bạn)
                        composable("login_screen") {
                            LoginScreen(
                                onLoginSuccess = {
                                    // Chuyển sang màn hình bác sĩ và xóa màn hình đăng nhập khỏi bộ nhớ
                                    navController.navigate("doctor_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Màn hình Bác sĩ (Lắp ráp code của Thảo vào đây)
                        composable("doctor_screen") {
                            // Trạng thái lưu trữ xem user đang chọn bác sĩ nào
                            var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }

                            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding),
                                    color = Color(0xFFF8FAFC)
                                ) {
                                    // Nếu chưa chọn bác sĩ -> Hiện danh sách
                                    if (selectedDoctor == null) {
                                        DoctorListScreen(onDoctorClick = { doctor ->
                                            selectedDoctor = doctor
                                        })
                                    }
                                    // Nếu đã click vào 1 bác sĩ -> Hiện chi tiết
                                    else {
                                        DoctorDetailScreen(
                                            doctor = selectedDoctor!!,
                                            onBack = { selectedDoctor = null }
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}