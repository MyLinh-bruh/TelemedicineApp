package com.example.telemedicineapp.presentation.screen.appointment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
import com.example.telemedicineapp.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AppointmentHistoryViewModel @Inject constructor(
    private val tokenManager: TokenManager // Inject TokenManager
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // LẤY EMAIL TỪ BỘ NHỚ MÁY ĐỂ LÀM KHÓA TÌM KIẾM
        val currentUserEmail = tokenManager.getEmail() ?: ""
        if (currentUserEmail.isNotEmpty()) {
            fetchMyAppointments(currentUserEmail)
        } else {
            _isLoading.value = false
            Log.e("HistoryVM", "Không tìm thấy Email người dùng trong máy!")
        }
    }

    private fun fetchMyAppointments(email: String) {
        viewModelScope.launch {
            _isLoading.value = true

            // Lọc trên Firestore: Chỉ lấy những lịch hẹn mà patientId = email của mình
            db.collection("Appointments")
                .whereEqualTo("patientId", email)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("HistoryVM", "Lỗi tải lịch sử: ", error)
                        _isLoading.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Appointment(
                                    id = doc.id,
                                    patientId = doc.getString("patientId") ?: "",
                                    doctorId = doc.getString("doctorId") ?: "",
                                    doctorName = doc.getString("doctorName") ?: "Bác sĩ",
                                    dateTimeUtc = doc.getString("dateTimeUtc") ?: "",
                                    reason = doc.getString("reason") ?: "",
                                    status = doc.getString("status") ?: "PENDING"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _appointments.value = list
                    }
                    _isLoading.value = false
                }
        }
    }

    // Xử lý hủy lịch hẹn
    fun cancelAppointment(appointmentId: String) {
        viewModelScope.launch {
            try {
                db.collection("Appointments").document(appointmentId)
                    .update("status", "CANCELLED")
                    .await()
                Log.d("HistoryVM", "Đã hủy lịch hẹn: $appointmentId")
            } catch (e: Exception) {
                Log.e("HistoryVM", "Lỗi khi hủy lịch hẹn: ", e)
            }
        }
    }

    // Hàm hỗ trợ format giờ UTC
    fun formatDateTime(utcString: String): String {
        return try {
            val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            utcFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = utcFormat.parse(utcString)

            val localFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
            localFormat.timeZone = TimeZone.getDefault()
            date?.let { localFormat.format(it) } ?: utcString
        } catch (e: Exception) {
            utcString
        }
    }
}