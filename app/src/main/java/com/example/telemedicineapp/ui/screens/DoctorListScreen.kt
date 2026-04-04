package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.DoctorStatus
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.ui.components.DoctorItem
import com.example.telemedicineapp.ui.components.DoctorShimmer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorListScreen(
    onDoctorClick: (User) -> Unit,
    onLogout: () -> Unit,
    allDoctors: List<User>
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Tất cả") }
    var isLoading by remember { mutableStateOf(true) }

    val filters = listOf("Tất cả", "Tim mạch", "Nhi khoa", "Da liễu")

    // Dữ liệu giả định được cập nhật theo User class
    val fakeDoctors = remember {
        listOf(
            User(id = "1", name = "BS. Lê Mạnh Hùng", role = Role.DOCTOR, doctorStatus = DoctorStatus.APPROVED, specialty = "Tim mạch", rating = 4.9, description = "Chuyên gia tim mạch hàng đầu...", address = "123 Hải Phòng, ĐN", hospitalName = "BV Đa khoa Đà Nẵng", colorHex = 0xFF3B82F6L, bankAccountNumber = "0123456789", bankName = "Vietcombank"),
            User(id = "2", name = "BS. Phan Mỹ Linh", role = Role.DOCTOR, doctorStatus = DoctorStatus.APPROVED, specialty = "Nhi khoa", rating = 4.8, description = "Hơn 15 năm kinh nghiệm...", address = "402 Lê Văn Hiến, ĐN", hospitalName = "BV Phụ sản Nhi", colorHex = 0xFFF43F5EL, bankAccountNumber = "9876543210", bankName = "MB Bank"),
            User(id = "3", name = "BS. Nguyễn Nam", role = Role.DOCTOR, doctorStatus = DoctorStatus.APPROVED, specialty = "Da liễu", rating = 4.7, description = "Chuyên gia Laser CO2...", address = "15 Nguyễn Văn Linh, ĐN", hospitalName = "Phòng khám Da liễu Pro", colorHex = 0xFF10B981L, bankAccountNumber = "1903827465", bankName = "Techcombank")
        )
    }

    LaunchedEffect(Unit) {
        delay(1000)
        isLoading = false
    }

    val filteredDoctors = fakeDoctors.filter {
        (selectedFilter == "Tất cả" || it.specialty == selectedFilter) &&
                it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(top = 48.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Khám Phá", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Tìm chuyên gia y tế phù hợp", fontSize = 12.sp, color = Color.Gray)
                }

                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tên bác sĩ", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9), unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = Color(0xFF2563EB).copy(alpha = 0.5f), unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB), selectedLabelColor = Color.White
                        ),
                        border = null
                    )
                }
            }
        }

        if (isLoading) {
            DoctorShimmer()
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filteredDoctors) { doctor ->
                    DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                }
            }
        }
    }
}