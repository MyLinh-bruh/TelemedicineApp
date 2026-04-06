package com.example.telemedicineapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.telemedicineapp.ui.components.MedicalRecordForm
import com.example.telemedicineapp.ui.components.PatientInfoForm
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun CreateMedicalRecordScreen(
    patientId: String = "", // Bổ sung tham số để xác định bệnh nhân
    doctorId: String = "",  // Bổ sung tham số để xác định bác sĩ
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

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

    var isSaving by remember { mutableStateOf(false) }

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
                if (patientId.isEmpty() || doctorId.isEmpty()) {
                    Toast.makeText(context, "Lỗi: Không tìm thấy ID bệnh nhân/Bác sĩ", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isSaving = true
                    try {
                        // 1. Lưu hồ sơ bệnh án (Tạo record mới trên Firebase)
                        val recordData = hashMapOf(
                            "patientId" to patientId,
                            "doctorId" to doctorId,
                            "patientName" to name,
                            "diagnosis" to diagnosis,
                            "symptoms" to symptoms,
                            "reason" to reason
                            // Thêm các trường khác nếu cần
                        )
                        db.collection("MedicalRecords").add(recordData).await()

                        // 2. 🌟 TỰ ĐỘNG ĐỔI TRẠNG THÁI LỊCH HẸN SANG ĐÃ KHÁM 🌟
                        val snapshot = db.collection("Appointments")
                            .whereEqualTo("patientId", patientId)
                            .whereEqualTo("doctorId", doctorId)
                            .whereEqualTo("status", "PAID")
                            .get().await()

                        for (doc in snapshot.documents) {
                            db.collection("Appointments").document(doc.id)
                                .update("status", "COMPLETED").await()
                        }

                        Toast.makeText(context, "Đã lưu Bệnh án & Cập nhật Lịch hẹn thành công!", Toast.LENGTH_LONG).show()
                        onBack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Lưu Hồ Sơ & Đóng Lịch Hẹn")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}