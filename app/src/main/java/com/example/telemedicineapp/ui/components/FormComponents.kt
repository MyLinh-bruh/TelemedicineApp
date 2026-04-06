package com.example.telemedicineapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun PatientInfoForm(
    name: String, onNameChange: (String) -> Unit,
    age: String, onAgeChange: (String) -> Unit,
    cccd: String, onCccdChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit,
    bhyt: String, onBhytChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Thông tin Bệnh nhân", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = name, onValueChange = onNameChange,
                label = { Text("Họ và Tên") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = age, onValueChange = onAgeChange,
                    label = { Text("Tuổi") },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = phone, onValueChange = onPhoneChange,
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            OutlinedTextField(
                value = cccd, onValueChange = onCccdChange,
                label = { Text("CCCD/CMND") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = bhyt, onValueChange = onBhytChange,
                label = { Text("Mã BHYT (Nếu có)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MedicalRecordForm(
    reason: String, onReasonChange: (String) -> Unit,
    symptoms: String, onSymptomsChange: (String) -> Unit,
    diagnosis: String, onDiagnosisChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Chi tiết Khám bệnh", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = reason, onValueChange = onReasonChange,
                label = { Text("Lý do đến khám") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = symptoms, onValueChange = onSymptomsChange,
                label = { Text("Triệu chứng lâm sàng") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            OutlinedTextField(
                value = diagnosis, onValueChange = onDiagnosisChange,
                label = { Text("Chẩn đoán sơ bộ") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}