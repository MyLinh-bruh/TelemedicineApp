package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.User

@Composable
fun DoctorDetailScreen(
    doctor: User,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit // 👈 THÊM MỚI Ở ĐÂY: Callback để chuyển trang
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "DOCTOR",
                    color = Color.White.copy(alpha = 0.1f),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 40.dp, start = 16.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                Box(
                    modifier = Modifier
                        .offset(y = (-50).dp)
                        .size(100.dp)
                        .clip(RoundedCornerShape(35.dp))
                        .background(Color.White)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            doctor.name.split(" ").last().take(1),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            doctor.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            doctor.specialty.uppercase(),
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(vertical = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatBox("Bệnh nhân", "1.2K+", Modifier.weight(1f))
                    StatBox("Kinh nghiệm", "10 Năm", Modifier.weight(1f))
                    StatBox("Phí khám", "300K", Modifier.weight(1f), Color(0xFF2563EB))
                }

                Text("🏥 ${doctor.hospitalName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(doctor.address, color = Color.Gray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(24.dp))
                Text("Thông tin giới thiệu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    doctor.description,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 20.sp
                )

                if (doctor.bankAccountNumber.isNotEmpty() && doctor.bankName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Thông tin thanh toán", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Ngân hàng: ${doctor.bankName}",
                                color = Color.DarkGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Số tài khoản: ${doctor.bankAccountNumber}",
                                color = Color.DarkGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        }

        Button(
            onClick = { onBookClick(doctor.id) }, // 👈 THÊM MỚI Ở ĐÂY: Truyền ID bác sĩ sang Navigation
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
        ) {
            Text("Đặt lịch ngay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = Color(0xFF334155)
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label.uppercase(),
            fontSize = 9.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}