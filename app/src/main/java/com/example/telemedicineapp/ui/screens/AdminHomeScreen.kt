package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage // Đảm bảo đã có thư viện Coil
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.ui.components.DoctorItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    allDoctors: List<User>,
    onDoctorClick: (User) -> Unit,
    onApproveClick: (User) -> Unit,
    onRejectClick: (User) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRequest by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // SỬA LỖI Ở ĐÂY: Thêm .toString() để ép kiểu về chuỗi trước khi so sánh
    val approvedDoctors = allDoctors.filter { it.doctorStatus.toString() == "APPROVED" }
    val pendingRequests = allDoctors.filter { it.doctorStatus.toString() == "PENDING" }

    val filteredDoctors = approvedDoctors.filter { doctor ->
        doctor.name.contains(searchQuery, ignoreCase = true)
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
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Hệ thống") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(badge = { if(pendingRequests.isNotEmpty()) Badge { Text("${pendingRequests.size}") } }) {
                            Icon(Icons.Default.Notifications, null)
                        }
                    },
                    label = { Text("Duyệt đơn") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                // DANH SÁCH BÁC SĨ ĐÃ DUYỆT
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tìm tên bác sĩ...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    items(filteredDoctors) { doctor ->
                        DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                        Spacer(Modifier.height(8.dp))
                    }
                }
            } else {
                // DANH SÁCH CHỜ DUYỆT (PENDING)
                ApprovalListView(
                    requests = pendingRequests,
                    onItemClick = { selectedRequest = it }
                )
            }

            // POPUP CHI TIẾT ĐỂ PHÊ DUYỆT / TỪ CHỐI
            selectedRequest?.let { req ->
                AdminDetailOverlay(
                    req = req,
                    onDismiss = { selectedRequest = null },
                    onApprove = {
                        onApproveClick(req)
                        selectedRequest = null
                    },
                    onReject = { // 🌟 TRUYỀN SỰ KIỆN TỪ CHỐI VÀO ĐÂY
                        onRejectClick(req)
                        selectedRequest = null
                    }
                )
            }
        }
    }
}

@Composable
fun ApprovalListView(requests: List<User>, onItemClick: (User) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Yêu cầu mới", fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)) }

        if (requests.isEmpty()) {
            item { Text("Không có đơn nào cần duyệt", color = Color.Gray, modifier = Modifier.padding(20.dp)) }
        }

        items(requests) { req ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onItemClick(req) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Color.Red)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        // ĐỒNG BỘ: Hiện tên thật
                        val displayName = req.name.ifEmpty { req.email.split("@").firstOrNull() ?: "Bác sĩ" }
                        Text(displayName, fontWeight = FontWeight.Bold)
                        // ĐỒNG BỘ: Hiện chuyên khoa thật (Thần Kinh,...)
                        Text(req.specialty.ifEmpty { "Chưa cập nhật chuyên khoa" }, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.KeyboardArrowRight, null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun AdminDetailOverlay(req: User, onDismiss: () -> Unit, onApprove: () -> Unit, onReject: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.6f)) {
        Box(contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thông tin đơn", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ĐỒNG BỘ THÔNG TIN TỪ MODEL USER
                    val displayName = req.name.ifEmpty { req.email.split("@").firstOrNull() ?: "Bác sĩ" }
                    DetailItem("Họ tên", displayName)
                    DetailItem("Chuyên khoa", req.specialty.ifEmpty { "Chưa có thông tin" })
                    DetailItem("Bệnh viện", req.hospitalName.ifEmpty { "Chưa có thông tin" })
                    DetailItem("Email", req.email)

                    Spacer(Modifier.height(16.dp))

                    Text("Ảnh chứng chỉ hành nghề:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))

                    // ĐỒNG BỘ: Hiện ảnh "Con mèo" (imageUrl) từ Firebase
                    AsyncImage(
                        model = req.imageUrl,
                        contentDescription = "Chứng chỉ",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Màu xanh lá phê duyệt
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PHÊ DUYỆT NGAY", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    Button(
                        onClick = onReject, // Gọi hàm xóa
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red), // Màu đỏ
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TỪ CHỐI & XÓA ĐƠN")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}
// Linh đã hoàn thiện chức năng UI Admin và duyệt đơn ở đây