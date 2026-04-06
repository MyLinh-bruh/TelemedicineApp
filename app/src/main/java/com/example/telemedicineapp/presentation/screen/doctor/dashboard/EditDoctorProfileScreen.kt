package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayOutputStream

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

    // 🌟 CHỖ NÀY ĐÃ SỬA: Biến ảnh thành Base64 ngay khi chọn xong
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val base64String = uriToBase64(context, it)
            if (base64String != null) {
                avatarUri = base64String // Bây giờ avatarUri là đống chữ Base64 vĩnh viễn
            }
        }
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
            // --- 1. ẢNH ĐẠI DIỆN ---
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
                        ProfileImageHelper(
                            base64String = avatarUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Chạm để đổi ảnh đại diện", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Text("Thông tin công tác", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Chuyên khoa") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = hosp, onValueChange = { hosp = it }, label = { Text("Bệnh viện công tác") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Địa chỉ phòng khám") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Giới thiệu bản thân / Kinh nghiệm") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp), minLines = 3)

            Spacer(Modifier.height(24.dp))

            // --- 3. CHỨNG CHỈ HÀNH NGHỀ ---
            Text("Chứng chỉ hành nghề (Chỉ xem)", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            val certData = profile?.certificateUrl ?: ""

            ProfileImageHelper(
                base64String = certData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            )

            Text(
                "Chứng chỉ đã được Admin phê duyệt. Bạn không thể tự ý thay đổi.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(24.dp))

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
                            imageUrl = avatarUri // 🌟 Giờ nó lưu đống chữ Base64 lên Firebase
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

// 🌟 HÀM DỊCH ẢNH THÀNH CHUỖI VĂN BẢN (Base64)
fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        // Nén ảnh xuống 25% để không bị quá dung lượng Firestore (1MB)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, outputStream)
        val byteArray = outputStream.toByteArray()
        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ProfileImageHelper(base64String: String, modifier: Modifier = Modifier) {
    if (base64String.isBlank()) {
        Box(modifier = modifier.background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(40.dp), tint = Color.Gray)
        }
        return
    }

    val decodedBitmap = remember(base64String) {
        try {
            val cleanBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Dự phòng cho ảnh link tạm thời
        coil.compose.AsyncImage(
            model = base64String,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}