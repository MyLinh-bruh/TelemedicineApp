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
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen // Nhớ import màn hình này
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
                    // 🌟 KHI BẤM VÀO BÁC SĨ, CHUYỂN HƯỚNG VÀ TRUYỀN ID CỦA BÁC SĨ ĐÓ
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                }
            )
        }

        // 5. MÀN HÌNH CHI TIẾT BÁC SĨ (MỚI THÊM)
        composable("doctor_detail/{doctorId}") { backStackEntry ->
            // Lấy ID bác sĩ từ đường dẫn (Route)
            val doctorId = backStackEntry.arguments?.getString("doctorId")

            // Tìm bác sĩ trong danh sách allDoctors có ID khớp với ID trên đường dẫn
            val selectedDoctor = allDoctors.find { it.id == doctorId }

            // Nếu tìm thấy bác sĩ thì hiển thị màn hình chi tiết
            if (selectedDoctor != null) {
                DoctorDetailScreen(
                    doctor = selectedDoctor,
                    onBack = {
                        // Nút quay lại: Rút màn hình hiện tại ra khỏi ngăn xếp (về lại danh sách)
                        navController.popBackStack()
                    }
                )
            }
        }

        // 6. MÀN HÌNH BÁC SĨ (Dự phòng)
        composable("doctor_dashboard") {
        }
    }
}