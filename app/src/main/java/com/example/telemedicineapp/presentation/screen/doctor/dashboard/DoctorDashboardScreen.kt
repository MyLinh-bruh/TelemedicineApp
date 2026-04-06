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
    onPatientClick: (String, String) -> Unit, // Điều hướng sang màn hình bệnh án
    viewModel: DoctorDashboardViewModel = hiltViewModel()
) {
    // 1. Quản lý trạng thái: Tab hiện tại và Chế độ chỉnh sửa hồ sơ
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditingProfile by remember { mutableStateOf(false) }

    // 2. Tự động tải dữ liệu khi vào màn hình
    LaunchedEffect(doctorId) {
        viewModel.listenToData(doctorId)
    }

    // 3. LOGIC CHUYỂN ĐỔI MÀN HÌNH
    if (isEditingProfile) {
        // HIỆN MÀN HÌNH CHỈNH SỬA (Nếu bác sĩ bấm vào Profile Card)
        EditDoctorProfileScreen(
            viewModel = viewModel,
            onBack = { isEditingProfile = false }
        )
    } else {
        // HIỆN GIAO DIỆN DASHBOARD CHÍNH
        Scaffold(
            containerColor = Color(0xFFF8FAFC),

            // NÚT CỘNG: Gửi phiếu khám mới (Hồ sơ mới)
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onPatientClick("new", "Hồ sơ mới") },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm bệnh án", tint = Color.White)
                }
            },

            // THANH ĐIỀU HƯỚNG DƯỚI CÙNG
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
                            onPatientClick(appt.patientId, pName)
                        }
                    )

                    // TAB 1: QUẢN LÝ NGHỈ (KHÓA LỊCH)
                    1 -> BusyScheduleSection(doctorId, viewModel)

                    // TAB 2: HỒ SƠ & QUẢN LÝ BỆNH NHÂN
                    2 -> ProfileTab(
                        viewModel = viewModel,
                        onEditProfileClick = { isEditingProfile = true }, // 🌟 Mở màn hình sửa khi bấm vào Card
                        onLogout = onLogout,
                        onPatientClick = onPatientClick
                    )
                }
            }
        }
    }
}