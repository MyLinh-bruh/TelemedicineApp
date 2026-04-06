package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.DoctorSchedule
import java.util.*

@OptIn(ExperimentalMaterial3Api::class) // 🌟 Thêm dòng này để Compose không báo lỗi thẻ FilterChip
@Composable
fun BusyScheduleSection(doctorId: String, viewModel: DoctorDashboardViewModel) {
    val context = LocalContext.current

    val busySchedules: List<DoctorSchedule> by viewModel.busySchedules.collectAsState(initial = emptyList())
    val availableSlots: List<String> by viewModel.availableSlotsForSetup.collectAsState(initial = emptyList())

    // 🌟 THÊM DEFAULT SLOTS: Danh sách các khung giờ chuẩn để bác sĩ chọn (từ 8h sáng -> 16h30 chiều)
    val defaultSlots = listOf(
        "08:00 - 08:30", "08:30 - 09:00", "09:00 - 09:30", "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00", "11:00 - 11:30",
        "13:00 - 13:30", "13:30 - 14:00", "14:00 - 14:30", "14:30 - 15:00", "15:00 - 15:30", "15:30 - 16:00", "16:00 - 16:30"
    )
    // Nếu ViewModel trả về rỗng, dùng luôn defaultSlots
    val displaySlots = availableSlots.takeIf { it.isNotEmpty() } ?: defaultSlots

    // States cho nghỉ 1 ngày hoặc nghỉ dài ngày
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val selectedBusySlots = remember { mutableStateListOf<String>() }

    LaunchedEffect(doctorId) { viewModel.fetchBusySchedules(doctorId) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Quản lý thời gian nghỉ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(20.dp))

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thiết lập khoảng ngày nghỉ", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Chọn ngày bắt đầu
                        OutlinedButton(
                            onClick = { showDatePicker(context) { startDate = it; viewModel.loadSlotsForDate(it) } },
                            modifier = Modifier.weight(1f)
                        ) { Text(startDate.ifEmpty { "Từ ngày" }, fontSize = 12.sp) }

                        // Chọn ngày kết thúc
                        OutlinedButton(
                            onClick = { showDatePicker(context) { endDate = it } },
                            modifier = Modifier.weight(1f)
                        ) { Text(endDate.ifEmpty { "Đến ngày" }, fontSize = 12.sp) }
                    }

                    if (startDate.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Chọn giờ bận:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            // NÚT CHỌN CẢ NGÀY (Dùng size của displaySlots để bắt điều kiện)
                            TextButton(onClick = {
                                if (selectedBusySlots.size == displaySlots.size) selectedBusySlots.clear()
                                else { selectedBusySlots.clear(); selectedBusySlots.addAll(displaySlots) }
                            }) {
                                Text(if (selectedBusySlots.size == displaySlots.size) "Bỏ chọn" else "Chọn cả ngày", fontSize = 13.sp)
                            }
                        }

                        // 🌟 Hiển thị Grid giờ bận từ biến displaySlots
                        displaySlots.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { slot ->
                                    val isSelected = selectedBusySlots.contains(slot)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { if (isSelected) selectedBusySlots.remove(slot) else selectedBusySlots.add(slot) },
                                        label = { Text(slot, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Mẹo nhỏ: Bù thêm Spacer nếu hàng đó bị lẻ 1 cột (để giao diện không bị lệch)
                                if (row.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (startDate.isEmpty() || selectedBusySlots.isEmpty()) return@Button
                                val finalEnd = endDate.ifEmpty { startDate } // Nếu không chọn ngày kết thúc thì coi như nghỉ 1 ngày

                                viewModel.saveBusyRange(doctorId, startDate, finalEnd, selectedBusySlots.toList()) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Đã khóa lịch thành công!", Toast.LENGTH_SHORT).show()
                                        startDate = ""; endDate = ""; selectedBusySlots.clear()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("XÁC NHẬN KHÓA LỊCH", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            Text("Lịch nghỉ hiện tại", fontWeight = FontWeight.Bold, color = Color.Gray)
        }

        items(busySchedules) { schedule: DoctorSchedule ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(schedule.date, fontWeight = FontWeight.Bold)
                        Text("Bận: ${schedule.busySlots.joinToString(", ")}", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { viewModel.deleteBusySchedule(schedule.id) { } }) {
                        Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                    }
                }
            }
        }
    }
}

// Hàm bổ trợ hiện DatePicker
fun showDatePicker(context: android.content.Context, onDateSelected: (String) -> Unit) {
    val c = Calendar.getInstance()
    DatePickerDialog(context, { _, y, m, d ->
        onDateSelected("$y-${(m + 1).toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}")
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}