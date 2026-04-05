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

    // 1. Trạng thái tiến trình đặt lịch (Idle/Loading/Success/Error)
    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState

    // 2. Lịch rảnh của bác sĩ (Lấy từ DoctorSchedules)
    private val _doctorSchedule = MutableStateFlow<DoctorSchedule?>(null)
    val doctorSchedule: StateFlow<DoctorSchedule?> = _doctorSchedule

    // 3. Danh sách các Slot đã có người đặt (Lấy từ Appointments)
    private val _bookedSlots = MutableStateFlow<List<String>>(emptyList())
    val bookedSlots: StateFlow<List<String>> = _bookedSlots

    fun getSchedulesAndAppointments(doctorId: String, date: String) {
        viewModelScope.launch {
            val localDate = java.time.LocalDate.parse(date)
            val dayOfWeek = localDate.dayOfWeek.value // 1 (Thứ 2) -> 7 (CN)

            try {
                // Bước 1: Tìm lịch "Sửa đổi/Bận" (DoctorSchedules)
                val specificSnapshot = db.collection("DoctorSchedules")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("date", date)
                    .get().await()

                if (!specificSnapshot.isEmpty) {
                    _doctorSchedule.value = specificSnapshot.documents[0].toObject(DoctorSchedule::class.java)
                } else {
                    // Bước 2: Tìm lịch "Mặc định của Bác sĩ" (DoctorDefaults)
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
                        // Bước 3: Lấy "Mặc định của App" (Hardcode hoặc từ config)
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            morningSlots = listOf("08:00 - 09:00", "09:00 - 10:00", "10:00 - 11:00"),
                            afternoonSlots = listOf("14:00 - 15:00", "15:00 - 16:00")
                        )
                    }
                }

                // Bước 4: Lấy các lịch đã được bệnh nhân đặt để hiện màu đỏ
                fetchBookedSlots(doctorId, date)

            } catch (e: Exception) {
                _doctorSchedule.value = null
            }
        }
    }

    private suspend fun fetchDoctorSchedule(doctorId: String, date: String) {
        try {
            val snapshot = db.collection("DoctorSchedules")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", date)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val schedule = snapshot.documents[0].toObject(DoctorSchedule::class.java)
                _doctorSchedule.value = schedule
            } else {
                // Nếu không có cấu hình, trả về object rỗng để UI không bị lỗi
                _doctorSchedule.value = DoctorSchedule(date = date, doctorId = doctorId)
            }
        } catch (e: Exception) {
            Log.e("AppointmentVM", "Lỗi fetchDoctorSchedule: ${e.message}")
            _doctorSchedule.value = null
        }
    }

    private suspend fun fetchBookedSlots(doctorId: String, date: String) {
        try {
            // Tạo khoảng thời gian từ đầu ngày đến cuối ngày theo chuẩn ISO-8601
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
                // Trạng thái đơn có thể là PENDING hoặc CONFIRMED đều coi là đã giữ chỗ
                val status = doc.getString("status") ?: ""
                if (status != "CANCELLED") {
                    TimeUtils.extractSlotFromUtc(utcTime)
                } else {
                    null
                }
            }
            _bookedSlots.value = booked
        } catch (e: Exception) {
            Log.e("AppointmentVM", "Lỗi fetchBookedSlots: ${e.message}")
            _bookedSlots.value = emptyList()
        }
    }

    // Hàm thực hiện lưu lịch hẹn mới
    fun bookAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            val success = appointmentRepository.createAppointment(appointment)
            if (success) {
                _bookingState.value = BookingState.Success
            } else {
                _bookingState.value = BookingState.Error("Không thể đặt lịch. Vui lòng thử lại!")
            }
        }
    }

    // Reset lại trạng thái sau khi hiện thông báo
    fun resetState() {
        _bookingState.value = BookingState.Idle
    }
}