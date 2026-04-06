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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordScreen(
    patientId: String,
    patientName: String,
    doctorId: String,
    onBack: () -> Unit,
    isReadOnly: Boolean = false,
    viewModel: MedicalRecordViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val record by viewModel.recordState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(patientId) {
        viewModel.fetchRecord(patientId, patientName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isReadOnly) "Chi tiết Bệnh án" else "Hồ sơ Bệnh án", fontWeight = FontWeight.Bold) },
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
                // 🌟 States cho Form thông tin hành chính - Lấy từ data lên
                var name by remember { mutableStateOf(currentRecord.patientName) }
                var age by remember { mutableStateOf(currentRecord.age) }
                var cccd by remember { mutableStateOf(currentRecord.identityCard) }
                var phone by remember { mutableStateOf(currentRecord.phone) }
                var bhyt by remember { mutableStateOf(currentRecord.healthInsurance) }

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

                var isSavingProgress by remember { mutableStateOf(false) }

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

                    // Form thông tin hành chính
                    PatientInfoForm(
                        name = name, onNameChange = { if (!isReadOnly) name = it },
                        age = age, onAgeChange = { if (!isReadOnly) age = it },
                        cccd = cccd, onCccdChange = { if (!isReadOnly) cccd = it },
                        phone = phone, onPhoneChange = { if (!isReadOnly) phone = it },
                        bhyt = bhyt, onBhytChange = { if (!isReadOnly) bhyt = it }
                    )

                    SectionCard(title = "Chỉ số cơ thể & Nhóm máu") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecordTextField(value = height, label = "Chiều cao", onValueChange = { height = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                            RecordTextField(value = weight, label = "Cân nặng", onValueChange = { weight = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                            RecordTextField(value = bloodType, label = "Máu", onValueChange = { bloodType = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                        }
                    }

                    SectionCard(title = "Chỉ số Sinh tồn") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecordTextField(value = bloodPressure, label = "Huyết áp", onValueChange = { bloodPressure = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                            RecordTextField(value = heartRate, label = "Nhịp tim", onValueChange = { heartRate = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                            RecordTextField(value = temperature, label = "Nhiệt độ", onValueChange = { temperature = it }, isReadOnly = isReadOnly, modifier = Modifier.weight(1f))
                        }
                    }

                    SectionCard(title = "Tiền sử Y tế") {
                        RecordTextField(value = allergies, label = "Dị ứng", onValueChange = { allergies = it }, isReadOnly = isReadOnly, singleLine = false)
                        RecordTextField(value = chronicDiseases, label = "Bệnh mãn tính", onValueChange = { chronicDiseases = it }, isReadOnly = isReadOnly, singleLine = false)
                        RecordTextField(value = pastSurgeries, label = "Tiền sử phẫu thuật", onValueChange = { pastSurgeries = it }, isReadOnly = isReadOnly, singleLine = false)
                    }

                    SectionCard(title = "Tình trạng lâm sàng & Chẩn đoán") {
                        RecordTextField(value = currentSymptoms, label = "Triệu chứng", onValueChange = { currentSymptoms = it }, isReadOnly = isReadOnly, singleLine = false, minLines = 2)
                        RecordTextField(value = diagnosis, label = "Chẩn đoán", onValueChange = { diagnosis = it }, isReadOnly = isReadOnly, singleLine = false, minLines = 2)
                        RecordTextField(value = prescription, label = "Kê đơn / Điều trị", onValueChange = { prescription = it }, isReadOnly = isReadOnly, singleLine = false, minLines = 3)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isReadOnly) {
                        Button(
                            onClick = {
                                isSavingProgress = true
                                // 🌟 Đóng gói tất cả thông tin hành chính và chuyên môn để lưu
                                val updatedRecord = currentRecord.copy(
                                    patientName = name,
                                    age = age,
                                    identityCard = cccd,
                                    phone = phone,
                                    healthInsurance = bhyt,
                                    height = height, weight = weight, bloodType = bloodType,
                                    bloodPressure = bloodPressure, heartRate = heartRate, temperature = temperature,
                                    allergies = allergies, chronicDiseases = chronicDiseases, pastSurgeries = pastSurgeries,
                                    currentSymptoms = currentSymptoms, diagnosis = diagnosis, prescription = prescription
                                )
                                viewModel.saveRecord(updatedRecord, doctorId) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Đã lưu hồ sơ bệnh án!", Toast.LENGTH_LONG).show()
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Lỗi khi lưu hồ sơ!", Toast.LENGTH_SHORT).show()
                                        isSavingProgress = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSavingProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            if (isSavingProgress) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("XÁC NHẬN & GỬI PHIẾU KHÁM", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
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
    isReadOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (!isReadOnly) onValueChange(it) },
        label = { Text(label, fontSize = 13.sp) },
        readOnly = isReadOnly,
        enabled = !isReadOnly,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color.Black,
            disabledBorderColor = Color.LightGray,
            disabledLabelColor = Color.Gray,
        )
    )
}