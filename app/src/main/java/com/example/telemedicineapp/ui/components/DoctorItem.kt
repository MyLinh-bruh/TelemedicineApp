package com.example.telemedicineapp.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.User

// --- 1. ITEM HIỂN THỊ BÁC SĨ THẬT ---
@Composable
fun DoctorItem(doctor: User, onClick: () -> Unit) {
    val themeColor = Color(0xFF1976D2) // Đã chuyển sang màu xanh dương

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
            // Avatar (Giải mã Base64 hoặc hiện chữ cái đầu)
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(themeColor),
                contentAlignment = Alignment.Center
            ) {
                if (doctor.imageUrl.isNotBlank()) {
                    val bitmap = remember(doctor.imageUrl) {
                        try {
                            val cleanBase64 = if (doctor.imageUrl.contains(",")) {
                                doctor.imageUrl.substringAfter(",")
                            } else {
                                doctor.imageUrl
                            }
                            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Avatar của ${doctor.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = doctor.name.trim().split(" ").lastOrNull()?.take(1)?.uppercase() ?: "BS"
                        Text(text = initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    }
                } else {
                    val initial = doctor.name.trim().split(" ").lastOrNull()?.take(1)?.uppercase() ?: "BS"
                    Text(text = initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val displayName = doctor.name.ifEmpty { "Bác sĩ chưa cập nhật tên" }
                    Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Text(text = "⭐ 5.0", color = Color(0xFFFFB100), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                val specialtyText = doctor.specialty.ifEmpty { "ĐA KHOA" }
                Text(text = specialtyText.uppercase(), color = themeColor, fontSize = 10.sp, fontWeight = FontWeight.Black)

                val hospitalText = doctor.hospitalName.ifEmpty { "Chưa cập nhật bệnh viện" }
                Text(text = "🏥 $hospitalText", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

// --- 2. HIỆU ỨNG LOADING (SHIMMER) BỊ MẤT ---
@Composable
fun DoctorShimmer() {
    // Cấu hình màu sắc nhấp nháy cho Shimmer
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    // Cấu hình hiệu ứng chuyển động
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    // Hiển thị một danh sách gồm 5 item loading giả
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(5) {
            ShimmerItem(brush = brush)
        }
    }
}

@Composable
fun ShimmerItem(brush: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Ô vuông giả lập Avatar
            Box(
                modifier = Modifier
                    .size(65.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(brush)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Thanh giả lập Tên bác sĩ
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Thanh giả lập Chuyên khoa
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Thanh giả lập Tên Bệnh viện
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .background(brush, RoundedCornerShape(4.dp))
                )
            }
        }
    }
}