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
    private val tokenManager: TokenManager
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState

    private val _doctorSchedule = MutableStateFlow<DoctorSchedule?>(null)
    val doctorSchedule: StateFlow<DoctorSchedule?> = _doctorSchedule

    private val _bookedSlots = MutableStateFlow<List<String>>(emptyList())
    val bookedSlots: StateFlow<List<String>> = _bookedSlots

    private var currentPendingAppointmentId: String? = null

    // 🌟 KHUNG GIỜ MẶC ĐỊNH ĐỂ PHÒNG HỜ BÁC SĨ CHỈ LƯU BUSY SLOTS MÀ QUÊN LƯU SLOTS KHÁM
    private val defaultMorning = listOf("08:00 - 08:30", "08:30 - 09:00", "09:00 - 09:30", "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00", "11:00 - 11:30")
    private val defaultAfternoon = listOf("13:00 - 13:30", "13:30 - 14:00", "14:00 - 14:30", "14:30 - 15:00", "15:00 - 15:30", "15:30 - 16:00", "16:00 - 16:30")

    fun getSchedulesAndAppointments(doctorId: String, date: String) {
        viewModelScope.launch {
            try {
                val localDate = java.time.LocalDate.parse(date)
                val dayOfWeek = localDate.dayOfWeek.value

                val specificSnapshot = db.collection("DoctorSchedules")
                    .whereEqualTo("doctorId", doctorId)
                    .whereEqualTo("date", date)
                    .get().await()

                if (!specificSnapshot.isEmpty) {
                    val schedule = specificSnapshot.documents[0].toObject(DoctorSchedule::class.java)
                    if (schedule != null) {
                        // 🌟 FIX LỖI: NẾU TRỐNG LỊCH (Do chỉ lưu lịch bận), LẤY LỊCH MẶC ĐỊNH LẤP VÀO
                        val mMorning = schedule.morningSlots.ifEmpty { defaultMorning }
                        val mAfternoon = schedule.afternoonSlots.ifEmpty { defaultAfternoon }
                        _doctorSchedule.value = schedule.copy(morningSlots = mMorning, afternoonSlots = mAfternoon)
                    }
                } else {
                    val doctorDefaultSnapshot = db.collection("DoctorDefaults")
                        .whereEqualTo("doctorId", doctorId)
                        .whereEqualTo("dayOfWeek", dayOfWeek)
                        .get().await()

                    if (!doctorDefaultSnapshot.isEmpty) {
                        val docData = doctorDefaultSnapshot.documents[0]
                        val mMorning = (docData.get("morningSlots") as? List<String> ?: emptyList()).ifEmpty { defaultMorning }
                        val mAfternoon = (docData.get("afternoonSlots") as? List<String> ?: emptyList()).ifEmpty { defaultAfternoon }

                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            doctorId = doctorId,
                            morningSlots = mMorning,
                            afternoonSlots = mAfternoon
                        )
                    } else {
                        _doctorSchedule.value = DoctorSchedule(
                            date = date,
                            doctorId = doctorId,
                            morningSlots = defaultMorning,
                            afternoonSlots = defaultAfternoon
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

                if (utcTime in startOfDay..endOfDay && (status == "PENDING" || status == "PAID" || status == "COMPLETED")) {
                    TimeUtils.extractSlotFromUtc(utcTime)
                } else null
            }
            _bookedSlots.value = booked
        } catch (e: Exception) {
            Log.e("AppointmentVM", "Lỗi fetchBookedSlots: ${e.message}")
            _bookedSlots.value = emptyList()
        }
    }

    fun bookAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading
            try {
                val checkSnapshot = db.collection("Appointments")
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                    .whereIn("status", listOf("PENDING", "PAID"))
                    .get().await()

                if (!checkSnapshot.isEmpty) {
                    _bookingState.value = BookingState.Conflict
                    return@launch
                }

                val newDocRef = db.collection("Appointments").document()
                val safeEmail = tokenManager.getEmail() ?: appointment.patientId
                val appointmentWithIdAndEmail = appointment.copy(
                    id = newDocRef.id,
                    patientId = safeEmail
                )

                newDocRef.set(appointmentWithIdAndEmail).await()
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

    // 🌟 HÀM NÀY SẼ DÙNG KHI BẤM NÚT "HỦY GIAO DỊCH" (XÓA LUÔN LỊCH)
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