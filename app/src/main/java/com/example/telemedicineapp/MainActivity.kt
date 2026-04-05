package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screen.auth.RegisterDoctorScreen
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.presentation.screen.doctor.DoctorViewModel
import com.example.telemedicineapp.ui.screens.*
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager // Inject TokenManager để kiểm tra trạng thái chờ

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemedicineAppTheme {
                // Truyền tokenManager vào AppNavigation
                AppNavigation(tokenManager = tokenManager)
            }
        }
    }
}

@Composable
fun AppNavigation(
    doctorViewModel: DoctorViewModel = hiltViewModel(),
    tokenManager: TokenManager
) {
    val navController = rememberNavController()
    val allDoctors by doctorViewModel.doctors.collectAsState()

    // 🌟 LOGIC QUAN TRỌNG: Kiểm tra xem có email bác sĩ nào đang chờ duyệt không
    // Nếu có email trong máy -> Vào thẳng màn hình RegisterDoctor (nơi có Popup chờ)
    // Nếu không -> Vào màn hình Login như bình thường
    val startDestination = remember {
        if (tokenManager.getPendingEmail() != null) {
            "register_doctor_screen"
        } else {
            "login_screen"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onGoToRegisterPatient = { navController.navigate("register_screen") },
                onGoToRegisterDoctor = { navController.navigate("register_doctor_screen") }
            )
        }

        // 2. MÀN HÌNH ĐĂNG KÝ BỆNH NHÂN
        composable("register_screen") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // 3. MÀN HÌNH ĐĂNG KÝ BÁC SĨ (Vào đây sẽ tự hiện Popup chờ duyệt nhờ logic trong ViewModel)
        composable("register_doctor_screen") {
            RegisterDoctorScreen(
                onRegisterSuccess = {
                    navController.navigate("login_screen") {
                        popUpTo("register_doctor_screen") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    // Nếu đang có đơn chờ duyệt mà bấm quay lại, ta nên cho về Login
                    if (tokenManager.getPendingEmail() != null) {
                        navController.navigate("login_screen") {
                            popUpTo("register_doctor_screen") { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }

        // 4. MÀN HÌNH ADMIN
        composable(route = "admin_dashboard") {
            AdminHomeScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor ->
                    navController.navigate("doctor_detail/${doctor.id}")
                },
                onApproveClick = { doctor ->
                    doctorViewModel.approveDoctor(doctor)
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        // 5. MÀN HÌNH BỆNH NHÂN (DANH SÁCH BÁC SĨ)
        composable("patient_home") {
            DoctorListScreen(
                allDoctors = allDoctors,
                onDoctorClick = { doctor -> navController.navigate("doctor_detail/${doctor.id}") },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                },
                onProfileClick = { /* Điều hướng tới Profile */ },
                onRegisterDoctorClick = { navController.navigate("register_doctor_screen") }
            )
        }

        // 6. CHI TIẾT BÁC SĨ
        composable("doctor_detail/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            val selectedDoctor = allDoctors.find { it.id == doctorId }
            if (selectedDoctor != null) {
                DoctorDetailScreen(
                    doctor = selectedDoctor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { id -> navController.navigate("booking_screen/$id") }
                )
            }
        }

        // 7. MÀN HÌNH ĐẶT LỊCH
        composable("booking_screen/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            val selectedDoctor = allDoctors.find { it.id == doctorId }
            if (selectedDoctor != null) {
                BookingScreen(
                    doctor = selectedDoctor,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // 8. MÀN HÌNH DASHBOARD CHO BÁC SĨ
        composable("doctor_dashboard") {
            // Hiển thị giao diện riêng cho bác sĩ
        }
    }
}