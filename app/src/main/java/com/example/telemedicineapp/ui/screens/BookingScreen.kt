package com.example.telemedicineapp.ui.screens

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.presentation.screen.appointment.AppointmentViewModel
import com.example.telemedicineapp.presentation.screen.appointment.BookingState
import com.example.telemedicineapp.presentation.screen.auth.ProfileViewModel
import com.example.telemedicineapp.ui.components.PaymentQRDialog
import com.example.telemedicineapp.utils.DateItem
import com.example.telemedicineapp.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    doctor: User,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    appointmentViewModel: AppointmentViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Tự động tải thông tin bệnh nhân để kiểm tra độ hoàn thiện
    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }
    val patient = profileViewModel.userState

    // Biến xác định bệnh nhân có thiếu thông tin không
    val isProfileIncomplete = patient != null && (patient.name.isBlank() || patient.phone.isBlank() || patient.address.isBlank())

    val bookingState by appointmentViewModel.bookingState.collectAsState()
    val schedule by appointmentViewModel.doctorSchedule.collectAsState()
    val bookedSlots by appointmentViewModel.bookedSlots.collectAsState()

    val availableDates = remember { TimeUtils.getNext7Days() }
    var selectedDate by remember { mutableStateOf(availableDates.first()) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }

    var selectedPaymentMethod by remember { mutableStateOf("STRIPE") }
    var showConflictDialog by remember { mutableStateOf(false) }
    var showPaymentWebView by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        appointmentViewModel.getSchedulesAndAppointments(doctor.id, selectedDate.fullDate)
    }

    LaunchedEffect(bookingState) {
        when (bookingState) {
            is BookingState.Success -> {
                if (selectedPaymentMethod == "STRIPE") showPaymentWebView = true else showQRDialog = true
            }
            is BookingState.Conflict -> showConflictDialog = true
            is BookingState.Error -> {
                Toast.makeText(context, (bookingState as BookingState.Error).message, Toast.LENGTH_SHORT).show()
                appointmentViewModel.resetState()
            }
            else -> {}
        }
    }

    // Hiển thị vòng xoay nếu dữ liệu bệnh nhân đang được lấy về
    if (profileViewModel.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    if (patient == null) return

    // DIALOG CẢNH BÁO BẮT BUỘC ĐIỀN THÔNG TIN
    if (isProfileIncomplete) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(Modifier.width(8.dp))
                    Text("Thiếu thông tin", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("Bạn cần cập nhật đầy đủ Họ tên, Số điện thoại và Địa chỉ trong Hồ sơ cá nhân trước khi tiến hành đặt lịch khám.") },
            confirmButton = {
                Button(
                    onClick = onNavigateToProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Cập nhật ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text("Quay lại", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showPaymentWebView) {
        PaymentWebViewContent(
            url = "https://buy.stripe.com/test_8x2aEQ7K566kaQ7gLs3sI00",
            onSuccess = {
                showPaymentWebView = false
                appointmentViewModel.confirmAppointmentStatus("PAID")
                showSuccessDialog = true
            },
            onCancel = {
                showPaymentWebView = false
                appointmentViewModel.resetState()
                onBack()
            }
        )
    } else {
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

            if (showSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Đặt lịch thành công!", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    },
                    text = {
                        Text(
                            "Hệ thống đã ghi nhận lịch hẹn của bạn. Bạn có thể kiểm tra chi tiết trong mục 'Lịch hẹn'.",
                            textAlign = TextAlign.Center, fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                appointmentViewModel.resetState()
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Xác nhận", fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (showQRDialog) {
                PaymentQRDialog(
                    amount = "150000",
                    appointmentId = "MED-${System.currentTimeMillis() % 10000}",
                    onConfirm = {
                        showQRDialog = false
                        appointmentViewModel.confirmAppointmentStatus("PAID")
                        showSuccessDialog = true
                    },
                    onDismiss = {
                        showQRDialog = false
                        appointmentViewModel.resetState()
                        onBack()
                        Toast.makeText(context, "Lịch hẹn đã được lưu vào Lịch sử chờ thanh toán", Toast.LENGTH_LONG).show()
                    }
                )
            }

            if (showConflictDialog) {
                AlertDialog(
                    onDismissRequest = { showConflictDialog = false },
                    title = { Text("Trùng lịch hẹn", fontWeight = FontWeight.Bold) },
                    text = { Text("Rất tiếc, khung giờ này vừa có người đặt. Vui lòng chọn khung giờ khác.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showConflictDialog = false
                            appointmentViewModel.resetState()
                            appointmentViewModel.getSchedulesAndAppointments(doctor.id, selectedDate.fullDate)
                        }) { Text("Đồng ý") }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF4F6F9))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                DoctorSimpleInfo(doctor)

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ngày khám *", fontWeight = FontWeight.Bold)
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
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Giờ khám *", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Buổi sáng", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                    TimeSlotGrid(
                        slots = schedule?.morningSlots ?: emptyList(),
                        selectedSlot = selectedTimeSlot,
                        bookedSlots = bookedSlots,
                        busySlots = schedule?.busySlots ?: emptyList()
                    ) { selectedTimeSlot = it }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Buổi chiều", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 14.sp)
                    TimeSlotGrid(
                        slots = schedule?.afternoonSlots ?: emptyList(),
                        selectedSlot = selectedTimeSlot,
                        bookedSlots = bookedSlots,
                        busySlots = schedule?.busySlots ?: emptyList()
                    ) { selectedTimeSlot = it }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Phương thức thanh toán *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    PaymentMethodOption(
                        title = "Thẻ Quốc tế (Stripe)",
                        isSelected = selectedPaymentMethod == "STRIPE",
                        onClick = { selectedPaymentMethod = "STRIPE" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentMethodOption(
                        title = "Quét mã QR (VietQR / MoMo)",
                        isSelected = selectedPaymentMethod == "QR",
                        onClick = { selectedPaymentMethod = "QR" }
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
                        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White)
                    )
                }

                Button(
                    onClick = {
                        if (selectedTimeSlot != null && !isProfileIncomplete) {
                            val startTime = selectedTimeSlot!!.split(" - ")[0]
                            val utcDateTime = TimeUtils.convertLocalToUtcString(selectedDate.fullDate, startTime)
                            val displayName = if (patient.name.isBlank()) "Bệnh nhân ẩn danh" else patient.name

                            if (utcDateTime != null) {
                                val newAppointment = Appointment(
                                    patientId = patient.email,
                                    patientName = displayName,
                                    doctorId = doctor.id,
                                    doctorName = doctor.name,
                                    dateTimeUtc = utcDateTime,
                                    reason = reason,
                                    status = "PENDING",
                                    createdAt = System.currentTimeMillis() // THÊM THỜI GIAN TẠO
                                )
                                appointmentViewModel.bookAppointment(newAppointment)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (selectedTimeSlot != null && !isProfileIncomplete) Color(0xFF2563EB) else Color.LightGray),
                    enabled = selectedTimeSlot != null && bookingState !is BookingState.Loading && !isProfileIncomplete
                ) {
                    Text("Xác nhận & Thanh toán", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PaymentMethodOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFEBF8FF) else Color.White,
        border = BorderStroke(2.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE5E7EB))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.DateRange,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2563EB) else Color.Gray
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun TimeSlotGrid(slots: List<String>, selectedSlot: String?, bookedSlots: List<String>, busySlots: List<String>, onSlotSelected: (String) -> Unit) {
    val rows = slots.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowSlots ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSlots.forEach { slot ->
                    TimeSlotItem(
                        slot = slot,
                        isSelected = selectedSlot == slot,
                        isBooked = bookedSlots.contains(slot),
                        isBusy = busySlots.contains(slot),
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
fun TimeSlotItem(slot: String, isSelected: Boolean, isBooked: Boolean, isBusy: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val isDisable = isBooked || isBusy
    val bgColor = when {
        isBooked -> Color(0xFFFEE2E2)
        isBusy -> Color(0xFFEEEEEE)
        isSelected -> Color(0xFF3B82F6)
        else -> Color.White
    }
    val textColor = when {
        isBooked -> Color(0xFFDC2626)
        isBusy -> Color.Gray
        isSelected -> Color.White
        else -> Color.Black
    }

    Surface(
        modifier = modifier.height(55.dp).clickable(enabled = !isDisable) { onClick() },
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = slot, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (isBooked) Text("Đã đặt", color = Color(0xFFDC2626), fontSize = 9.sp)
                else if (isBusy) Text("Bận", color = Color.Gray, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun DoctorSimpleInfo(doctor: User) {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(Color(0xFFEBF8FF), CircleShape), contentAlignment = Alignment.Center) { Text("👨‍⚕️", fontSize = 24.sp) }
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
    Surface(
        modifier = Modifier.width(85.dp).height(65.dp).clickable { onClick() },
        color = if (isSelected) Color(0xFFEBF8FF) else Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color.LightGray)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(item.displayDate, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF3B82F6) else Color.Black)
            Text(item.displayDayOfWeek, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PaymentWebViewContent(url: String, onSuccess: () -> Unit, onCancel: () -> Unit) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView?, r: WebResourceRequest?): Boolean {
                        val u = r?.url.toString()
                        if (u.contains("success")) { onSuccess(); return true }
                        if (u.contains("cancel")) { onCancel(); return true }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}