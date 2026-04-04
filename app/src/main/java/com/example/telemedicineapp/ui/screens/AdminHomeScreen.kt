package com.example.telemedicineapp.ui.screens
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.Doctor
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
    allDoctors: List<Doctor>,
    onDoctorClick: (Doctor) -> Unit,
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
            PendingRequest(
                "1",
                "BS. Nguyễn Văn An",
                "Tim mạch",
                "10 năm",
                "BV Bạch Mai",
                "0901234567",
                "CCHN-123"
            ),
            PendingRequest(
                "2",
                "BS. Lê Thị Bình",
                "Nhi khoa",
                "8 năm",
                "BV Nhi TW",
                "0987654321",
                "CCHN-456"
            )
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
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.ExitToApp,
                            null,
                            tint = Color.Red
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Hệ thống") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, null) },
                    label = { Text("Duyệt đơn") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {

                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Quản lý danh sách bác sĩ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tìm tên bác sĩ...", fontSize = 14.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                    items(filteredDoctors) { doctor ->
                        DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            DropdownMenu(
                expanded = expandedLocation,
                onDismissRequest = { expandedLocation = false }) {
                locations.forEach { loc ->
                    DropdownMenuItem(
                        text = { Text(loc) },
                        onClick = { selectedLocation = loc; expandedLocation = false })
                }
            }
            DropdownMenu(
                expanded = expandedSpecialty,
                onDismissRequest = { expandedSpecialty = false }) {
                specialties.forEach { spec ->
                    DropdownMenuItem(
                        text = { Text(spec) },
                        onClick = { selectedSpecialty = spec; expandedSpecialty = false })
                }
            }
        }
    }
    @Composable
    fun ApprovalListView(requests: List<PendingRequest>, onItemClick: (PendingRequest) -> Unit) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                Text("Đơn đăng ký mới", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(
                Modifier.height(16.dp)
            )
            }
            items(requests) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        .clickable { onItemClick(req) },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).background(Color(0xFFFFEBEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Person, null, tint = Color.Red) }
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
}
