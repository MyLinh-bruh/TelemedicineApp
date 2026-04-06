package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDoctorProfileScreen(
    viewModel: DoctorDashboardViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.doctorProfile.collectAsState()
    val context = LocalContext.current

    // Khởi tạo các State từ dữ liệu hiện có
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var spec by remember(profile) { mutableStateOf(profile?.specialty ?: "") }
    var hosp by remember(profile) { mutableStateOf(profile?.hospitalName ?: "") }
    var addr by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var bankName by remember(profile) { mutableStateOf(profile?.bankName ?: "") }
    var bankAcc by remember(profile) { mutableStateOf(profile?.bankAccountNumber ?: "") }
    var desc by remember(profile) { mutableStateOf(profile?.description ?: "") }
    var avatarUri by remember(profile) { mutableStateOf(profile?.imageUrl ?: "") }

    // Launcher mở thư viện ảnh cho AVATAR
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { avatarUri = it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 1. ẢNH ĐẠI DIỆN (Được phép đổi) ---
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri.isNotEmpty()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chạm để đổi ảnh đại diện", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // --- 2. THÔNG TIN CÔNG TÁC ---
            Text("Thông tin công tác", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Chuyên khoa") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = hosp, onValueChange = { hosp = it }, label = { Text("Bệnh viện công tác") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Địa chỉ phòng khám") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Giới thiệu bản thân / Kinh nghiệm") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )

            Spacer(Modifier.height(24.dp))

            // --- 3. 🌟 CHỨNG CHỈ HÀNH NGHỀ (CHỈ XEM) ---
            Text("Chứng chỉ hành nghề (Chỉ xem)", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            // Lấy chứng chỉ (ưu tiên certificateUrl, nếu acc cũ thì lấy tạm imageUrl)
            val certUri = profile?.certificateUrl?.takeIf { it.isNotEmpty() } ?: profile?.imageUrl ?: ""

            if (certUri.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = certUri,
                        contentDescription = "Chứng chỉ",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    "Chứng chỉ đã được Admin phê duyệt. Bạn không thể tự ý thay đổi.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            } else {
                Text("Chưa cập nhật chứng chỉ", color = Color.Gray, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // --- 4. THÔNG TIN THANH TOÁN ---
            Text("Thông tin thanh toán", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = bankAcc, onValueChange = { bankAcc = it }, label = { Text("Số tài khoản") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    profile?.let {
                        val updated = it.copy(
                            name = name,
                            specialty = spec,
                            hospitalName = hosp,
                            address = addr,
                            bankName = bankName,
                            bankAccountNumber = bankAcc,
                            description = desc,
                            imageUrl = avatarUri // Chỉ cập nhật Avatar
                        )
                        viewModel.updateProfile(updated) { success ->
                            if (success) {
                                Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("LƯU THAY ĐỔI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}