package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.telemedicineapp.ui.components.DoctorItem

data class PendingRequest(
    val id: String,
    val name: String,
    val specialty: String,
    val experience: String,
    val hospital: String,
    val phone: String,
    val certId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    allDoctors: List<User>, // Đổi Doctor thành User
    onDoctorClick: (User) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRequest by remember { mutableStateOf<PendingRequest?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("Toàn quốc") }
    var selectedSpecialty by remember { mutableStateOf("Tất cả") }
    var expandedLocation by remember { mutableStateOf(false) }
    var expandedSpecialty by remember { mutableStateOf(false) }

    val locations = listOf("Toàn quốc", "Hà Nội", "Đà Nẵng", "TP. HCM")
    val specialties = listOf("Tất cả", "Tim mạch", "Nhi khoa", "Da liễu")

    val requests = remember {
        listOf(
            PendingRequest("1", "BS. Nguyễn Văn An", "Tim mạch", "10 năm", "BV Bạch Mai", "0901234567", "CCHN-123"),
            PendingRequest("2", "BS. Lê Thị Bình", "Nhi khoa", "8 năm", "BV Nhi TW", "0987654321", "CCHN-456")
        )
    }

    val filteredDoctors = allDoctors.filter { doctor ->
        val matchLoc = selectedLocation == "Toàn quốc" || doctor.address.contains(selectedLocation)
        val matchSpec = selectedSpecialty == "Tất cả" || doctor.specialty == selectedSpecialty
        val matchName = doctor.name.contains(searchQuery, ignoreCase = true)
        matchLoc && matchSpec && matchName
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Hệ thống") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Duyệt đơn") })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        Text("Quản lý danh sách bác sĩ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tìm tên bác sĩ...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
                            singleLine = true
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminFilterBox(Modifier.weight(1f), "Khu vực", selectedLocation) { expandedLocation = true }
                            AdminFilterBox(Modifier.weight(1f), "Chuyên khoa", selectedSpecialty) { expandedSpecialty = true }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    items(filteredDoctors) { doctor ->
                        DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                ApprovalListView(requests) { selectedRequest = it }
            }
            selectedRequest?.let { req -> AdminDetailOverlay(req, onDismiss = { selectedRequest = null }) }
        }
        DropdownMenu(expanded = expandedLocation, onDismissRequest = { expandedLocation = false }) {
            locations.forEach { loc -> DropdownMenuItem(text = { Text(loc) }, onClick = { selectedLocation = loc; expandedLocation = false }) }
        }
        DropdownMenu(expanded = expandedSpecialty, onDismissRequest = { expandedSpecialty = false }) {
            specialties.forEach { spec -> DropdownMenuItem(text = { Text(spec) }, onClick = { selectedSpecialty = spec; expandedSpecialty = false }) }
        }
    }
}

@Composable
fun ApprovalListView(requests: List<PendingRequest>, onItemClick: (PendingRequest) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Đơn đăng ký mới", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)) }
        items(requests) { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onItemClick(req) },
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = Color.Red) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(req.name, fontWeight = FontWeight.Bold)
                        Text(req.specialty, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun AdminDetailOverlay(req: PendingRequest, onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(0.6f)) {
        Box(contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(0.85f), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thông tin đơn", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }
                    DetailItem("Họ tên", req.name)
                    DetailItem("Chuyên khoa", req.specialty)
                    DetailItem("Kinh nghiệm", req.experience)
                    DetailItem("Bệnh viện", req.hospital)
                    DetailItem("Mã CCHN", req.certId)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A))) { Text("Phê duyệt") }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray) // Cập nhật Divider thành HorizontalDivider cho bản Compose mới
    }
}

@Composable
fun AdminFilterBox(modifier: Modifier, label: String, value: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 9.sp, color = Color.Gray)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
        }
    }
}