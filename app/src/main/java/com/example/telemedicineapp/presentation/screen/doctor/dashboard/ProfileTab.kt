package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileTab(
    viewModel: DoctorDashboardViewModel,
    onEditProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onPatientClick: (String, String) -> Unit
) {
    val profile by viewModel.doctorProfile.collectAsState(initial = null)
    val records by viewModel.patientRecords.collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }

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
        // 🌟 ĐÃ THAY ĐỔI: Đưa Đăng xuất lên cạnh tiêu đề
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Hồ sơ & Quản lý",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Đăng xuất", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

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

                    // HIỂN THỊ AVATAR BẰNG BASE64
                    Base64Image(
                        base64String = profile?.imageUrl ?: "",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    )

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

        Text(
            text = "Hồ sơ bệnh nhân đã khám",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E3A8A)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Tìm theo tên hoặc chẩn đoán...", fontSize = 14.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (records.isEmpty()) {
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
                    textAlign = TextAlign.Center
                )
            }
        } else {
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
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = record.patientName, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                            Text(text = "Chẩn đoán: ${record.diagnosis}", fontSize = 13.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(text = record.lastUpdated.split(" ")[0], fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        // 🌟 ĐÃ XÓA NÚT ĐĂNG XUẤT Ở ĐÂY VÌ ĐÃ CHUYỂN LÊN ĐẦU
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun Base64Image(base64String: String, modifier: Modifier = Modifier) {
    if (base64String.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
        }
        return
    }

    val decodedBitmap = remember(base64String) {
        try {
            val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) { null }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = "Avatar",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp), tint = Color.Gray)
        }
    }
}