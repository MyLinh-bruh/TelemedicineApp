package com.example.telemedicineapp.presentation.screen.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.model.MedicalRecord
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MedicalRecordViewModel @Inject constructor() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _recordState = MutableStateFlow<MedicalRecord?>(null)
    val recordState: StateFlow<MedicalRecord?> = _recordState

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchRecord(appointmentId: String, patientId: String, patientName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Tìm xem Lịch khám này đã được tạo Bệnh án chưa
                val currentRecordSnapshot = db.collection("MedicalRecords")
                    .whereEqualTo("appointmentId", appointmentId)
                    .get().await()

                if (!currentRecordSnapshot.isEmpty) {
                    // ĐÃ CÓ: Lôi ra cho bác sĩ xem/sửa tiếp
                    val doc = currentRecordSnapshot.documents[0]
                    _recordState.value = doc.toObject(MedicalRecord::class.java)?.copy(id = doc.id)
                } else {
                    // CHƯA CÓ BỆNH ÁN CHO LỊCH NÀY -> Tạo mới

                    // 🌟 BƯỚC QUAN TRỌNG: Lấy data mới nhất từ bảng Users (để khắc phục lỗi lệch Nhóm máu AB)
                    val userSnapshot = db.collection("Users").whereEqualTo("email", patientId).get().await()
                    var latestBloodType = ""
                    var latestPhone = ""
                    var latestGender = "Khác"

                    if (!userSnapshot.isEmpty) {
                        val userDoc = userSnapshot.documents[0]
                        latestBloodType = userDoc.getString("bloodType") ?: ""
                        latestPhone = userDoc.getString("phone") ?: ""
                        latestGender = userDoc.getString("gender") ?: "Khác"
                    }

                    // Tìm bệnh án cũ (nếu có) để mượn tạm chiều cao, cân nặng, tiền sử...
                    val pastRecordsSnapshot = db.collection("MedicalRecords")
                        .whereEqualTo("patientId", patientId)
                        .get().await()

                    val latestOldRecord = if (!pastRecordsSnapshot.isEmpty) {
                        pastRecordsSnapshot.toObjects(MedicalRecord::class.java).maxByOrNull { it.lastUpdated }
                    } else null

                    // 🌟 TẠO MỚI kết hợp thông tin
                    _recordState.value = MedicalRecord(
                        appointmentId = appointmentId,
                        patientId = patientId,
                        patientName = patientName,

                        // ƯU TIÊN DỮ LIỆU TỪ BẢNG USERS VÌ ĐÂY LÀ DỮ LIỆU TƯƠI NHẤT
                        bloodType = latestBloodType.ifEmpty { latestOldRecord?.bloodType ?: "" },
                        phone = latestPhone.ifEmpty { latestOldRecord?.phone ?: "" },
                        gender = latestGender,

                        // DỮ LIỆU CŨ TỪ BỆNH ÁN TRƯỚC (Nếu có)
                        age = latestOldRecord?.age ?: "",
                        identityCard = latestOldRecord?.identityCard ?: "",
                        healthInsurance = latestOldRecord?.healthInsurance ?: "",
                        height = latestOldRecord?.height ?: "",
                        weight = latestOldRecord?.weight ?: "",
                        allergies = latestOldRecord?.allergies ?: "",
                        chronicDiseases = latestOldRecord?.chronicDiseases ?: "",
                        pastSurgeries = latestOldRecord?.pastSurgeries ?: "",
                        familyMedicalHistory = latestOldRecord?.familyMedicalHistory ?: ""
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally { _isLoading.value = false }
        }
    }

    fun saveRecord(record: MedicalRecord, doctorId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val collection = db.collection("MedicalRecords")
                val finalId = if (record.id.isEmpty()) collection.document().id else record.id
                val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                val finalRecord = record.copy(id = finalId, doctorId = doctorId, lastUpdated = currentTime)
                collection.document(finalId).set(finalRecord).await()

                // Tự động cập nhật lịch hẹn thành COMPLETED
                if (record.appointmentId.isNotEmpty() && record.appointmentId != "none") {
                    db.collection("Appointments").document(record.appointmentId)
                        .update("status", "COMPLETED").await()
                }

                onComplete(true)
            } catch (e: Exception) { onComplete(false) }
        }
    }
}