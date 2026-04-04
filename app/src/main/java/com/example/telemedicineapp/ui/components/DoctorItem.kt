package com.example.telemedicineapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.Doctor

@Composable
fun DoctorItem(doctor: Doctor, onClick: () -> Unit) {
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
            // Avatar Initial
            Box(
                modifier = Modifier.size(65.dp).clip(RoundedCornerShape(18.dp)).background(Color(doctor.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = doctor.name.split(" ").last().take(1),
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = doctor.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(text = "⭐ ${doctor.rating}", color = Color(0xFFFFB100), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(text = doctor.specialty.uppercase(), color = Color(0xFF2563EB), fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(text = "🏥 ${doctor.hospitalName}", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}