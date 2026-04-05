package com.example.telemedicineapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.R

@Composable
fun DoctorItem(doctor: User, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            AsyncImage(
                model = doctor.imageUrl.ifEmpty { R.drawable.ic_doctor_placeholder },
                contentDescription = "Avatar bác sĩ",
                modifier = Modifier
                    .size(65.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // CẬP NHẬT: Nếu tên rỗng, lấy phần đầu của email hiển thị
                    val displayName = doctor.name.ifEmpty {
                        doctor.email.split("@").firstOrNull() ?: "Bác sĩ"
                    }
                    Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                val specialtyText = doctor.specialty.ifEmpty { "ĐA KHOA" }
                val hospitalText = doctor.hospitalName.ifEmpty { "Chưa cập nhật" }

                Text(text = specialtyText.uppercase(), color = Color(0xFF2563EB), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = "🏥 $hospitalText", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}