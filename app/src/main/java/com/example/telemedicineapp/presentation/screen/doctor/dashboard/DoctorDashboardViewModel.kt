package com.example.telemedicineapp.presentation.screen.doctor.dashboard

import android.util.Log
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
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DoctorDashboardViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _doctorProfile = MutableStateFlow<User?>(null)
    val doctorProfile: StateFlow<User?> = _doctorProfile.asStateFlow()

    private val _markedDays = MutableStateFlow<List<String>>(emptyList())
    val markedDays: StateFlow<List<String>> = _markedDays.asStateFlow()

    private val _appointments = MutableStateFlow<List<Appointment>>(emptyList())
    val appointments: StateFlow<List<Appointment>> = _appointments.asStateFlow()

    private val _patientRecords = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val patientRecords: StateFlow<List<MedicalRecord>> = _patientRecords.asStateFlow()

    private val _busySchedules = MutableStateFlow<List<DoctorSchedule>>(emptyList())
    val busySchedules: StateFlow<List<DoctorSchedule>> = _busySchedules.asStateFlow()

    private val _availableSlotsForSetup = MutableStateFlow(
        listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")
    )
    val availableSlotsForSetup: StateFlow<List<String>> = _availableSlotsForSetup.asStateFlow()

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
                        Log.e("DOCTOR_ERROR", "Lỗi tải hồ sơ: ${e.message}")
                    }
                }
            }
    }

    // 2. Cập nhật hồ sơ bác sĩ
    fun updateProfile(user: User, onComplete: (Boolean) -> Unit) {
        db.collection("Users").document(user.id).set(user, SetOptions.merge())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // 3. Quản lý lịch hẹn (Calendar)
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

    // 4. Lấy danh sách bệnh án (Sắp xếp theo giờ mới nhất)
    fun fetchPatientRecords(doctorId: String) {
        db.collection("MedicalRecords")
            .whereEqualTo("doctorId", doctorId)
            .orderBy("lastUpdated", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    _patientRecords.value = snapshot.toObjects(MedicalRecord::class.java)
                }
            }
    }

    // 🌟 5. HÀM LƯU HỒ SƠ MỚI (FIXED: Nằm trong class, tạo ID mới)
    // 🌟 HÀM LƯU HỒ SƠ MỚI (Đã sửa để tạo hồ sơ riêng biệt cho mỗi lần khám)
    fun saveMedicalRecord(
        record: MedicalRecord,
        appointmentId: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val newRecordRef = db.collection("MedicalRecords").document()

                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val currentTime = sdf.format(Date())

                val finalRecord = record.copy(
                    id = newRecordRef.id,
                    appointmentId = appointmentId, // 🌟 QUAN TRỌNG
                    lastUpdated = currentTime
                )

                val batch = db.batch()
                batch.set(newRecordRef, finalRecord)

                // update đúng lịch hẹn
                if (appointmentId.isNotBlank()) {
                    val apptRef = db.collection("Appointments").document(appointmentId)
                    batch.update(apptRef, "status", "COMPLETED")
                }

                batch.commit().await()
                onComplete(true)

            } catch (e: Exception) {
                Log.e("SAVE_RECORD_ERROR", "Lỗi: ${e.message}")
                onComplete(false)
            }
        }
    }

    // 6. QUẢN LÝ LỊCH BẬN
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
                val batch = db.batch()

                var current = start
                while (!current.isAfter(end)) {
                    val dateStr = current.toString()
                    val docRef = db.collection("DoctorSchedules").document()
                    val data = hashMapOf(
                        "doctorId" to doctorId,
                        "date" to dateStr,
                        "busySlots" to slots
                    )
                    batch.set(docRef, data)
                    current = current.plusDays(1)
                }
                batch.commit().await()
                onComplete(true)
            } catch (e: Exception) {
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