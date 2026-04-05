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
import com.example.telemedicineapp.presentation.screen.doctor.DoctorViewModel
import com.example.telemedicineapp.ui.screens.AdminHomeScreen
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.screens.BookingScreen // 👈 THÊM MỚI Ở ĐÂY: Import màn hình BookingScreen
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

    // Lấy danh sách Bác sĩ Realtime từ Firebase
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
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    // Nếu Admin bấm vào bác sĩ, cũng có thể chuyển sang trang chi tiết
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 4. MÀN HÌNH BỆNH NHÂN (DANH SÁCH BÁC SĨ)
        composable("patient_home") {
            DoctorListScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                }
            )
        }

        // 5. MÀN HÌNH CHI TIẾT BÁC SĨ (ĐÃ CẬP NHẬT)
        composable("doctor_detail/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            val selectedDoctor = allDoctors.find { it.id == doctorId }

            if (selectedDoctor != null) {
                DoctorDetailScreen(
                    doctor = selectedDoctor,
                    onBack = {
                        navController.popBackStack()
                    },
                    onBookClick = { id -> // 👈 THÊM MỚI Ở ĐÂY: Xử lý khi nhấn "Đặt lịch ngay"
                        navController.navigate("booking_screen/$id")
                    }
                )
            }
        }

        // 6. MÀN HÌNH ĐẶT LỊCH (👈 THÊM MỚI Ở ĐÂY)
        composable("booking_screen/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            val selectedDoctor = allDoctors.find { it.id == doctorId }

            if (selectedDoctor != null) {
                BookingScreen(
                    doctor = selectedDoctor,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // 7. MÀN HÌNH BÁC SĨ (Dự phòng)
        composable("doctor_dashboard") {
        }
    }
}