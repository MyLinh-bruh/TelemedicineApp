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

    // 🌟 LOGIC THÔNG MINH: Tìm bệnh án theo Lịch, nếu không có thì lấy TThông tin hành chính từ bệnh án cũ
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
                    // CHƯA CÓ: Tìm tất cả bệnh án cũ của bệnh nhân này
                    val pastRecordsSnapshot = db.collection("MedicalRecords")
                        .whereEqualTo("patientId", patientId)
                        .get().await()

                    if (!pastRecordsSnapshot.isEmpty) {
                        // Sắp xếp lấy cái mới nhất
                        val latestOldRecord = pastRecordsSnapshot.toObjects(MedicalRecord::class.java)
                            .maxByOrNull { it.lastUpdated }

                        // 🌟 TẠO MỚI nhưng COPY thông tin hành chính & tiền sử, GIỮ TRỐNG thông tin khám
                        _recordState.value = MedicalRecord(
                            appointmentId = appointmentId,
                            patientId = patientId,
                            patientName = patientName,
                            age = latestOldRecord?.age ?: "",
                            phone = latestOldRecord?.phone ?: "",
                            identityCard = latestOldRecord?.identityCard ?: "",
                            healthInsurance = latestOldRecord?.healthInsurance ?: "",
                            height = latestOldRecord?.height ?: "",
                            weight = latestOldRecord?.weight ?: "",
                            bloodType = latestOldRecord?.bloodType ?: "",
                            gender = latestOldRecord?.gender ?: "Khác",
                            allergies = latestOldRecord?.allergies ?: "",
                            chronicDiseases = latestOldRecord?.chronicDiseases ?: "",
                            pastSurgeries = latestOldRecord?.pastSurgeries ?: "",
                            familyMedicalHistory = latestOldRecord?.familyMedicalHistory ?: ""
                        )
                    } else {
                        // Bệnh nhân mới toanh: Khởi tạo trống hoàn toàn
                        _recordState.value = MedicalRecord(
                            appointmentId = appointmentId,
                            patientId = patientId,
                            patientName = patientName
                        )
                    }
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

                // 🌟 Tự động cập nhật ĐÚNG cái lịch hẹn đó thành COMPLETED
                if (record.appointmentId.isNotEmpty() && record.appointmentId != "none") {
                    db.collection("Appointments").document(record.appointmentId)
                        .update("status", "COMPLETED").await()
                }

                onComplete(true)
            } catch (e: Exception) { onComplete(false) }
        }
    }
}