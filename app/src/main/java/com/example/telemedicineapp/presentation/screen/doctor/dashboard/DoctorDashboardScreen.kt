package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DoctorDashboardScreen(
    doctorId: String,
    onLogout: () -> Unit,
    // 🌟 ĐÃ FIX: Nhận đủ 3 tham số (apptId, patientId, patientName)
    onPatientClick: (String, String, String) -> Unit,
    viewModel: DoctorDashboardViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditingProfile by remember { mutableStateOf(false) }

    LaunchedEffect(doctorId) {
        viewModel.listenToData(doctorId)
    }

    if (isEditingProfile) {
        EditDoctorProfileScreen(
            viewModel = viewModel,
            onBack = { isEditingProfile = false }
        )
    } else {
        Scaffold(
            containerColor = Color(0xFFF8FAFC),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onPatientClick("none", "new", "Hồ sơ mới") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm bệnh án", tint = Color.White)
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.DateRange, null) },
                        label = { Text("Lịch khám") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Block, null) },
                        label = { Text("Lịch bận") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Hồ sơ") }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (selectedTab) {
                    // TAB 0: LỊCH LÀM VIỆC
                    0 -> CalendarSection(
                        viewModel = viewModel,
                        onAppointmentClick = { appt ->
                            val pName = if (appt.patientName.isBlank()) "Ẩn danh" else appt.patientName
                            // 🌟 Truyền đủ 3 thông số
                            onPatientClick(appt.id, appt.patientId, pName)
                        }
                    )

                    // TAB 1: QUẢN LÝ NGHỈ (KHÓA LỊCH)
                    1 -> BusyScheduleSection(doctorId, viewModel)

                    // TAB 2: HỒ SƠ & QUẢN LÝ BỆNH NHÂN
                    2 -> ProfileTab(
                        viewModel = viewModel,
                        onEditProfileClick = { isEditingProfile = true },
                        onLogout = onLogout,
                        // Bọc lại thành 2 tham số cho ProfileTab cũ
                        onPatientClick = { pId, pName -> onPatientClick("none", pId, pName) }
                    )
                }
            }
        }
    }
}