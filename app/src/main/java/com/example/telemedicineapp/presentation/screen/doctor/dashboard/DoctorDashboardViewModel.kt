package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.UserEntity
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.model.DoctorSchedule
import com.example.telemedicineapp.model.MedicalRecord
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.model.Role
import com.example.telemedicineapp.model.DoctorStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate // Cần thiết để xử lý vòng lặp ngày
import javax.inject.Inject

@HiltViewModel
class DoctorDashboardViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _doctorProfile = MutableStateFlow<User?>(null)
    val doctorProfile: StateFlow<User?> = _doctorProfile.asStateFlow()

    // 1. Lấy thông tin hồ sơ bác sĩ
    fun fetchDoctorProfile(doctorId: String) {
        db.collection("Users").document(doctorId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val entity = snapshot.toObject(UserEntity::class.java)
                        if (entity != null) {
                            val role = try { Role.valueOf(entity.role.uppercase()) } catch (e: Exception) { Role.DOCTOR }
                            val status = try { DoctorStatus.valueOf(entity.doctorStatus.uppercase()) } catch (e: Exception) { DoctorStatus.APPROVED }

                            _doctorProfile.value = User(
                                id = snapshot.id,
                                email = entity.email,
                                name = entity.name,
                                role = role,
                                doctorStatus = status,
                                specialty = entity.specialty,
                                hospitalName = entity.hospitalName,
                                imageUrl = entity.imageUrl,
                                certificateUrl = entity.certificateUrl,
                                description = entity.description,
                                address = entity.address,
                                bankName = entity.bankName,
                                bankAccountNumber = entity.bankAccountNumber
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DOCTOR_ERROR", "Lỗi tải hồ sơ: ${e.message}")
                    }
                }
            }
    }

    // 2. Cập nhật hồ sơ (sử dụng merge để tránh mất password)
    fun updateProfile(user: User, onComplete: (Boolean) -> Unit) {
        db.collection("Users").document(user.id).set(user, SetOptions.merge())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // 3. Quản lý lịch hẹn (Calendar)
    private val _markedDays = MutableStateFlow<List<String>>(emptyList())
    val markedDays: StateFlow<List<String>> = _markedDays.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    fun fetchCalendarData(doctorId: String) {
        db.collection("Appointments")
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.toObjects(Appointment::class.java)
                    _appointments.value = list
                    _markedDays.value = list.map { it.dateTimeUtc.split("T")[0] }.distinct()
                }
            }
    }

    // 4. Lấy danh sách bệnh án đã khám
    private val _patientRecords = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val patientRecords: StateFlow<List<MedicalRecord>> = _patientRecords.asStateFlow()

    fun fetchPatientRecords(doctorId: String) {
        db.collection("MedicalRecords")
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _patientRecords.value = snapshot.toObjects(MedicalRecord::class.java)
                }
            }
    }

    // 5. QUẢN LÝ LỊCH BẬN (BUSY SCHEDULE)
    private val _busySchedules = MutableStateFlow<List<DoctorSchedule>>(emptyList())
    val busySchedules: StateFlow<List<DoctorSchedule>> = _busySchedules.asStateFlow()

    private val _availableSlotsForSetup = MutableStateFlow(
        listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")
    )
    val availableSlotsForSetup: StateFlow<List<String>> = _availableSlotsForSetup.asStateFlow()

    fun fetchBusySchedules(doctorId: String) {
        db.collection("DoctorSchedules")
            .whereEqualTo("doctorId", doctorId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val schedule = doc.toObject(DoctorSchedule::class.java)
                        schedule?.copy(id = doc.id)
                    }
                    _busySchedules.value = list
                }
            }
    }

    fun loadSlotsForDate(date: String) {
        _availableSlotsForSetup.value = listOf(
            "08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00"
        )
    }

    /**
     * FIX CHÍNH: Hàm khóa lịch nhiều ngày sử dụng WriteBatch
     * Giúp khóa từ ngày 14 đến 16 chỉ với 1 lần nhấn nút.
     */
    fun saveBusyRange(
        doctorId: String,
        startDate: String,
        endDate: String,
        slots: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val start = LocalDate.parse(startDate)
                val end = LocalDate.parse(endDate)

                // Sử dụng WriteBatch để tối ưu hóa hiệu suất và đảm bảo tính nguyên tử
                val batch = db.batch()

                var current = start
                while (!current.isAfter(end)) {
                    val dateStr = current.toString() // Trả về định dạng yyyy-MM-dd

                    // Tạo document mới tự động ID cho mỗi ngày trong khoảng
                    val docRef = db.collection("DoctorSchedules").document()

                    val data = hashMapOf(
                        "doctorId" to doctorId,
                        "date" to dateStr,
                        "busySlots" to slots
                    )
                    batch.set(docRef, data)

                    current = current.plusDays(1) // Chuyển sang ngày tiếp theo
                }

                batch.commit().await()
                onComplete(true)
            } catch (e: Exception) {
                android.util.Log.e("SAVE_ERROR", "Lỗi khóa lịch: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun deleteBusySchedule(scheduleId: String, onComplete: (Boolean) -> Unit) {
        db.collection("DoctorSchedules").document(scheduleId).delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun listenToData(doctorId: String) {
        fetchDoctorProfile(doctorId)
        fetchPatientRecords(doctorId)
        fetchBusySchedules(doctorId)
        fetchCalendarData(doctorId)
    }
}