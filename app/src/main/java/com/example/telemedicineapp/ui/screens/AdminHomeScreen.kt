package com.example.telemedicineapp.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.ui.components.DoctorItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    allDoctors: List<User>,
    onDoctorClick: (User) -> Unit,
    onApproveClick: (User) -> Unit,
    onRejectClick: (User) -> Unit,
    onLogout: () -> Unit,
    // Thêm tham số callback để xử lý xóa hàng loạt
    onDeleteDoctorsClick: (List<String>) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRequest by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Trạng thái lưu trữ danh sách ID bác sĩ được chọn để xóa
    val selectedDoctorIds = remember { mutableStateListOf<String>() }
    // Trạng thái ẩn/hiện hộp thoại xác nhận xóa
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val approvedDoctors = allDoctors.filter { it.doctorStatus.toString() == "APPROVED" }
    val pendingRequests = allDoctors.filter { it.doctorStatus.toString() == "PENDING" }

    val filteredDoctors = approvedDoctors.filter { doctor ->
        doctor.name.contains(searchQuery, ignoreCase = true)
    }

    val pinkThemeColor = Color(0xFFE91E63) // Màu hồng chủ đạo

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
                        // Thanh tìm kiếm và nút xóa
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Tìm tên bác sĩ...") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            Spacer(Modifier.width(12.dp))

                            // Nút xóa hàng loạt
                            IconButton(
                                onClick = {
                                    if (selectedDoctorIds.isNotEmpty()) {
                                        showDeleteConfirmDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp) // Cùng chiều cao tương đối với OutlinedTextField
                                    .background(color = pinkThemeColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Xóa bác sĩ",
                                    tint = if (selectedDoctorIds.isNotEmpty()) pinkThemeColor else Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    items(filteredDoctors) { doctor ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Checkbox để chọn bác sĩ
                            Checkbox(
                                checked = selectedDoctorIds.contains(doctor.id),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        selectedDoctorIds.add(doctor.id)
                                    } else {
                                        selectedDoctorIds.remove(doctor.id)
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = pinkThemeColor)
                            )

                            // Doctor Item
                            Box(modifier = Modifier.weight(1f)) {
                                DoctorItem(doctor, onClick = { onDoctorClick(doctor) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Hộp thoại xác nhận xóa
                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = {
                            Text(text = "Xác nhận xóa tài khoản")
                        },
                        text = {
                            Text(text = "Bạn có chắc chắn muốn xóa vĩnh viễn ${selectedDoctorIds.size} bác sĩ đã chọn? Dữ liệu sẽ bị xóa khỏi cơ sở dữ liệu và không thể khôi phục.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // Gọi hàm xóa và truyền danh sách ID
                                    onDeleteDoctorsClick(selectedDoctorIds.toList())

                                    // Xóa xong thì làm sạch danh sách đã chọn và ẩn dialog
                                    selectedDoctorIds.clear()
                                    showDeleteConfirmDialog = false
                                }
                            ) {
                                Text("Xóa vĩnh viễn", color = pinkThemeColor)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteConfirmDialog = false }
                            ) {
                                Text("Hủy", color = Color.Gray)
                            }
                        }
                    )
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
                    onReject = {
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
                        val displayName = req.name.ifEmpty { req.email.split("@").firstOrNull() ?: "Bác sĩ" }
                        Text(displayName, fontWeight = FontWeight.Bold)
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

                    val displayName = req.name.ifEmpty { req.email.split("@").firstOrNull() ?: "Bác sĩ" }
                    DetailItem("Họ tên", displayName)
                    DetailItem("Chuyên khoa", req.specialty.ifEmpty { "Chưa có thông tin" })
                    DetailItem("Bệnh viện", req.hospitalName.ifEmpty { "Chưa có thông tin" })
                    DetailItem("Email", req.email)

                    Spacer(Modifier.height(16.dp))

                    Text("Ảnh chứng chỉ hành nghề:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))

                    Base64Image(
                        base64String = req.imageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    )

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("PHÊ DUYỆT NGAY", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                    Button(
                        onClick = onReject,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
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

@Composable
fun Base64Image(base64String: String, modifier: Modifier = Modifier) {
    if (base64String.isBlank()) {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Text("Không có ảnh", color = Color.DarkGray)
        }
        return
    }

    val decodedBitmap = remember(base64String) {
        try {
            val cleanBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = "Ảnh chứng chỉ",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(modifier = modifier.background(Color(0xFFFFEBEE)), contentAlignment = Alignment.Center) {
            Text("Lỗi định dạng ảnh", color = Color.Red)
        }
    }
}