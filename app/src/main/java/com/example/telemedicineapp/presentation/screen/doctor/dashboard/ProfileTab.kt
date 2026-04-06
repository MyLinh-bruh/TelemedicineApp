package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.User
import java.text.Normalizer

// Hàm hỗ trợ tìm kiếm tiếng Việt không dấu
fun String.toSearchableString(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return pattern.replace(temp, "").replace("đ", "d").replace("Đ", "D").lowercase().trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTab(
    viewModel: DoctorDashboardViewModel,
    onEditProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onPatientClick: (String, String) -> Unit
) {
    val doctor by viewModel.doctorProfile.collectAsState()
    val patientRecords by viewModel.patientRecords.collectAsState() // 🌟 Lấy danh sách bệnh án

    var searchQuery by remember { mutableStateOf("") }

    if (doctor == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = doctor!!

    // 🌟 Lọc danh sách bệnh án theo từ khóa tìm kiếm
    val filteredRecords = remember(searchQuery, patientRecords) {
        val queryClean = searchQuery.toSearchableString()
        if (queryClean.isEmpty()) {
            patientRecords
        } else {
            patientRecords.filter { record ->
                record.patientName.toSearchableString().contains(queryClean) ||
                        record.currentSymptoms.toSearchableString().contains(queryClean) ||
                        record.diagnosis.toSearchableString().contains(queryClean)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. HEADER MÀU XANH & AVATAR ---
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFF2563EB))
            ) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Đăng xuất", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp)
                    .size(100.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2563EB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- 2. THÔNG TIN CƠ BẢN ---
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Text(text = user.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Text(
                text = user.specialty.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2563EB),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 3. THỐNG KÊ ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard("BỆNH NHÂN", "${patientRecords.size}") // Hiển thị số bệnh nhân thực tế
                StatCard("KINH NGHIỆM", "10 Năm")
                StatCard("PHÍ KHÁM", "300K", valueColor = Color(0xFF2563EB))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 4. BỆNH VIỆN & ĐỊA CHỈ ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = user.hospitalName.ifBlank { "Chưa cập nhật bệnh viện" }, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text(
                text = user.address.ifBlank { "Chưa cập nhật địa chỉ phòng khám" },
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 28.dp, top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 5. GIỚI THIỆU ---
            Text(text = "Thông tin giới thiệu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = user.description.ifBlank { "Bạn chưa cập nhật thông tin giới thiệu bản thân." },
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- 6. NÚT CHỈNH SỬA ---
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(thickness = 4.dp, color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(24.dp))

            // ==========================================
            // 🌟 7. PHẦN QUẢN LÝ LỊCH SỬ BỆNH ÁN
            // ==========================================
            Text(text = "Lịch sử bệnh án", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))

            // Ô Tìm kiếm
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm theo tên, triệu chứng, chẩn đoán...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Danh sách bệnh án (Dùng forEach thay vì LazyColumn để tránh lỗi lồng Scroll)
            if (filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy bệnh án nào", color = Color.Gray)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    filteredRecords.forEach { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val pName = if (record.patientName.isBlank()) "Ẩn danh" else record.patientName
                                    onPatientClick(record.patientId, pName) // Bấm vào để xem/sửa chi tiết
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (record.patientName.isBlank()) "Bệnh nhân ẩn danh" else record.patientName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(text = record.lastUpdated.split(" ")[0], fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(text = "Triệu chứng: ${record.currentSymptoms}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Text(text = "Chẩn đoán: ${record.diagnosis}", fontSize = 13.sp, color = Color(0xFF2563EB), maxLines = 1, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(title: String, value: String, valueColor: Color = Color.Black) {
    Column(
        modifier = Modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 16.sp, color = valueColor, fontWeight = FontWeight.ExtraBold)
    }
}