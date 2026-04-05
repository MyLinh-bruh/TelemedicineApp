package com.example.telemedicineapp.presentation.screen.appointment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.AppointmentRepository
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.model.DoctorSchedule
import com.example.telemedicineapp.utils.TimeUtils
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // 1. Trạng thái tiến trình đặt lịch
    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState

    // 2. Lịch rảnh của bác sĩ
    private val _doctorSchedule = MutableStateFlow<DoctorSchedule?>(null)
    val doctorSchedule: StateFlow<DoctorSchedule?> = _doctorSchedule

    // 3. Danh sách các Slot đã có người đặt
    private val _bookedSlots = MutableStateFlow<List<String>>(emptyList())
    val bookedSlots: StateFlow<List<String>> = _bookedSlots

    // 🌟 Biến lưu ID lịch hẹn đang xử lý thanh toán
    private var currentAppointmentId: String? = null

    fun getSchedulesAndAppointments(doctorId: String, date: String) {
        viewModelScope.launch {
            val localDate = java.time.LocalDate.parse(date)
            val dayOfWeek = localDate.dayOfWeek.value

            try {
                val specificSnapshot = db.collection("DoctorSchedules")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("date", date)
                    .get().await()

                if (!specificSnapshot.isEmpty) {
                    _doctorSchedule.value = specificSnapshot.documents[0].toObject(DoctorSchedule::class.java)
                } else {
                    val doctorDefaultSnapshot = db.collection("DoctorDefaults")
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("dayOfWeek", dayOfWeek)
                        .get().await()

                    if (!doctorDefaultSnapshot.isEmpty) {
                        val docData = doctorDefaultSnapshot.documents[0]
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            morningSlots = docData.get("morningSlots") as List<String>,
                            afternoonSlots = docData.get("afternoonSlots") as List<String>
                        )
                    } else {
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            morningSlots = listOf("08:00 - 09:00", "09:00 - 10:00", "10:00 - 11:00"),
                            afternoonSlots = listOf("14:00 - 15:00", "15:00 - 16:00")
                        )
                    }
                }
                fetchBookedSlots(doctorId, date)
            } catch (e: Exception) {
                _doctorSchedule.value = null
            }
        }
    }

    private suspend fun fetchBookedSlots(doctorId: String, date: String) {
        try {
            val startOfDay = "${date}T00:00:00Z"
            val endOfDay = "${date}T23:59:59Z"

            val snapshot = db.collection("Appointments")
                .whereEqualTo("doctorId", doctorId)
                .whereGreaterThanOrEqualTo("dateTimeUtc", startOfDay)
                .whereLessThanOrEqualTo("dateTimeUtc", endOfDay)
                .get()
                .await()

            val booked = snapshot.documents.mapNotNull { doc ->
                val utcTime = doc.getString("dateTimeUtc") ?: return@mapNotNull null
                val status = doc.getString("status") ?: ""
                if (status != "CANCELLED") {
                    TimeUtils.extractSlotFromUtc(utcTime)
                } else {
                    null
                }
            }
            _bookedSlots.value = booked
        } catch (e: Exception) {
            _bookedSlots.value = emptyList()
        }
    }

    // 🌟 Hàm bookAppointment duy nhất: Kiểm tra trùng và lưu ID
    fun bookAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            try {
                // 1. Kiểm tra conflict (Fresh Check)
                val checkSnapshot = db.collection("Appointments")
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                    .whereNotEqualTo("status", "CANCELLED")
                    .get().await()

                if (!checkSnapshot.isEmpty) {
                    _bookingState.value = BookingState.Conflict
                    return@launch
                }

                // 2. Lưu trực tiếp qua Firestore để lấy Document ID ngay lập tức
                val docRef = db.collection("Appointments").add(appointment).await()
                currentAppointmentId = docRef.id

                _bookingState.value = BookingState.Success
            } catch (e: Exception) {
                _bookingState.value = BookingState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    // 🌟 Hàm cập nhật trạng thái sau khi thanh toán
    fun confirmAppointmentStatus(status: String) {
        val id = currentAppointmentId ?: return
        viewModelScope.launch {
            try {
                db.collection("Appointments").document(id)
                    .update("status", status).await()
                currentAppointmentId = null // Xong việc thì reset ID
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi confirm: ${e.message}")
            }
        }
    }

    // 🌟 Hàm hủy nếu người dùng thoát thanh toán
    fun cancelPendingAppointment() {
        val id = currentAppointmentId ?: return
        viewModelScope.launch {
            try {
                db.collection("Appointments").document(id).delete().await()
                currentAppointmentId = null
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi cancel: ${e.message}")
            }
        }
    }

    fun resetState() {
        _bookingState.value = BookingState.Idle
    }
}