package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import androidx.lifecycle.ViewModel
import com.example.telemedicineapp.model.Appointment
import com.example.telemedicineapp.model.DoctorSchedule
import com.example.telemedicineapp.model.MedicalRecord
import com.example.telemedicineapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DoctorDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // --- 1. HỒ SƠ BÁC SĨ ---
    private val _doctorProfile = MutableStateFlow<User?>(null)
    val doctorProfile: StateFlow<User?> = _doctorProfile.asStateFlow()

    fun fetchDoctorProfile(doctorId: String) {
        db.collection("Users").document(doctorId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    _doctorProfile.value = snapshot.toObject(User::class.java)
                }
            }
    }

    fun updateProfile(user: User, onComplete: (Boolean) -> Unit) {
        db.collection("Users").document(user.id).set(user)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- 2. LỊCH KHÁM (CALENDAR) ---
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

    // --- 3. DANH SÁCH BỆNH ÁN ---
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

    // --- 4. LỊCH NGHỈ (BUSY SCHEDULE) ---
    private val _busySchedules = MutableStateFlow<List<DoctorSchedule>>(emptyList())
    val busySchedules: StateFlow<List<DoctorSchedule>> = _busySchedules.asStateFlow()

    private val _availableSlotsForSetup = MutableStateFlow(
        listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")
    )
    val availableSlotsForSetup: StateFlow<List<String>> = _availableSlotsForSetup.asStateFlow()

    // 🌟 ĐÃ FIX: Lấy Document ID để hàm xóa không bị lỗi
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
            "08:00", "09:00", "10:00", "11:00",
            "13:00", "14:00", "15:00", "16:00"
        )
    }

    fun saveBusyRange(
        doctorId: String,
        startDate: String,
        endDate: String,
        slots: List<String>,
        onComplete: (Boolean) -> Unit
    ) {
        val data = hashMapOf(
            "doctorId" to doctorId,
            "date" to startDate,
            "busySlots" to slots
        )
        db.collection("DoctorSchedules").add(data)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteBusySchedule(scheduleId: String, onComplete: (Boolean) -> Unit) {
        db.collection("DoctorSchedules").document(scheduleId).delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- 5. KÍCH HOẠT TẤT CẢ ---
    fun listenToData(doctorId: String) {
        fetchDoctorProfile(doctorId)
        fetchPatientRecords(doctorId)
        fetchBusySchedules(doctorId)
        fetchCalendarData(doctorId)
    }
}