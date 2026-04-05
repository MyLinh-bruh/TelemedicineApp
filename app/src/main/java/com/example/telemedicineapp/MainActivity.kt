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
import com.example.telemedicineapp.ui.screens.AdminHomeScreen
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.screens.BookingScreen
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TelemedicineAppTheme {
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

    // Tách riêng 2 danh sách để hiển thị đúng dữ liệu
    val allDoctorsForAdmin by doctorViewModel.allDoctors.collectAsState()
    val approvedDoctorsForPatient by doctorViewModel.doctors.collectAsState()

    val startDestination = remember {
        if (tokenManager.getPendingEmail() != null) {
            "register_doctor_screen"
        } else {
            "login_screen"
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
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

        composable("register_screen") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable("register_doctor_screen") {
            RegisterDoctorScreen(
                onRegisterSuccess = {
                    navController.navigate("login_screen") {
                        popUpTo("register_doctor_screen") { inclusive = true }
                    }
                },
                onBackToLogin = {
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

        composable("admin_dashboard") {
            AdminHomeScreen(
                allDoctors = allDoctorsForAdmin, // Đưa toàn bộ danh sách cho Admin duyệt
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) {
                        navController.navigate("doctor_detail/${doctor.id}")
                    }
                },
                onApproveClick = { doctor ->
                    doctorViewModel.approveDoctor(doctor)
                },
                // 🌟 THÊM SỰ KIỆN TỪ CHỐI TẠI ĐÂY
                onRejectClick = { doctor ->
                    doctorViewModel.rejectDoctor(doctor)
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("patient_home") {
            DoctorListScreen(
                allDoctors = approvedDoctorsForPatient, // Chỉ đưa bác sĩ ĐÃ DUYỆT cho Bệnh nhân
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) {
                        navController.navigate("doctor_detail/${doctor.id}")
                    }
                },
                onLogout = {
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                },
                onProfileClick = {},
                onRegisterDoctorClick = {
                    navController.navigate("register_doctor_screen")
                }
            )
        }

        composable("doctor_detail/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            // Lấy từ danh sách Admin để có thể hiển thị kể cả bác sĩ đang chờ duyệt (nếu Admin click vào)
            val selectedDoctor = allDoctorsForAdmin.find { it.id == doctorId }

            if (selectedDoctor != null) {
                DoctorDetailScreen(
                    doctor = selectedDoctor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { id -> navController.navigate("booking_screen/$id") }
                )
            }
        }

        composable("booking_screen/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
            val selectedDoctor = allDoctorsForAdmin.find { it.id == doctorId }

            if (selectedDoctor != null) {
                BookingScreen(
                    doctor = selectedDoctor,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("doctor_dashboard") {}
    }
}