package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileTab(
    viewModel: DoctorDashboardViewModel,
    onEditProfileClick: () -> Unit, // Mở màn hình EditDoctorProfileScreen
    onLogout: () -> Unit,
    onPatientClick: (String, String) -> Unit // Xem lại hồ sơ bệnh án chi tiết
) {
    // Thu thập dữ liệu từ ViewModel
    val profile by viewModel.doctorProfile.collectAsState(initial = null)
    val records by viewModel.patientRecords.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Tiêu đề chính
        Text(
            text = "Hồ sơ & Quản lý",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(Modifier.height(20.dp))

        // --- PHẦN 1: CARD THÔNG TIN BÁC SĨ (Dạng rút gọn) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfileClick() }, // 🌟 Chạm vào để mở trang chỉnh sửa tất cả thông tin
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar giả lập
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color.Gray
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.name ?: "Đang tải...",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = profile?.specialty ?: "Chuyên khoa chưa cập nhật",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Chạm để xem và sửa hồ sơ",
                        color = Color(0xFF2563EB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.LightGray
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- PHẦN 2: DANH SÁCH HỒ SƠ BỆNH NHÂN ĐÃ KHÁM ---
        Text(
            text = "Hồ sơ bệnh nhân đã khám",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A)
        )
        Spacer(Modifier.height(12.dp))

        if (records.isEmpty()) {
            // Trạng thái trống
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Chưa có hồ sơ nào được lưu.",
                    color = Color.Gray,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 14.sp
                )
            }
        } else {
            // Danh sách các bệnh nhân
            records.forEach { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onPatientClick(record.patientId, record.patientName) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.patientName,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Chẩn đoán: ${record.diagnosis}",
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = record.lastUpdated.split(" ")[0], // Chỉ lấy ngày, bỏ giờ
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- PHẦN 3: NÚT ĐĂNG XUẤT ---
        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
        ) {
            Text("Đăng xuất", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(60.dp)) // Padding dưới cùng để không bị lấp bởi BottomBar
    }
}