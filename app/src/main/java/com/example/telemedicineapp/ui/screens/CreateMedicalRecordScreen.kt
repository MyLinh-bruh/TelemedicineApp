package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.telemedicineapp.ui.components.MedicalRecordForm
import com.example.telemedicineapp.ui.components.PatientInfoForm

@Composable
fun CreateMedicalRecordScreen() {
    // State cho Thông tin bệnh nhân
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var cccd by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bhyt by remember { mutableStateOf("") }

    // State cho Bệnh án
    var reason by remember { mutableStateOf("") }
    var symptoms by remember { mutableStateOf("") }
    var diagnosis by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tạo Bệnh Án Mới",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Gọi form thông tin
        PatientInfoForm(
            name = name, onNameChange = { name = it },
            age = age, onAgeChange = { age = it },
            cccd = cccd, onCccdChange = { cccd = it },
            phone = phone, onPhoneChange = { phone = it },
            bhyt = bhyt, onBhytChange = { bhyt = it }
        )

        // Gọi form bệnh án
        MedicalRecordForm(
            reason = reason, onReasonChange = { reason = it },
            symptoms = symptoms, onSymptomsChange = { symptoms = it },
            diagnosis = diagnosis, onDiagnosisChange = { diagnosis = it }
        )

        // Nút Lưu
        Button(
            onClick = {
                // Kiểm tra in ra logcat xem dữ liệu đã ăn chưa
                println("Đã lưu bệnh án: $name - Chẩn đoán: $diagnosis")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Lưu Hồ Sơ & Bệnh Án")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}