package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val profile by viewModel.doctorProfile.collectAsState()
    val context = LocalContext.current

    // Khởi tạo các State từ dữ liệu hiện có trong Firebase
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var spec by remember(profile) { mutableStateOf(profile?.specialty ?: "") }
    var hosp by remember(profile) { mutableStateOf(profile?.hospitalName ?: "") }
    var addr by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var bankName by remember(profile) { mutableStateOf(profile?.bankName ?: "") }
    var bankAcc by remember(profile) { mutableStateOf(profile?.bankAccountNumber ?: "") }

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
            Text("Thông tin công tác", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Họ và tên") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Chuyên khoa") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = hosp, onValueChange = { hosp = it }, label = { Text("Bệnh viện công tác") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Địa chỉ phòng khám") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(24.dp))

            Text("Thông tin thanh toán", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
            OutlinedTextField(value = bankName, onValueChange = { bankName = it }, label = { Text("Tên ngân hàng") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))
            OutlinedTextField(value = bankAcc, onValueChange = { bankAcc = it }, label = { Text("Số tài khoản") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    profile?.let {
                        val updated = it.copy(name = name, specialty = spec, hospitalName = hosp, address = addr, bankName = bankName, bankAccountNumber = bankAcc)
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
