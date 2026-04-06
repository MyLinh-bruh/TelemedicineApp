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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun ProfileTab(
    viewModel: DoctorDashboardViewModel,
    onEditProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onPatientClick: (String, String) -> Unit
) {
    val profile by viewModel.doctorProfile.collectAsState(initial = null)
    val records by viewModel.patientRecords.collectAsState(initial = emptyList())

    // 🌟 THÊM MỚI: State lưu từ khóa tìm kiếm
    var searchQuery by remember { mutableStateOf("") }

    // 🌟 THÊM MỚI: Tự động lọc danh sách dựa trên từ khóa (Tìm theo Tên hoặc Chẩn đoán)
    val filteredRecords = records.filter {
        it.patientName.contains(searchQuery, ignoreCase = true) ||
                it.diagnosis.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Hồ sơ & Quản lý",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(Modifier.height(20.dp))

        // --- PHẦN 1: CARD THÔNG TIN BÁC SĨ ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfileClick() },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val imageUrl = profile?.imageUrl ?: ""
                    if (imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
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
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.LightGray
                    )
                }

                val description = profile?.description ?: ""
                if (description.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Giới thiệu:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                    Text(
                        text = description,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Chạm để xem và sửa hồ sơ",
                    color = Color(0xFF2563EB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- PHẦN 2: DANH SÁCH BỆNH NHÂN (CÓ TÌM KIẾM) ---
        Text(
            text = "Hồ sơ bệnh nhân đã khám",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A)
        )
        Spacer(Modifier.height(12.dp))

        // 🌟 THÊM MỚI: Thanh tìm kiếm (Search Bar)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm theo tên hoặc chẩn đoán...", fontSize = 14.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color.Transparent
            )
        )

        // 🌟 HIỂN THỊ DANH SÁCH SAU KHI LỌC
        if (records.isEmpty()) {
            // Trường hợp chưa từng khám ai
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Chưa có hồ sơ nào được lưu.",
                    color = Color.Gray,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (filteredRecords.isEmpty()) {
            // Trường hợp tìm kiếm không ra kết quả
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Không tìm thấy hồ sơ phù hợp.",
                    color = Color(0xFFDC2626),
                    modifier = Modifier.padding(20.dp),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            // Hiển thị các hồ sơ khớp từ khóa
            filteredRecords.forEach { record ->
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = record.lastUpdated.split(" ")[0],
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

        Spacer(Modifier.height(60.dp))
    }
}