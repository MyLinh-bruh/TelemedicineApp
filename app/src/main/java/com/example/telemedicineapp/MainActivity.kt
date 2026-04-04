package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.presentation.screen.doctor.DoctorViewModel // Đảm bảo import đúng package của ViewModel
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
fun AppNavigation(doctorViewModel: DoctorViewModel = hiltViewModel()) {
    val navController = rememberNavController()

    // 🌟 Lấy dữ liệu danh sách Bác sĩ Realtime từ Firebase thông qua ViewModel
    val allDoctors by doctorViewModel.doctors.collectAsState()

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
                        Role.PATIENT -> "patient_home"
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

        // 3. MÀN HÌNH ADMIN
        composable("admin_dashboard") {
            AdminHomeScreen(
                allDoctors = allDoctors, // Truyền List<User> thật từ Firebase
                onDoctorClick = { doctor ->
                    // Logic xem chi tiết bác sĩ dành cho Admin (nếu có)
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 4. MÀN HÌNH BỆNH NHÂN
        composable("patient_home") {
            DoctorListScreen(
                allDoctors = allDoctors, // Truyền List<User> thật từ Firebase
                onDoctorClick = { doctor ->
                    // Logic chuyển sang màn hình DoctorDetailScreen
                    // Ví dụ: navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                }
            )
        }

        // 5. MÀN HÌNH BÁC SĨ
        composable("doctor_dashboard") {
            // Giao diện chính của Bác sĩ sau khi đăng nhập sẽ hiển thị ở đây
        }
    }
}