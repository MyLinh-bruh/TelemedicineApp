package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

    var isSaving by remember { mutableStateOf(false) }

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
            // Nút Lưu cố định ở dưới cùng
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        isSaving = true
                        val updatedUser = doctor!!.copy(
                            name = name,
                            specialty = specialty,
                            hospitalName = hospitalName,
                            address = address,
                            description = description,
                            bankName = bankName,
                            bankAccountNumber = bankAccount
                        )
                        viewModel.updateProfile(updatedUser) { success ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                                onBack() // Sửa xong thì quay lại chế độ Xem
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

            // Avatar placeholder
            Box(
                modifier = Modifier.size(100.dp).background(Color(0xFFF1F5F9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Chạm để đổi ảnh đại diện", fontSize = 12.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // Các ô nhập liệu
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
                Text("Chứng chỉ hành nghề (Chỉ xem)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                // Placeholder cho ảnh chứng chỉ
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp).background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Tránh bị che bởi nút Lưu
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