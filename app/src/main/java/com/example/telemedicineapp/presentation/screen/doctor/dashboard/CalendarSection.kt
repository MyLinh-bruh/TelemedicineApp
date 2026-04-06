package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.utils.TimeUtils
import java.time.LocalDate

@Composable
fun CalendarSection(
    viewModel: DoctorDashboardViewModel,
    onAppointmentClick: (Appointment) -> Unit
) {
    val markedDays: List<String> by viewModel.markedDays.collectAsState(initial = emptyList())
    val allAppointments: List<Appointment> by viewModel.appointments.collectAsState(initial = emptyList())

    // State lưu giữ ngày đang chọn
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Lọc danh sách khám của ngày được chọn
    val appointmentsToday = allAppointments.filter {
        it.dateTimeUtc.substring(0, 10) == selectedDate.toString()
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Lịch làm việc", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(12.dp)) {

                // 🌟 BỘ ĐIỀU KHIỂN THÁNG/NĂM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedDate = selectedDate.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước", tint = Color(0xFF2563EB))
                    }

                    Text(
                        text = "Tháng ${selectedDate.monthValue} / ${selectedDate.year}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        fontSize = 18.sp
                    )

                    IconButton(onClick = { selectedDate = selectedDate.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau", tint = Color(0xFF2563EB))
                    }
                }

                Spacer(Modifier.height(8.dp))

                val daysInMonth = selectedDate.lengthOfMonth()

                LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(260.dp)) {
                    items(daysInMonth) { index ->
                        val day = index + 1
                        val dateStr = "${selectedDate.year}-${selectedDate.monthValue.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
                        val hasSchedule = markedDays.contains(dateStr)
                        val isSelected = selectedDate.dayOfMonth == day

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2563EB) else if (hasSchedule) Color(0xFFDCFCE7) else Color.Transparent)
                                .clickable { selectedDate = selectedDate.withDayOfMonth(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                color = if (isSelected) Color.White else if (hasSchedule) Color(0xFF166534) else Color.Black,
                                fontWeight = if (isSelected || hasSchedule) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- DANH SÁCH LỊCH HẸN BÊN DƯỚI ---
        if (appointmentsToday.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎉", fontSize = 40.sp)
                    Text("Hôm nay không có lịch", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Bác sĩ có thể nghỉ ngơi!", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(appointmentsToday) { appt: Appointment ->

                    // 🌟 KIỂM TRA TRẠNG THÁI VÀ ĐỔI MÀU XANH
                    val isCompleted = appt.status == "COMPLETED"
                    val bgColor = if (isCompleted) Color(0xFFDCFCE7) else Color.White // Xanh mint nhạt nếu đã khám
                    val textColor = if (isCompleted) Color(0xFF166534) else Color(0xFF1E293B) // Chữ xanh lá thẫm

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onAppointmentClick(appt) },
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(if (isCompleted) 0.dp else 2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if(appt.patientName.isEmpty()) "Bệnh nhân ẩn danh" else appt.patientName,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = textColor
                                    )
                                    // 🌟 HIỆN NHÃN "ĐÃ KHÁM"
                                    if (isCompleted) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(color = Color(0xFF22C55E), shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                text = "Đã khám",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Lý do: ${appt.reason}", fontSize = 13.sp, color = if (isCompleted) Color(0xFF166534).copy(alpha = 0.7f) else Color.Gray)
                            }
                            val time = TimeUtils.convertUtcToLocalDisplay(appt.dateTimeUtc).split(" - ")[0]
                            Text(time, color = if (isCompleted) Color(0xFF166534) else Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}