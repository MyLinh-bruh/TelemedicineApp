package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.model.Doctor
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.ui.screens.AdminHomeScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemedicineAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Dữ liệu bác sĩ dùng chung
    val allDoctors = remember {
        listOf(
            Doctor(1, "BS. Lê Mạnh Hùng", "Tim mạch", 4.9, "", "Mô tả...", "Đà Nẵng", "BV Đa khoa", 0xFF3B82F6L),
            Doctor(2, "BS. Phan Mỹ Linh", "Nhi khoa", 4.8, "", "Mô tả...", "Đà Nẵng", "BV Phụ sản Nhi", 0xFFF43F5EL)
        )
    }

    NavHost(
        navController = navController,
        startDestination = "login_screen"
    ) {
        // 1. MÀN HÌNH ĐĂNG NHẬP
        composable("login_screen") {
            LoginScreen(
                onLoginSuccess = { role ->
                    val destination = when (role) {
                        Role.ADMIN -> "admin_dashboard"
                        Role.PATIENT -> "patient_home" // Đường dẫn này phải khớp với composable bên dưới
                        Role.DOCTOR -> "doctor_dashboard"
                    }
                    navController.navigate(destination) {
                        popUpTo("login_screen") { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate("register_screen") }
            )
        }

        // 2. MÀN HÌNH ĐĂNG KÝ
        composable("register_screen") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // 3. MÀN HÌNH ADMIN (Đã gộp và sửa tên route)
        composable("admin_dashboard") {
            AdminHomeScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    // Logic xem chi tiết bác sĩ
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 4. MÀN HÌNH BỆNH NHÂN (FIX LỖI: Trước đó bạn bị thiếu phần này nên bị Out)
        composable("patient_home") {
            DoctorListScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    // Logic xem chi tiết bác sĩ
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                }
            )
        }

        // 5. MÀN HÌNH BÁC SĨ (Dự phòng)
        composable("doctor_dashboard") {
            // Giao diện bác sĩ dán ở đây
        }
    }
}