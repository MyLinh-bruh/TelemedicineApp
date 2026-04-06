package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDoctorProfileScreen(
    viewModel: DoctorDashboardViewModel,
    onBack: () -> Unit
) {
    val doctor by viewModel.doctorProfile.collectAsState()
    val context = LocalContext.current

    if (doctor == null) return

    // Clone dữ liệu từ Firebase vào State để sửa
    var name by remember { mutableStateOf(doctor!!.name) }
    var specialty by remember { mutableStateOf(doctor!!.specialty) }
    var hospitalName by remember { mutableStateOf(doctor!!.hospitalName) }
    var address by remember { mutableStateOf(doctor!!.address) }
    var description by remember { mutableStateOf(doctor!!.description) }
    var bankName by remember { mutableStateOf(doctor!!.bankName) }
    var bankAccount by remember { mutableStateOf(doctor!!.bankAccountNumber) }

    // 🌟 THÊM STATE ĐỂ LƯU ẢNH MỚI
    var imageUrl by remember { mutableStateOf(doctor!!.imageUrl) }
    var certificateUrl by remember { mutableStateOf(doctor!!.certificateUrl) }

    var isSaving by remember { mutableStateOf(false) }

    // 🌟 HÀM HỖ TRỢ: Chuyển Uri (Ảnh chọn từ máy) sang Base64
    fun uriToBase64(uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
            bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        } catch (e: Exception) {
            null
        }
    }

    // 🌟 TRÌNH KHỞI CHẠY CHỌN ẢNH CHO AVATAR
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64String = uriToBase64(it)
            if (base64String != null) {
                imageUrl = base64String // Cập nhật state ảnh mới
            } else {
                Toast.makeText(context, "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🌟 TRÌNH KHỞI CHẠY CHỌN ẢNH CHO CHỨNG CHỈ
    val certPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64String = uriToBase64(it)
            if (base64String != null) {
                certificateUrl = base64String // Cập nhật state ảnh chứng chỉ mới
            } else {
                Toast.makeText(context, "Lỗi khi xử lý ảnh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa hồ sơ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        isSaving = true
                        // 🌟 ĐÃ CẬP NHẬT LƯU THÊM ẢNH VÀ CHỨNG CHỈ
                        val updatedUser = doctor!!.copy(
                            name = name,
                            specialty = specialty,
                            hospitalName = hospitalName,
                            address = address,
                            description = description,
                            bankName = bankName,
                            bankAccountNumber = bankAccount,
                            imageUrl = imageUrl,             // Lưu avatar mới
                            certificateUrl = certificateUrl  // Lưu chứng chỉ mới
                        )
                        viewModel.updateProfile(updatedUser) { success ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                onBack()
                            } else {
                                Toast.makeText(context, "Lỗi khi cập nhật!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Lưu thay đổi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 AVATAR - THÊM CƠ CHẾ CLICK ĐỂ CHỌN ẢNH
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB))
                    .clickable {
                        // Mở thư viện ảnh để chọn avatar
                        avatarPickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    val bitmap = remember(imageUrl) {
                        try {
                            val cleanBase64 = if (imageUrl.contains(",")) {
                                imageUrl.substringAfter(",")
                            } else {
                                imageUrl
                            }
                            val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Chạm để đổi ảnh đại diện", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Thông tin công tác", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                EditTextField(value = name, label = "Họ và tên", onValueChange = { name = it })
                EditTextField(value = specialty, label = "Chuyên khoa", onValueChange = { specialty = it })
                EditTextField(value = hospitalName, label = "Bệnh viện công tác", onValueChange = { hospitalName = it })
                EditTextField(value = address, label = "Địa chỉ phòng khám", onValueChange = { address = it })
                EditTextField(value = description, label = "Giới thiệu bản thân / Kinh nghiệm", onValueChange = { description = it }, minLines = 3)

                Spacer(modifier = Modifier.height(8.dp))
                Text("Thông tin thanh toán", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                EditTextField(value = bankName, label = "Tên Ngân hàng (VD: MB Bank, Vietcombank)", onValueChange = { bankName = it })
                EditTextField(value = bankAccount, label = "Số tài khoản", onValueChange = { bankAccount = it })

                Spacer(modifier = Modifier.height(8.dp))
                Text("Chứng chỉ hành nghề (Chạm để đổi)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                // 🌟 CHỨNG CHỈ HÀNH NGHỀ - THÊM CƠ CHẾ CLICK ĐỂ CHỌN ẢNH
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable {
                            // Mở thư viện ảnh để chọn chứng chỉ mới
                            certPickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (certificateUrl.isNotBlank()) {
                        val certBitmap = remember(certificateUrl) {
                            try {
                                val cleanBase64 = if (certificateUrl.contains(",")) {
                                    certificateUrl.substringAfter(",")
                                } else {
                                    certificateUrl
                                }
                                val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (certBitmap != null) {
                            Image(
                                bitmap = certBitmap.asImageBitmap(),
                                contentDescription = "Chứng chỉ hành nghề",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Ảnh lỗi", color = Color.Gray)
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có chứng chỉ, chạm để thêm", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EditTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedBorderColor = Color(0xFF2563EB)
        )
    )
}