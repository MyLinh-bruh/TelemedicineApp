package com.example.telemedicineapp

import android.net.Uri // 🌟 Thêm import Uri để mã hóa chuỗi
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screens.auth.AuthViewModel
import com.example.telemedicineapp.presentation.screen.doctor.DoctorViewModel
import com.example.telemedicineapp.presentation.screen.doctor.dashboard.DoctorDashboardScreen
import com.example.telemedicineapp.ui.screens.*
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
fun AppNavigation(
    doctorViewModel: DoctorViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    // Lắng nghe dữ liệu
    val allDoctors by doctorViewModel.doctors.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

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
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") { popUpTo("admin_dashboard") { inclusive = true } }
                }
            )
        }

        // 4. MÀN HÌNH BỆNH NHÂN
        composable("patient_home") {
            DoctorListScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    navController.navigate("login_screen") { popUpTo("patient_home") { inclusive = true } }
                },
                onProfileClick = { /* Logic xem Profile */ },
                onRegisterDoctorClick = { navController.navigate("doctor_dashboard") }
            )
        }

        // 5. MÀN HÌNH CHI TIẾT BÁC SĨ
        composable(
            route = "doctor_detail/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val selectedDoctor = allDoctors.find { it.id == doctorId }

            selectedDoctor?.let { doctor ->
                DoctorDetailScreen(
                    doctor = doctor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { id -> navController.navigate("booking_screen/$id") }
                )
            }
        }

        // 6. MÀN HÌNH ĐẶT LỊCH
        composable(
            route = "booking_screen/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctor = allDoctors.find { it.id == doctorId }

            // Chỉ hiển thị khi có đủ Bác sĩ và Người bệnh đang đăng nhập
            if (doctor != null && currentUser != null) {
                BookingScreen(
                    patient = currentUser!!,
                    doctor = doctor,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // 7. MÀN HÌNH DASHBOARD BÁC SĨ
        composable("doctor_dashboard") {
            val doctorId = currentUser?.id ?: "5JTeKVE65z25NtxJIAkm"

            DoctorDashboardScreen(
                doctorId = doctorId,
                onLogout = {
                    navController.navigate("login_screen") { popUpTo("doctor_dashboard") { inclusive = true } }
                },
                onPatientClick = { patientId, patientName ->
                    // 🌟 MÃ HÓA CHUỖI ĐỂ TRÁNH CRASH KHI CÓ KHOẢNG TRẮNG HOẶC KÝ TỰ ĐẶC BIỆT
                    val encodedName = Uri.encode(patientName)
                    navController.navigate("medical_record_screen/$patientId/$encodedName/$doctorId")
                }
            )
        }

        // 8. MÀN HÌNH BỆNH ÁN (TẠO/XEM)
        composable(
            route = "medical_record_screen/{patientId}/{patientName}/{doctorId}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            val patientName = backStackEntry.arguments?.getString("patientName") ?: "" // Tự động decode lại tên
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""

            MedicalRecordScreen(
                patientId = patientId,
                patientName = patientName,
                doctorId = doctorId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}