package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.presentation.screen.appointment.AppointmentHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHistoryScreen(
    onBack: () -> Unit,
    viewModel: AppointmentHistoryViewModel = hiltViewModel()
) {
    val appointments by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sắp tới", "Đã khám", "Đã hủy")

    var searchQuery by remember { mutableStateOf("") }
    var isAscending by remember { mutableStateOf(true) }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    // LOGIC LỌC, TÌM KIẾM VÀ SẮP XẾP CHUẨN
    val filteredList = appointments.filter { appt ->
        val matchTab = when (selectedTab) {
            0 -> appt.status == "PENDING" || appt.status == "PAID"
            1 -> appt.status == "COMPLETED"
            2 -> appt.status == "CANCELLED"
            else -> true
        }

        // Fix lỗi tìm kiếm: Bỏ khoảng trắng 2 đầu và kiểm tra chuỗi rỗng
        val query = searchQuery.trim()
        val matchSearch = if (query.isEmpty()) true else {
            appt.doctorName.contains(query, ignoreCase = true) ||
                    appt.reason.contains(query, ignoreCase = true)
        }

        matchTab && matchSearch
    }.let { list ->
        if (isAscending) {
            list.sortedBy { it.dateTimeUtc }
        } else {
            list.sortedByDescending { it.dateTimeUtc }
        }
    }

    if (appointmentToCancel != null) {
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            title = { Text("Xác nhận hủy lịch", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn hủy lịch hẹn với ${appointmentToCancel?.doctorName} không? Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelAppointment(appointmentToCancel!!.id)
                        appointmentToCancel = null
                    }
                ) {
                    Text("Đồng ý hủy", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToCancel = null }) {
                    Text("Đóng")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch hẹn của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm bác sĩ, triệu chứng...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { isAscending = !isAscending },
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color(0xFFEBF8FF), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = "Sort",
                        tint = Color(0xFF2563EB)
                    )
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF2563EB)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color(0xFF2563EB) else Color.Gray
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Không có lịch hẹn nào.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { appt ->
                        AppointmentCard(
                            appointment = appt,
                            formattedTime = viewModel.formatDateTime(appt.dateTimeUtc),
                            onCancelClick = { appointmentToCancel = appt }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    appointment: Appointment,
    formattedTime: String,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF1E293B)
                )
                StatusBadge(status = appointment.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEBF8FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Text("👨‍⚕️", fontSize = 20.sp) }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text("Bác sĩ", fontSize = 12.sp, color = Color.Gray)
                    Text(appointment.doctorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (appointment.reason.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Triệu chứng: ${appointment.reason}",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }

            if (appointment.status == "PENDING" || appointment.status == "PAID") {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFEF2F2),
                        contentColor = Color(0xFFDC2626)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Hủy lịch hẹn", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        "PAID" -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "ĐÃ THANH TOÁN")
        "PENDING" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "CHỜ THANH TOÁN")
        "COMPLETED" -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "HOÀN THÀNH")
        "CANCELLED" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "ĐÃ HỦY")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), status)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}