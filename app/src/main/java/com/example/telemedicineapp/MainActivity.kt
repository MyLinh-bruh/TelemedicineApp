package com.example.telemedicineapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.telemedicineapp.presentation.screen.appointment.AppointmentHistoryViewModel
import com.example.telemedicineapp.ui.screens.AdminHomeScreen
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.screens.BookingScreen
import com.example.telemedicineapp.ui.screens.AppointmentHistoryScreen
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    // --- XIN QUYỀN THÔNG BÁO (ANDROID 13+) ---
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Quyền thông báo đã được cấp")
        } else {
            Log.w("FCM", "Người dùng từ chối cấp quyền thông báo")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Kiểm tra và xin quyền gửi Notification nếu máy chạy Android 13 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 2. Lấy Device Token từ Firebase Cloud Messaging
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM_TOKEN", "Lấy FCM token thất bại", task.exception)
                return@addOnCompleteListener
            }

            // Lấy token thành công
            val token = task.result

            // IN RA LOGCAT ĐỂ LẤY MÃ ĐI DEMO
            Log.d("FCM_TOKEN", "Device Token của máy này: $token")
        }

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
        // --- CÁC MÀN HÌNH CŨ GIỮ NGUYÊN ---
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
                allDoctors = allDoctorsForAdmin,
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) {
                        navController.navigate("doctor_detail/${doctor.id}")
                    }
                },
                onApproveClick = { doctor -> doctorViewModel.approveDoctor(doctor) },
                onRejectClick = { doctor -> doctorViewModel.rejectDoctor(doctor) },
                onLogout = {
                    tokenManager.clearSession() // Xóa session khi logout
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                }
            )
        }

        composable("patient_home") {
            DoctorListScreen(
                allDoctors = approvedDoctorsForPatient,
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) {
                        navController.navigate("doctor_detail/${doctor.id}")
                    }
                },
                onLogout = {
                    tokenManager.clearSession()
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                },
                // 🌟 CẬP NHẬT: Điều hướng tới trang hồ sơ khi click profile
                onProfileClick = {
                    navController.navigate("patient_profile")
                },
                onRegisterDoctorClick = {
                    navController.navigate("register_doctor_screen")
                },
                onHistoryClick = {
                    navController.navigate("appointment_history")
                }
            )
        }

        // 🌟 THÊM MỚI: Điều hướng tới trang hồ sơ bệnh nhân
        composable("patient_profile") {
            com.example.telemedicineapp.presentation.screen.ui.screens.PatientProfileScreen(
                navController = navController
            )
        }

        composable("doctor_detail/{doctorId}") { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId")
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

        composable("appointment_history") {
            AppointmentHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}