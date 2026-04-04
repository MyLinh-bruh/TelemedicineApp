package com.example.telemedicineapp

import android.os.Bundle // Sửa lỗi Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.ui.screens.AdminHomeScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen // Import màn hình của Thảo
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // SỬA LỖI: Thêm dấu ? sau Bundle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemedicineAppTheme {
                // Khởi tạo bộ điều hướng
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login_screen"
    ) {
        // 1. MÀN HÌNH ĐĂNG NHẬP
        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = { role ->
                    // Logic phân quyền điều hướng
                    val destination = when (role) {
                        Role.ADMIN -> "admin_dashboard"
                        Role.DOCTOR -> "doctor_dashboard"
                        Role.PATIENT -> "patient_home"
                    }
                    navController.navigate(destination) {
                        popUpTo("login_screen") { inclusive = true }
                    }
                },
                onGoToRegister = {
                    navController.navigate("register_screen")
                }
            )
        }

        // 2. MÀN HÌNH ĐĂNG KÝ
        composable("register_screen") {
            RegisterScreen(
                onRegisterSuccess = {
                    // Đăng ký xong quay về Login theo ý bạn
                    navController.popBackStack()
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // 3. MÀN HÌNH DANH SÁCH BÁC SĨ (KẾT NỐI VỚI THẢO)
        composable("patient_home") {
            DoctorListScreen(
                onDoctorClick = { doctor ->
                    // Logic khi nhấn vào 1 bác sĩ (sau này làm trang Detail)
                    println("Đang xem bác sĩ: ${doctor.name}")
                }
            )
        }

        // 4. CÁC MÀN HÌNH CHỨC NĂNG KHÁC (ADMIN/DOCTOR)
        composable("admin_dashboard") {
            AdminHomeScreen(
                allDoctors = emptyList(), // Sau này bạn truyền danh sách bác sĩ từ ViewModel vào đây
                onDoctorClick = { doctor ->
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    // Thoát về màn hình đăng nhập
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("doctor_dashboard") {
            // Sau này dán màn hình Bác sĩ của bạn vào đây
        }
    }
}