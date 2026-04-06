package com.example.telemedicineapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.presentation.screen.doctor.MedicalRecordViewModel
import com.example.telemedicineapp.ui.components.PatientInfoForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    patientId: String,
    patientName: String,
    doctorId: String,
    onBack: () -> Unit,
    viewModel: MedicalRecordViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val record by viewModel.recordState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(patientId) {
        viewModel.fetchRecord(patientId, patientName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ Bệnh án", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            record?.let { currentRecord ->
                // States cho Form thông tin hành chính
                var name by remember { mutableStateOf(currentRecord.patientName) }
                var age by remember { mutableStateOf("") }
                var cccd by remember { mutableStateOf("") }
                var phone by remember { mutableStateOf("") }
                var bhyt by remember { mutableStateOf("") }

                // States cho chỉ số cơ thể & bệnh lý
                var height by remember { mutableStateOf(currentRecord.height) }
                var weight by remember { mutableStateOf(currentRecord.weight) }
                var bloodType by remember { mutableStateOf(currentRecord.bloodType) }
                var bloodPressure by remember { mutableStateOf(currentRecord.bloodPressure) }
                var heartRate by remember { mutableStateOf(currentRecord.heartRate) }
                var temperature by remember { mutableStateOf(currentRecord.temperature) }

                var allergies by remember { mutableStateOf(currentRecord.allergies) }
                var chronicDiseases by remember { mutableStateOf(currentRecord.chronicDiseases) }
                var pastSurgeries by remember { mutableStateOf(currentRecord.pastSurgeries) }

                var currentSymptoms by remember { mutableStateOf(currentRecord.currentSymptoms) }
                var diagnosis by remember { mutableStateOf(currentRecord.diagnosis) }
                var prescription by remember { mutableStateOf(currentRecord.prescription) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F6F9))
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Bệnh nhân: $name", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))

                    if (currentRecord.lastUpdated.isNotEmpty()) {
                        Text("Cập nhật lần cuối: ${currentRecord.lastUpdated}", fontSize = 12.sp, color = Color.Gray)
                    }

                    PatientInfoForm(
                        name = name, onNameChange = { name = it },
                        age = age, onAgeChange = { age = it },
                        cccd = cccd, onCccdChange = { cccd = it },
                        phone = phone, onPhoneChange = { phone = it },
                        bhyt = bhyt, onBhytChange = { bhyt = it }
                    )

                    SectionCard(title = "Chỉ số cơ thể & Nhóm máu") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecordTextField(value = height, label = "Chiều cao", onValueChange = { height = it }, modifier = Modifier.weight(1f))
                            RecordTextField(value = weight, label = "Cân nặng", onValueChange = { weight = it }, modifier = Modifier.weight(1f))
                            RecordTextField(value = bloodType, label = "Máu", onValueChange = { bloodType = it }, modifier = Modifier.weight(1f))
                        }
                    }

                    SectionCard(title = "Chỉ số Sinh tồn") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecordTextField(value = bloodPressure, label = "Huyết áp", onValueChange = { bloodPressure = it }, modifier = Modifier.weight(1f))
                            RecordTextField(value = heartRate, label = "Nhịp tim", onValueChange = { heartRate = it }, modifier = Modifier.weight(1f))
                            RecordTextField(value = temperature, label = "Nhiệt độ", onValueChange = { temperature = it }, modifier = Modifier.weight(1f))
                        }
                    }

                    SectionCard(title = "Tiền sử Y tế") {
                        RecordTextField(value = allergies, label = "Dị ứng", onValueChange = { allergies = it }, singleLine = false)
                        RecordTextField(value = chronicDiseases, label = "Bệnh mãn tính", onValueChange = { chronicDiseases = it }, singleLine = false)
                        RecordTextField(value = pastSurgeries, label = "Tiền sử phẫu thuật", onValueChange = { pastSurgeries = it }, singleLine = false)
                    }

                    SectionCard(title = "Tình trạng lâm sàng & Chẩn đoán") {
                        RecordTextField(value = currentSymptoms, label = "Triệu chứng", onValueChange = { currentSymptoms = it }, singleLine = false, minLines = 2)
                        RecordTextField(value = diagnosis, label = "Chẩn đoán", onValueChange = { diagnosis = it }, singleLine = false, minLines = 2)
                        RecordTextField(value = prescription, label = "Kê đơn / Điều trị", onValueChange = { prescription = it }, singleLine = false, minLines = 3)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🌟 NÚT XÁC NHẬN & GỬI PHIẾU KHÁM (MÀU XANH LÁ)
                    Button(
                        onClick = {
                            val updatedRecord = currentRecord.copy(
                                patientName = name, // Cập nhật cả tên nếu bác sĩ sửa
                                height = height, weight = weight, bloodType = bloodType,
                                bloodPressure = bloodPressure, heartRate = heartRate, temperature = temperature,
                                allergies = allergies, chronicDiseases = chronicDiseases, pastSurgeries = pastSurgeries,
                                currentSymptoms = currentSymptoms, diagnosis = diagnosis, prescription = prescription
                            )
                            viewModel.saveRecord(updatedRecord, doctorId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Đã gửi phiếu khám cho bệnh nhân $name!", Toast.LENGTH_LONG).show()
                                    onBack()
                                } else {
                                    Toast.makeText(context, "Lỗi khi lưu hồ sơ!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("XÁC NHẬN & GỬI PHIẾU KHÁM", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF334155), fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun RecordTextField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(8.dp)
    )
}