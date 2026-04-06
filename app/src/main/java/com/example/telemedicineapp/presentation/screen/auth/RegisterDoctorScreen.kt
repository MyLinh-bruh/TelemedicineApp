package com.example.telemedicineapp.presentation.screen.auth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.telemedicineapp.presentation.screens.auth.AuthViewModel
import java.io.ByteArrayOutputStream

// 🌟 Hàm chuyển đổi URI thành chuỗi Base64
fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        // Nén ảnh xuống 20% chất lượng để không vượt quá giới hạn 1MB của Firestore
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, outputStream)
        val byteArray = outputStream.toByteArray()

        "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterDoctorScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // --- CÁC BIẾN TRẠNG THÁI ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var certificateUri by remember { mutableStateOf<Uri?>(null) }

    // 🌟 KHAI BÁO CHO DROPDOWN ĐỊA CHỈ & CHUYÊN KHOA
    var selectedProvince by remember { mutableStateOf("") }
    var expandedProvince by remember { mutableStateOf(false) }

    var specialty by remember { mutableStateOf("") }
    var expandedSpecialty by remember { mutableStateOf(false) }

    // DANH SÁCH 34 TỈNH THÀNH
    val provinces = listOf(
        "Hà Nội", "TP. Hồ Chí Minh", "Hải Phòng", "Đà Nẵng", "Cần Thơ", "Huế",
        "Tuyên Quang", "Lào Cai", "Thái Nguyên", "Phú Thọ", "Bắc Ninh", "Hưng Yên",
        "Ninh Bình", "Quảng Trị", "Quảng Ngãi", "Gia Lai", "Khánh Hòa", "Lâm Đồng",
        "Đắk Lắk", "Đồng Nai", "Tây Ninh", "Vĩnh Long", "Đồng Tháp", "Cà Mau",
        "An Giang", "Lai Châu", "Điện Biên", "Sơn La", "Lạng Sơn", "Quảng Ninh",
        "Thanh Hóa", "Nghệ An", "Hà Tĩnh", "Cao Bằng"
    )

    // 🌟 DANH SÁCH CHUYÊN KHOA
    val specialtiesList = listOf(
        "Nội khoa", "Ngoại khoa", "Nhi khoa", "Sản phụ khoa", "Tim mạch",
        "Da liễu", "Tai Mũi Họng", "Răng Hàm Mặt", "Mắt", "Cơ Xương Khớp",
        "Thần kinh", "Tiêu hóa - Gan mật", "Hô hấp", "Thận - Tiết niệu",
        "Nội tiết", "Dị ứng - Miễn dịch", "Huyết học", "Truyền nhiễm",
        "Ung bướu", "Chấn thương chỉnh hình", "Phục hồi chức năng",
        "Tâm thần", "Dinh dưỡng", "Y học cổ truyền"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        certificateUri = uri
    }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isWaitingApproval by viewModel.isWaitingApproval.collectAsState()
    val isApproved by viewModel.isApproved.collectAsState()
    val isRejected by viewModel.isRejected.collectAsState()

    // ----------------------------------------------------------------
    // POPUPS XỬ LÝ TRẠNG THÁI
    // ----------------------------------------------------------------
    if (isWaitingApproval) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Đang xử lý", fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Yêu cầu đăng ký đã được gửi.\nVui lòng chờ Admin phê duyệt...", textAlign = TextAlign.Center)
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.cancelRegistration() }) {
                    Text("HỦY", fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        )
    }

    if (isApproved) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Thành công!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
            text = { Text("Tài khoản Bác sĩ đã được phê duyệt.\nChào mừng bạn gia nhập hệ thống!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApprovalState()
                        onBackToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Đăng nhập ngay") }
            }
        )
    }

    if (isRejected) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Yêu cầu bị từ chối", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("Yêu cầu đăng ký Bác sĩ của bạn đã bị Admin từ chối và hủy bỏ khỏi hệ thống.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApprovalState()
                        onBackToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Quay lại Đăng nhập", color = Color.White) }
            }
        )
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Lỗi hệ thống", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(errorMessage!!) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("ĐÓNG") } }
        )
    }

    // ----------------------------------------------------------------
    // GIAO DIỆN CHÍNH
    // ----------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đăng ký Bác sĩ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Hồ sơ Chuyên môn", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5), modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên Bác sĩ") }, placeholder = { Text("Ví dụ: BS. Nguyễn Phương Thảo") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) }, shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email đăng nhập") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Xác nhận mật khẩu") }, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Thông tin bác sĩ", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            // 🌟 MENU CHỌN CHUYÊN KHOA
            ExposedDropdownMenuBox(
                expanded = expandedSpecialty,
                onExpandedChange = { if (!isLoading) expandedSpecialty = !expandedSpecialty }
            ) {
                OutlinedTextField(
                    value = specialty.ifEmpty { "Chọn Chuyên khoa" },
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Medication, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSpecialty) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedSpecialty,
                    onDismissRequest = { expandedSpecialty = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    specialtiesList.forEach { spec ->
                        DropdownMenuItem(
                            text = { Text(spec) },
                            onClick = {
                                specialty = spec
                                expandedSpecialty = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = hospitalName, onValueChange = { hospitalName = it }, label = { Text("Bệnh viện công tác") }, leadingIcon = { Icon(Icons.Default.Business, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))

            // 🌟 MENU CHỌN TỈNH/THÀNH PHỐ
            ExposedDropdownMenuBox(
                expanded = expandedProvince,
                onExpandedChange = { if (!isLoading) expandedProvince = !expandedProvince }
            ) {
                OutlinedTextField(
                    value = selectedProvince.ifEmpty { "Khu vực hoạt động (Tỉnh/Thành phố)" },
                    onValueChange = {},
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProvince) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedProvince,
                    onDismissRequest = { expandedProvince = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    provinces.forEach { province ->
                        DropdownMenuItem(
                            text = { Text(province) },
                            onClick = {
                                selectedProvince = province
                                expandedProvince = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- PHẦN CHỌN ẢNH CHỨNG CHỈ ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Ảnh chứng chỉ hành nghề (Bắt buộc)", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))

                if (certificateUri != null) {
                    AsyncImage(
                        model = certificateUri,
                        contentDescription = "Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF3F51B5), RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Đổi ảnh khác", fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
                    }
                } else {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(Icons.Default.UploadFile, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bấm để tải ảnh chứng chỉ lên", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🌟 NÚT GỬI ĐĂNG KÝ
            Button(
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || specialty.isEmpty() || hospitalName.isEmpty()) {
                        viewModel.showError("Vui lòng điền đầy đủ các thông tin!")
                    } else if (password != confirmPassword) {
                        viewModel.showError("Mật khẩu xác nhận không khớp!")
                    } else if (selectedProvince.isEmpty()) {
                        viewModel.showError("Vui lòng chọn Tỉnh/Thành phố hoạt động!")
                    } else if (certificateUri == null) {
                        viewModel.showError("Vui lòng tải lên ảnh chứng chỉ hành nghề!")
                    } else {
                        val base64ImageString = uriToBase64(context, certificateUri!!)

                        if (base64ImageString != null) {
                            // 🌟 ĐÃ SỬA: Bổ sung selectedProvince vào hàm
                            viewModel.registerDoctorRequest(
                                name = name,
                                email = email,
                                pass = password,
                                specialty = specialty,
                                hospitalName = hospitalName,
                                address = selectedProvince, // <--- ĐỊA CHỈ NÈ MÁ
                                certificateImage = base64ImageString
                            )
                        } else {
                            viewModel.showError("Lỗi xử lý ảnh, vui lòng chọn ảnh khác!")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("GỬI YÊU CẦU ĐĂNG KÝ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}