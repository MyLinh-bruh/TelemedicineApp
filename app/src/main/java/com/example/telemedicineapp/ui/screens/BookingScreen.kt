package com.example.telemedicineapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.presentation.screen.appointment.AppointmentViewModel
import com.example.telemedicineapp.presentation.screen.appointment.BookingState
import com.example.telemedicineapp.utils.DateItem
import com.example.telemedicineapp.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    doctor: User,
    onBack: () -> Unit,
    appointmentViewModel: AppointmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Lấy dữ liệu từ ViewModel
    val bookingState by appointmentViewModel.bookingState.collectAsState()
    val schedule by appointmentViewModel.doctorSchedule.collectAsState()
    val bookedSlots by appointmentViewModel.bookedSlots.collectAsState()

    val availableDates = remember { TimeUtils.getNext7Days() }
    var selectedDate by remember { mutableStateOf(availableDates.first()) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }

    // Gọi API lấy cả lịch rảnh và lịch đã đặt khi đổi ngày
    LaunchedEffect(selectedDate) {
        appointmentViewModel.getSchedulesAndAppointments(doctor.id, selectedDate.fullDate)
    }

    LaunchedEffect(bookingState) {
        when (bookingState) {
            is BookingState.Success -> {
                Toast.makeText(context, "Đặt lịch thành công!", Toast.LENGTH_SHORT).show()
                appointmentViewModel.resetState()
                onBack()
            }
            is BookingState.Error -> {
                Toast.makeText(context, (bookingState as BookingState.Error).message, Toast.LENGTH_SHORT).show()
                appointmentViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đặt lịch khám", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F6F9))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            DoctorSimpleInfo(doctor)

            // PHẦN CHỌN NGÀY
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ngày khám *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableDates) { dateItem ->
                        DateSelectorItem(
                            item = dateItem,
                            isSelected = selectedDate == dateItem,
                            onClick = {
                                selectedDate = dateItem
                                selectedTimeSlot = null
                            }
                        )
                    }
                    item { OtherDateButton() }
                }
            }

            // PHẦN CHỌN GIỜ
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Giờ khám *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // BUỔI SÁNG
                Text("Buổi sáng", fontWeight = FontWeight.SemiBold, color = Color.DarkGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (schedule?.morningSlots.isNullOrEmpty()) {
                    Text("Không có lịch hẹn", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    TimeSlotGrid(
                        slots = schedule!!.morningSlots,
                        selectedSlot = selectedTimeSlot,
                        bookedSlots = bookedSlots,
                        busySlots = schedule?.busySlots ?: emptyList()
                    ) { selectedTimeSlot = it }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BUỔI CHIỀU
                Text("Buổi chiều", fontWeight = FontWeight.SemiBold, color = Color.DarkGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (schedule?.afternoonSlots.isNullOrEmpty()) {
                    Text("Không có lịch hẹn", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    TimeSlotGrid(
                        slots = schedule!!.afternoonSlots, // Sửa lỗi: lấy đúng afternoonSlots
                        selectedSlot = selectedTimeSlot,
                        bookedSlots = bookedSlots,
                        busySlots = schedule?.busySlots ?: emptyList()
                    ) { selectedTimeSlot = it }
                }

                Text(
                    "Tất cả thời gian theo múi giờ Việt Nam GMT +7",
                    color = Color.Gray, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Triệu chứng / Lý do khám") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    )
                )
            }

            Button(
                onClick = {
                    if (selectedTimeSlot != null) {
                        val startTime = selectedTimeSlot!!.split(" - ")[0]
                        val utcDateTime = TimeUtils.convertLocalToUtcString(selectedDate.fullDate, startTime)
                        if (utcDateTime != null) {
                            val newAppointment = Appointment(
                                patientId = "CURRENT_USER_ID",
                                doctorId = doctor.id,
                                doctorName = doctor.name,
                                dateTimeUtc = utcDateTime,
                                reason = reason,
                                status = "CONFIRMED" // Gửi trạng thái để tính vào bookedSlots lần sau
                            )
                            appointmentViewModel.bookAppointment(newAppointment)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTimeSlot != null) Color(0xFF2563EB) else Color.LightGray
                ),
                enabled = selectedTimeSlot != null && bookingState !is BookingState.Loading
            ) {
                if (bookingState is BookingState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Xác nhận đặt lịch", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TimeSlotGrid(
    slots: List<String>,
    selectedSlot: String?,
    bookedSlots: List<String>,
    busySlots: List<String>,
    onSlotSelected: (String) -> Unit
) {
    val rows = slots.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowSlots ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEach { slot ->
                    TimeSlotItem(
                        slot = slot,
                        isSelected = selectedSlot == slot,
                        isBooked = bookedSlots.contains(slot), // Kiểm tra nếu giờ này đã có người đặt
                        isBusy = busySlots.contains(slot),     // Kiểm tra nếu bác sĩ đánh dấu bận
                        modifier = Modifier.weight(1f),
                        onClick = { onSlotSelected(slot) }
                    )
                }
                repeat(3 - rowSlots.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun TimeSlotItem(
    slot: String,
    isSelected: Boolean,
    isBooked: Boolean,
    isBusy: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isDisable = isBooked || isBusy
    val bgColor = when {
        isBooked -> Color(0xFFFFEBEE)
        isBusy -> Color(0xFFEEEEEE)
        isSelected -> Color(0xFF3B82F6)
        else -> Color.White
    }
    val textColor = when {
        isBooked -> Color.Red
        isBusy -> Color.Gray
        isSelected -> Color.White
        else -> Color.Black
    }

    Surface(
        modifier = modifier
            .height(50.dp)
            .clickable(enabled = !isDisable) { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if(isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(slot, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                if (isBooked) Text("Đã đặt", color = Color.Red, fontSize = 9.sp)
                else if (isBusy) Text("Bận", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

// Giữ nguyên DoctorSimpleInfo, DateSelectorItem, và OtherDateButton từ bản trước
@Composable
fun DoctorSimpleInfo(doctor: User) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).background(Color(0xFFEBF8FF), RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center
            ) { Text("👨‍⚕️", fontSize = 24.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(doctor.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E3A8A))
                Text(doctor.specialty, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun DateSelectorItem(item: DateItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color(0xFFEBF8FF) else Color.White
    val borderColor = if (isSelected) Color(0xFF3B82F6) else Color.LightGray
    val contentColor = if (isSelected) Color(0xFF3B82F6) else Color.Black

    Surface(
        modifier = Modifier.width(85.dp).height(65.dp).clickable { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(item.displayDate, fontWeight = FontWeight.Bold, color = contentColor)
            Text(item.displayDayOfWeek, fontSize = 11.sp, color = if(isSelected) contentColor else Color.Gray)
        }
    }
}

@Composable
fun OtherDateButton() {
    Surface(
        modifier = Modifier.width(85.dp).height(65.dp).clickable { },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Khác", fontSize = 11.sp, color = Color.Black)
        }
    }
}