package com.example.telemedicineapp

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.presentation.screen.auth.RegisterDoctorScreen
import com.example.telemedicineapp.presentation.screen.auth.RegisterScreen
import com.example.telemedicineapp.presentation.screens.auth.LoginScreen
import com.example.telemedicineapp.presentation.screens.auth.AuthViewModel
import com.example.telemedicineapp.presentation.screen.doctor.DoctorViewModel
import com.example.telemedicineapp.presentation.screen.doctor.dashboard.DoctorDashboardScreen
import com.example.telemedicineapp.presentation.screen.ui.screens.PatientProfileScreen
import com.example.telemedicineapp.ui.screens.*
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Device Token: ${task.result}")
            }
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
    authViewModel: AuthViewModel = hiltViewModel(),
    tokenManager: TokenManager
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val allDoctorsForAdmin by doctorViewModel.allDoctors.collectAsState()
    val approvedDoctorsForPatient by doctorViewModel.doctors.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val authErrorMessage by authViewModel.errorMessage.collectAsState()

    LaunchedEffect(authErrorMessage) {
        authErrorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            authViewModel.clearError()
        }
    }

    val startDestination = remember {
        if (tokenManager.getPendingEmail() != null) "register_doctor_screen" else "login_screen"
    }

    NavHost(navController = navController, startDestination = startDestination) {

        // --- AUTH SCREENS ---
        composable("login_screen") {
            LoginScreen(
                viewModel = authViewModel,
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

        // --- ADMIN SCREENS ---
        composable("admin_dashboard") {
            AdminHomeScreen(
                allDoctors = allDoctorsForAdmin,
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) navController.navigate("doctor_detail/${doctor.id}")
                },
                onApproveClick = { doctor -> doctorViewModel.approveDoctor(doctor) },
                onRejectClick = { doctor -> doctorViewModel.rejectDoctor(doctor) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login_screen") {
                        popUpTo("admin_dashboard") { inclusive = true }
                    }
                },
                onDeleteDoctorsClick = { selectedDoctorIds ->
                    authViewModel.deleteSelectedDoctors(selectedDoctorIds)
                }
            )
        }

        // --- PATIENT SCREENS ---
        composable("patient_home") {
            val user = currentUser
            val displayName = user?.name?.takeIf { it.isNotBlank() } ?: user?.email ?: "Khách"

            DoctorListScreen(
                userName = displayName,
                userImageUrl = user?.imageUrl ?: "",
                allDoctors = approvedDoctorsForPatient,
                onDoctorClick = { doctor ->
                    if (doctor.id.isNotEmpty()) navController.navigate("doctor_detail/${doctor.id}")
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate("login_screen") {
                        popUpTo("patient_home") { inclusive = true }
                    }
                },
                onProfileClick = { navController.navigate("patient_profile") },
                onRegisterDoctorClick = { navController.navigate("register_doctor_screen") },
                onHistoryClick = { navController.navigate("appointment_history") },
                onMedicalRecordsClick = { navController.navigate("patient_medical_records") }
            )
        }

        composable("patient_profile") {
            PatientProfileScreen(navController = navController)
        }

        composable("appointment_history") {
            AppointmentHistoryScreen(
                onBack = { navController.popBackStack() },
                onViewRecordClick = { appointment ->
                    val encodedName = Uri.encode(appointment.patientName.ifEmpty { "Khách" })
                    navController.navigate("patient_view_record/${appointment.patientId}/$encodedName/${appointment.doctorId}")
                }
            )
        }

        composable("patient_medical_records") {
            val user = currentUser
            if (user != null) {
                PatientMedicalRecordScreen(
                    patientId = user.email,
                    onBack = { navController.popBackStack() },
                    onRecordClick = { record ->
                        val encodedName = Uri.encode(record.patientName.ifEmpty { "Khách" })
                        navController.navigate("patient_view_record/${record.patientId}/$encodedName/${record.doctorId}")
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        composable(
            route = "patient_view_record/{patientId}/{patientName}/{doctorId}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pId = backStackEntry.arguments?.getString("patientId") ?: ""
            val pName = backStackEntry.arguments?.getString("patientName") ?: ""
            val dId = backStackEntry.arguments?.getString("doctorId") ?: ""

            MedicalRecordScreen(
                patientId = pId,
                patientName = pName,
                doctorId = dId,
                onBack = { navController.popBackStack() },
                isReadOnly = true // Bệnh nhân chỉ đọc
            )
        }

        composable(
            route = "doctor_detail/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val selectedDoctor = allDoctorsForAdmin.find { it.id == doctorId } ?: approvedDoctorsForPatient.find { it.id == doctorId }

            selectedDoctor?.let { doctor ->
                DoctorDetailScreen(
                    doctor = doctor,
                    onBack = { navController.popBackStack() },
                    onBookClick = { id -> navController.navigate("booking_screen/$id") }
                )
            }
        }

        composable(
            route = "booking_screen/{doctorId}",
            arguments = listOf(navArgument("doctorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val doctor = approvedDoctorsForPatient.find { it.id == doctorId } ?: allDoctorsForAdmin.find { it.id == doctorId }

            if (doctor != null) {
                BookingScreen(
                    doctor = doctor,
                    onBack = { navController.popBackStack() },
                    onNavigateToProfile = { navController.navigate("patient_profile") }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // --- DOCTOR SCREENS ---
        composable("doctor_dashboard") {
            val user = currentUser
            if (user != null && user.id.isNotEmpty()) {
                DoctorDashboardScreen(
                    doctorId = user.id,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate("login_screen") {
                            popUpTo("doctor_dashboard") { inclusive = true }
                        }
                    },
                    onPatientClick = { patientId, patientName ->
                        val encodedName = Uri.encode(patientName)
                        navController.navigate("medical_record_screen/$patientId/$encodedName/${user.id}")
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        composable(
            route = "medical_record_screen/{patientId}/{patientName}/{doctorId}",
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("patientName") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pId = backStackEntry.arguments?.getString("patientId") ?: ""
            val pName = backStackEntry.arguments?.getString("patientName") ?: ""
            val dId = backStackEntry.arguments?.getString("doctorId") ?: ""

            MedicalRecordScreen(
                patientId = pId,
                patientName = pName,
                doctorId = dId,
                onBack = { navController.popBackStack() },
                isReadOnly = false // Bác sĩ được phép sửa
            )
        }
    }
}