package com.example.telemedicineapp.presentation.screen.appointment

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.core.TokenManager
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
    private val appointmentRepository: AppointmentRepository,
    private val tokenManager: TokenManager // 👈 THÊM TOKEN MANAGER
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // 1. Trạng thái tiến trình đặt lịch (Idle, Loading, Success, Conflict, Error)
    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState

    // 2. Lịch rảnh của bác sĩ
    private val _doctorSchedule = MutableStateFlow<DoctorSchedule?>(null)
    val doctorSchedule: StateFlow<DoctorSchedule?> = _doctorSchedule

    // 3. Danh sách các Slot đã có người đặt (Dùng để hiển thị màu đỏ/vô hiệu hóa nút)
    private val _bookedSlots = MutableStateFlow<List<String>>(emptyList())
    val bookedSlots: StateFlow<List<String>> = _bookedSlots

    // Biến lưu trữ lịch hẹn đang xử lý
    private var currentPendingAppointmentId: String? = null

    /**
     * Lấy lịch làm việc của bác sĩ và danh sách các slot đã bị đặt
     */
    fun getSchedulesAndAppointments(doctorId: String, date: String) {
        viewModelScope.launch {
            try {
                val localDate = java.time.LocalDate.parse(date)
                val dayOfWeek = localDate.dayOfWeek.value

                // Ưu tiên 1: Tìm lịch cụ thể cho ngày đó trong DoctorSchedules
                val specificSnapshot = db.collection("DoctorSchedules")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("date", date)
                    .get().await()

                if (!specificSnapshot.isEmpty) {
                    _doctorSchedule.value = specificSnapshot.documents[0].toObject(DoctorSchedule::class.java)
                } else {
                    // Ưu tiên 2: Lấy lịch mặc định theo thứ trong tuần từ DoctorDefaults
                    val doctorDefaultSnapshot = db.collection("DoctorDefaults")
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("dayOfWeek", dayOfWeek)
                        .get().await()

                    if (!doctorDefaultSnapshot.isEmpty) {
                        val docData = doctorDefaultSnapshot.documents[0]
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            doctorId = doctorId,
                            morningSlots = docData.get("morningSlots") as? List<String> ?: emptyList(),
                            afternoonSlots = docData.get("afternoonSlots") as? List<String> ?: emptyList()
                        )
                    } else {
                        // Ưu tiên 3: Giá trị mặc định cứng nếu không cấu hình gì cả
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            doctorId = doctorId,
                            morningSlots = listOf("08:00 - 09:00", "09:00 - 10:00", "10:00 - 11:00"),
                            afternoonSlots = listOf("14:00 - 15:00", "15:00 - 16:00")
                        )
                    }
                }
                fetchBookedSlots(doctorId, date)
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi getSchedules: ${e.message}")
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
                .get()
                .await()

            val booked = snapshot.documents.mapNotNull { doc ->
                val utcTime = doc.getString("dateTimeUtc") ?: return@mapNotNull null
                val status = doc.getString("status") ?: ""

                if (utcTime in startOfDay..endOfDay && (status == "PENDING" || status == "PAID")) {
                    TimeUtils.extractSlotFromUtc(utcTime)
                } else null
            }
            _bookedSlots.value = booked
        } catch (e: Exception) {
            Log.e("AppointmentVM", "Lỗi fetchBookedSlots: ${e.message}")
            _bookedSlots.value = emptyList()
        }
    }

    /**
     * Thực hiện giữ chỗ (book) lịch hẹn với trạng thái PENDING
     */
    fun bookAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            try {
                // 1. Kiểm tra Fresh Check
                val checkSnapshot = db.collection("Appointments")
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                    .whereIn("status", listOf("PENDING", "PAID"))
                    .get().await()

                if (!checkSnapshot.isEmpty) {
                    _bookingState.value = BookingState.Conflict
                    return@launch
                }

                // 2. Tạo ID mới
                val newDocRef = db.collection("Appointments").document()

                // 3. ÉP CẬP NHẬT patientId THÀNH EMAIL ĐANG ĐĂNG NHẬP (An toàn tuyệt đối)
                val safeEmail = tokenManager.getEmail() ?: appointment.patientId
                val appointmentWithIdAndEmail = appointment.copy(
                    id = newDocRef.id,
                    patientId = safeEmail // 👈 LUÔN LUÔN LƯU BẰNG EMAIL
                )

                newDocRef.set(appointmentWithIdAndEmail).await()

                // 4. Lưu lại ID tạm thời
                currentPendingAppointmentId = appointmentWithIdAndEmail.id
                _bookingState.value = BookingState.Success

            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi bookAppointment: ${e.message}")
                _bookingState.value = BookingState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    fun confirmAppointmentStatus(newStatus: String) {
        val apptId = currentPendingAppointmentId ?: return
        viewModelScope.launch {
            try {
                db.collection("Appointments").document(apptId)
                    .update("status", newStatus).await()
                Log.d("AppointmentVM", "Cập nhật status $newStatus cho: $apptId")
                currentPendingAppointmentId = null
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi cập nhật trạng thái: ${e.message}")
            }
        }
    }

    fun cancelPendingAppointment() {
        val apptId = currentPendingAppointmentId ?: return
        viewModelScope.launch {
            try {
                db.collection("Appointments").document(apptId).delete().await()
                Log.d("AppointmentVM", "Đã xóa slot tạm thời: $apptId")
                currentPendingAppointmentId = null
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi khi xóa Slot: ${e.message}")
            }
        }
    }

    fun resetState() {
        _bookingState.value = BookingState.Idle
    }
}