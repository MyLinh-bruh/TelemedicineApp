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

        // Thêm tiền tố này để thư viện AsyncImage (Coil) tự động hiểu đây là ảnh Base64
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
    // Lấy context để dùng cho hàm uriToBase64
    val context = LocalContext.current

    // --- CÁC BIẾN TRẠNG THÁI ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var certificateUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher mở thư viện ảnh
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        certificateUri = uri
    }

    // State từ ViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isWaitingApproval by viewModel.isWaitingApproval.collectAsState()
    val isApproved by viewModel.isApproved.collectAsState()
    val isRejected by viewModel.isRejected.collectAsState()


    // ----------------------------------------------------------------
    // 1. POPUP: ĐANG CHỜ PHÊ DUYỆT (Hiện sau khi bấm đăng ký)
    // ----------------------------------------------------------------
    if (isWaitingApproval) {
        AlertDialog(
            onDismissRequest = { /* Chặn tắt ngang */ },
            title = { Text("Đang xử lý", fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(color = Color(0xFF3F51B5))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Yêu cầu đăng ký đã được gửi.\nVui lòng chờ Admin phê duyệt...",
                        textAlign = TextAlign.Center
                    )
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

    // ----------------------------------------------------------------
    // 2. POPUP: ĐÃ ĐƯỢC PHÊ DUYỆT (Tự động hiện khi Admin bấm Duyệt)
    // ----------------------------------------------------------------
    if (isApproved) {
        AlertDialog(
            onDismissRequest = { /* Chặn tắt ngang */ },
            title = { Text("Thành công!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) },
            text = { Text("Tài khoản Bác sĩ đã được phê duyệt.\nChào mừng bạn gia nhập hệ thống!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApprovalState()
                        onBackToLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Đăng nhập ngay")
                }
            }
        )
    }

    if (isRejected) {
        AlertDialog(
            onDismissRequest = { /* Chặn tắt ngang */ },
            title = { Text("Yêu cầu bị từ chối", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text("Yêu cầu đăng ký Bác sĩ của bạn đã bị Admin từ chối và hủy bỏ khỏi hệ thống.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApprovalState()
                        onBackToLogin() // Bấm xong thì đẩy họ về trang đăng nhập
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Quay lại Đăng nhập", color = Color.White)
                }
            }
        )
    }

    // POPUP THÔNG BÁO LỖI
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Lỗi hệ thống", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("ĐÓNG") }
            }
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

            Text(
                text = "Hồ sơ Chuyên môn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F51B5),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Họ và tên Bác sĩ") },
                placeholder = { Text("Ví dụ: BS. Nguyễn Phương Thảo") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email đăng nhập") }, leadingIcon = { Icon(Icons.Default.Email, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Xác nhận mật khẩu") }, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Default.Lock, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Thông tin bác sĩ", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(value = specialty, onValueChange = { specialty = it }, label = { Text("Chuyên khoa (VD: Thần Kinh)") }, leadingIcon = { Icon(Icons.Default.Medication, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = hospitalName, onValueChange = { hospitalName = it }, label = { Text("Bệnh viện công tác") }, leadingIcon = { Icon(Icons.Default.Business, null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)

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

            // 🌟 NÚT GỬI ĐĂNG KÝ MỚI
            Button(
                onClick = {
                    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || specialty.isEmpty() || hospitalName.isEmpty()) {
                        viewModel.showError("Vui lòng điền đầy đủ các thông tin!")
                    } else if (password != confirmPassword) {
                        viewModel.showError("Mật khẩu xác nhận không khớp!")
                    } else if (certificateUri == null) {
                        viewModel.showError("Vui lòng tải lên ảnh chứng chỉ hành nghề!")
                    } else {
                        // Chuyển Uri thành chuỗi Base64
                        val base64ImageString = uriToBase64(context, certificateUri!!)

                        if (base64ImageString != null) {
                            // Gửi chuỗi text này vào ViewModel
                            viewModel.registerDoctorRequest(name, email, password, specialty, hospitalName, base64ImageString)
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