package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.ui.components.DoctorItem
import com.example.telemedicineapp.ui.components.DoctorShimmer
import java.text.Normalizer

// --- HÀM CHUẨN HÓA CHUỖI (Giúp lọc không dấu, không phân biệt hoa thường) ---
fun String.toCleanString(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    return pattern.replace(temp, "")
        .replace("đ", "d")
        .replace("Đ", "D")
        .trim()
        .lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorListScreen(
    onDoctorClick: (User) -> Unit,
    onLogout: () -> Unit,
    allDoctors: List<User>,
    onRegisterDoctorClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onMedicalRecordsClick: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSpecialty by remember { mutableStateOf("Tất cả chuyên khoa") }
    var selectedLocation by remember { mutableStateOf("Tất cả khu vực") }

    val specialties = listOf("Tất cả chuyên khoa", "Tim mạch", "Nhi khoa", "Da liễu", "Nội khoa")
    val locations = listOf("Tất cả khu vực", "Đà Nẵng", "Hà Nội", "TP. Hồ Chí Minh")

    // --- LOGIC LỌC TỐI ƯU GỘP TỪ CODE 2 ---
    val filteredDoctors = remember(searchQuery, selectedSpecialty, selectedLocation, allDoctors) {
        allDoctors.filter { doctor ->
            val docNameClean = doctor.name.toCleanString()
            val docSpecClean = doctor.specialty.toCleanString()
            val docAddrClean = doctor.address.toCleanString()

            val searchClean = searchQuery.toCleanString()
            val specFilterClean = selectedSpecialty.toCleanString()
            val locFilterClean = selectedLocation.toCleanString()

            // 1. Tìm kiếm theo tên (Tìm chuỗi con)
            val matchesSearch = searchClean.isEmpty() || docNameClean.contains(searchClean)

            // 2. Lọc chuyên khoa (Tách từ để khớp thông minh: "Nhi khoa" khớp "Khoa nhi")
            val filterWords = specFilterClean.split(" ").filter { it.isNotBlank() && it != "tat" && it != "ca" }
            val matchesSpecialty = selectedSpecialty == "Tất cả chuyên khoa" ||
                    filterWords.all { word -> docSpecClean.contains(word) }

            // 3. Lọc địa điểm
            val matchesLocation = selectedLocation == "Tất cả khu vực" ||
                    docAddrClean.contains(locFilterClean)

            matchesSearch && matchesSpecialty && matchesLocation
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // --- HEADER ---
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
                    Text("Khám Phá", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                    Text("Tìm chuyên gia y tế phù hợp", fontSize = 12.sp, color = Color.Gray)
                }

                Box {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.Black)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hồ sơ bệnh nhân", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp)) },
                            onClick = { showMenu = false; onProfileClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Lịch hẹn của tôi", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Event, null, modifier = Modifier.size(20.dp)) },
                            onClick = { showMenu = false; onHistoryClick() }
                        )
                        DropdownMenuItem(
                            text = { Text("Hồ sơ bệnh án", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Description, null, modifier = Modifier.size(20.dp)) },
                            onClick = {
                                showMenu = false
                                // Thêm tham số callback onMedicalRecordsClick vào hàm DoctorListScreen
                                onMedicalRecordsClick()
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
                        DropdownMenuItem(
                            text = { Text("Đăng xuất", fontSize = 14.sp, color = Color.Red) },
                            leadingIcon = { Icon(Icons.Default.ExitToApp, null, tint = Color.Red, modifier = Modifier.size(20.dp)) },
                            onClick = { showMenu = false; onLogout() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ô TÌM KIẾM
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập tên bác sĩ...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F5F9),
                    unfocusedContainerColor = Color(0xFFF1F5F9),
                    focusedBorderColor = Color(0xFF2563EB).copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // BỘ LỌC
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Chuyên khoa",
                    options = specialties,
                    selectedOption = selectedSpecialty,
                    onOptionSelected = { selectedSpecialty = it },
                    modifier = Modifier.weight(1f)
                )
                FilterDropdown(
                    label = "Địa điểm",
                    options = locations,
                    selectedOption = selectedLocation,
                    onOptionSelected = { selectedLocation = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // --- DANH SÁCH BÁC SĨ ---
        if (allDoctors.isEmpty()) {
            Column(Modifier.padding(16.dp)) {
                repeat(5) { DoctorShimmer() }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredDoctors) { doctor ->
                    DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                }
            }
        }
    }
}

// --- HÀM DROPDOWN PHỤ (Sửa lỗi 'it' và tích hợp vào 1 file) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 10.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF1F5F9),
                unfocusedContainerColor = Color(0xFFF1F5F9),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 13.sp, color = Color.Black) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}