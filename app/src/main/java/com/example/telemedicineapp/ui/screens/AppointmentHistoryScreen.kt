package com.example.telemedicineapp.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.presentation.screen.appointment.AppointmentHistoryViewModel
import com.example.telemedicineapp.ui.components.PaymentQRDialog
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentHistoryScreen(
    onBack: () -> Unit,
    // 🌟 ĐÃ SỬA: Đổi tên thành onViewRecordClick và nhận vào đối tượng Appointment cho khớp với MainActivity
    onViewRecordClick: (Appointment) -> Unit = {},
    viewModel: AppointmentHistoryViewModel = hiltViewModel()
) {
    val appointments by viewModel.appointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sắp tới", "Đã khám", "Đã hủy")

    var searchQuery by remember { mutableStateOf("") }
    var isAscending by remember { mutableStateOf(true) }
    var appointmentToCancel by remember { mutableStateOf<Appointment?>(null) }

    var appointmentToPay by remember { mutableStateOf<Appointment?>(null) }
    var showPaymentChoiceDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf("STRIPE") }
    var showPaymentWebView by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }

    val filteredList = appointments.filter { appt ->
        val matchTab = when (selectedTab) {
            0 -> appt.status == "PENDING" || appt.status == "PAID"
            1 -> appt.status == "COMPLETED"
            2 -> appt.status == "CANCELLED"
            else -> true
        }

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

    if (showPaymentWebView && appointmentToPay != null) {
        PaymentWebViewContent(
            url = "https://buy.stripe.com/test_8x2aEQ7K566kaQ7gLs3sI00",
            onSuccess = {
                showPaymentWebView = false
                viewModel.confirmPayment(appointmentToPay!!.id)
                appointmentToPay = null
            },
            onCancel = {
                showPaymentWebView = false
            }
        )
        return
    }

    if (showQRDialog && appointmentToPay != null) {
        PaymentQRDialog(
            amount = "150000",
            appointmentId = appointmentToPay!!.id,
            onConfirm = {
                showQRDialog = false
                viewModel.confirmPayment(appointmentToPay!!.id)
                appointmentToPay = null
            },
            onDismiss = {
                showQRDialog = false
            },
            onCancelTransaction = {
                showQRDialog = false
                viewModel.deleteAppointment(appointmentToPay!!.id)
                appointmentToPay = null
            }
        )
    }

    if (showPaymentChoiceDialog && appointmentToPay != null) {
        AlertDialog(
            onDismissRequest = { showPaymentChoiceDialog = false },
            title = { Text("Chọn phương thức", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    PaymentMethodOption("Thẻ Quốc tế (Stripe)", selectedPaymentMethod == "STRIPE") { selectedPaymentMethod = "STRIPE" }
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentMethodOption("Quét mã QR", selectedPaymentMethod == "QR") { selectedPaymentMethod = "QR" }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentChoiceDialog = false
                        if (selectedPaymentMethod == "STRIPE") showPaymentWebView = true else showQRDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) { Text("Thanh toán", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentChoiceDialog = false }) { Text("Hủy") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (appointmentToCancel != null) {
        val isPending = appointmentToCancel?.status == "PENDING"
        AlertDialog(
            onDismissRequest = { appointmentToCancel = null },
            title = { Text(if (isPending) "Xác nhận hủy thanh toán" else "Xác nhận hủy lịch", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isPending) "Bạn có chắc chắn muốn hủy? Lịch hẹn chờ thanh toán này sẽ bị xóa hoàn toàn khỏi hệ thống."
                    else "Bạn có chắc chắn muốn hủy lịch hẹn với ${appointmentToCancel?.doctorName} không? Hành động này không thể hoàn tác."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isPending) {
                            viewModel.deleteAppointment(appointmentToCancel!!.id)
                        } else {
                            viewModel.cancelAppointment(appointmentToCancel!!.id)
                        }
                        appointmentToCancel = null
                    }
                ) {
                    Text(if (isPending) "Đồng ý xóa" else "Đồng ý hủy", color = Color.Red, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.weight(1f).height(50.dp),
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
                    modifier = Modifier.size(45.dp).background(Color(0xFFEBF8FF), RoundedCornerShape(12.dp))
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
                            onCancelClick = { appointmentToCancel = appt },
                            onPayClick = {
                                appointmentToPay = appt
                                showPaymentChoiceDialog = true
                            },
                            onTimeout = {
                                viewModel.deleteAppointment(appt.id)
                            },
                            onViewRecordClick = {
                                // 🌟 TRUYỀN TOÀN BỘ ĐỐI TƯỢNG APPT ĐỂ MAINACTIVITY LẤY DATA
                                onViewRecordClick(appt)
                            }
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
    onCancelClick: () -> Unit,
    onPayClick: () -> Unit,
    onTimeout: () -> Unit,
    onViewRecordClick: () -> Unit = {}
) {
    // 🌟 LOGIC GIẢI MÃ ẢNH BÁC SĨ TỪ BASE64
    val avatarBitmap = remember(appointment.doctorImageUrl) {
        if (appointment.doctorImageUrl.isNotBlank()) {
            try {
                val cleanBase64 = if (appointment.doctorImageUrl.contains(",")) {
                    appointment.doctorImageUrl.substringAfter(",")
                } else {
                    appointment.doctorImageUrl
                }
                val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) { null }
        } else null
    }

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
                // 🌟 HIỂN THỊ AVATAR BÁC SĨ HOẶC ICON MẶC ĐỊNH
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Avatar Bác sĩ",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFEBF8FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("👨‍⚕️", fontSize = 20.sp) }
                }

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

            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 CÁC NÚT BẤM DỰA VÀO TRẠNG THÁI LỊCH HẸN
            if (appointment.status == "PENDING") {
                PendingPaymentTimer(
                    createdAt = appointment.createdAt,
                    onTimeout = onTimeout,
                    onPayClick = onPayClick,
                    onCancelClick = onCancelClick
                )
            } else if (appointment.status == "PAID") {
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
            } else if (appointment.status == "COMPLETED") {
                // 🌟 NÚT XEM BỆNH ÁN HIỂN THỊ KHI ĐÃ KHÁM XONG
                Button(
                    onClick = onViewRecordClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981), // Màu xanh lá
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Xem hồ sơ bệnh án", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun PendingPaymentTimer(createdAt: Long, onTimeout: () -> Unit, onPayClick: () -> Unit, onCancelClick: () -> Unit) {
    var timeLeft by remember(createdAt) {
        mutableLongStateOf((10 * 60) - ((System.currentTimeMillis() - createdAt) / 1000))
    }

    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000L)
            timeLeft = (10 * 60) - ((System.currentTimeMillis() - createdAt) / 1000)
        } else {
            onTimeout()
        }
    }

    if (timeLeft > 0) {
        val mins = timeLeft / 60
        val secs = timeLeft % 60
        val timeString = String.format("%02d:%02d", mins, secs)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.weight(1.2f).height(45.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = BorderStroke(1.dp, Color.Red),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Hủy thanh toán", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPayClick,
                modifier = Modifier.weight(1.8f).height(45.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Thanh toán ($timeString)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    } else {
        Text("Đang hủy giao dịch...", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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