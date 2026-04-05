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

    // 🌟 Lấy hồ sơ (Nếu đã khám rồi thì hiện lại, chưa thì tạo mới)
    fun fetchRecord(patientId: String, patientName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Lấy bản ghi mới nhất của bệnh nhân này
                val snapshot = db.collection("MedicalRecords")
                    .whereEqualTo("patientId", patientId)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    val data = document.toObject(MedicalRecord::class.java)
                    _recordState.value = data?.copy(id = document.id)
                } else {
                    _recordState.value = MedicalRecord(
                        patientId = patientId,
                        patientName = patientName
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🌟 XÁC NHẬN & GỬI PHIẾU KHÁM
    fun saveRecord(record: MedicalRecord, doctorId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val collection = db.collection("MedicalRecords")

                // 1. Tạo ID nếu chưa có (cho hồ sơ mới)
                val finalId = if (record.id.isEmpty()) collection.document().id else record.id

                // 2. Định dạng thời gian gửi
                val currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

                // 3. Chuẩn bị dữ liệu cuối cùng để gửi
                val finalRecord = record.copy(
                    id = finalId,
                    doctorId = doctorId,
                    lastUpdated = currentTime
                )

                // 4. Đẩy lên Firestore (dùng .set để ghi đè hoặc tạo mới tại đúng ID đó)
                collection.document(finalId).set(finalRecord).await()

                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}