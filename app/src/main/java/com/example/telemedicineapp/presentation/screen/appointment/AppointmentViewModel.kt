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

    // BIẾN LƯU TRỮ DOCUMENT ID: Dùng để Xóa hoặc Cập nhật chính xác lịch hẹn vừa tạo
    private var currentPendingAppointment: Appointment? = null

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

    // Hàm thực hiện lưu lịch hẹn mới (Giữ chỗ)
    fun bookAppointment(appointment: Appointment) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading

            try {
                // 1. Kiểm tra "tươi" (Fresh Check) trên server xem slot này còn trống không
                val checkSnapshot = db.collection("Appointments")
                    .whereEqualTo("doctorId", appointment.doctorId)
                    .whereEqualTo("dateTimeUtc", appointment.dateTimeUtc)
                    .whereNotEqualTo("status", "CANCELLED")
                    .get().await()

                if (!checkSnapshot.isEmpty) {
                    // 409 Conflict: Đã có người đặt rồi!
                    _bookingState.value = BookingState.Conflict
                    return@launch
                }

                // 2. TẠO DOCUMENT MỚI TRƯỚC ĐỂ LẤY ID
                val newDocRef = db.collection("Appointments").document()
                val newId = newDocRef.id // Đây chính là chuỗi "2BtOk..." ngẫu nhiên

                // 3. COPY ĐỐI TƯỢNG VÀ GẮN ID VÀO
                // LƯU Ý: Nếu data class Appointment của bạn không khai báo biến id,
                // hãy chắc chắn rằng nó có thuộc tính var id: String = ""
                val appointmentToSave = appointment.copy(id = newId)

                // 4. LƯU LẠI VÀO BIẾN TẠM ĐỂ DÙNG LÚC HỦY/THÀNH CÔNG
                currentPendingAppointment = appointmentToSave

                // 5. LƯU THẲNG LÊN FIREBASE BẰNG ĐÚNG CÁI ID ĐÓ (Không thông qua Repository)
                newDocRef.set(appointmentToSave).await()

                // Thông báo UI chuyển sang hiển thị QR/Thanh toán
                _bookingState.value = BookingState.Success

            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi bookAppointment: ${e.message}")
                _bookingState.value = BookingState.Error(e.message ?: "Lỗi không xác định")
            }
        }
    }

    // =====================================================================
    // CÁC HÀM XỬ LÝ THANH TOÁN (HỦY SLOT HOẶC CHỐT SLOT BẰNG DOCUMENT ID)
    // =====================================================================

    // Gọi khi: Hết giờ thanh toán HOẶC Bệnh nhân bấm "Hủy giao dịch"
    fun cancelPendingAppointment() {
        val apptId = currentPendingAppointment?.id

        if (apptId.isNullOrEmpty()) return // Không có ID thì dừng ngay

        viewModelScope.launch {
            try {
                // Chỉ thẳng mặt Document ID đó và XÓA khỏi Database để nhả Slot
                db.collection("Appointments").document(apptId).delete().await()

                Log.d("AppointmentVM", "Đã xóa thành công slot bị hủy: $apptId")
                currentPendingAppointment = null // Reset biến tạm
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi khi xóa Slot: ${e.message}")
            }
        }
    }

    // Gọi khi: Thanh toán thành công (Stripe hoặc QR Mocking)
    fun confirmAppointmentStatus(newStatus: String) {
        val apptId = currentPendingAppointment?.id

        if (apptId.isNullOrEmpty()) return // Không có ID thì dừng ngay

        viewModelScope.launch {
            try {
                // Chỉ thẳng mặt Document ID đó và ĐỔI trạng thái (VD: Thành "PAID")
                db.collection("Appointments").document(apptId).update("status", newStatus).await()

                Log.d("AppointmentVM", "Đã cập nhật trạng thái thành $newStatus cho: $apptId")
                currentPendingAppointment = null // Reset biến tạm
            } catch (e: Exception) {
                Log.e("AppointmentVM", "Lỗi khi cập nhật trạng thái: ${e.message}")
            }
        }
    }

    // Reset lại trạng thái UI sau khi hiện thông báo
    fun resetState() {
        _bookingState.value = BookingState.Idle
    }
}