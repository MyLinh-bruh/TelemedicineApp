package com.example.telemedicineapp.presentation.screen.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.DoctorRepository
import com.example.telemedicineapp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DoctorViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository
) : ViewModel() {

    // Danh sách cho Admin (Tất cả bác sĩ)
    private val _allDoctors = MutableStateFlow<List<User>>(emptyList())
    val allDoctors: StateFlow<List<User>> = _allDoctors

    // Danh sách cho Bệnh Nhân (Chỉ bác sĩ đã duyệt)
    private val _doctors = MutableStateFlow<List<User>>(emptyList())
    val doctors: StateFlow<List<User>> = _doctors

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchAllDoctors()
        fetchApprovedDoctors()
    }

    private fun fetchAllDoctors() {
        viewModelScope.launch {
            doctorRepository.getDoctorsStream()
                .catch { error -> /* Handle error */ }
                .collect { doctorList ->
                    _allDoctors.value = doctorList
                }
        }
    }

    private fun fetchApprovedDoctors() {
        viewModelScope.launch {
            doctorRepository.getApprovedDoctorsStream()
                .catch { error -> /* Handle error */ }
                .collect { doctorList ->
                    _doctors.value = doctorList
                }
        }
    }

    fun approveDoctor(doctor: User) {
        viewModelScope.launch {
            try {
                db.collection("Users")
                    .document(doctor.id)
                    .update("doctorStatus", "APPROVED")
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Trong presentation/screen/doctor/DoctorViewModel.kt
    fun rejectDoctor(doctor: User) {
        viewModelScope.launch {
            try {
                // Admin ra lệnh xóa đơn trực tiếp trên Firebase
                val success = doctorRepository.rejectAndRemoveDoctor(doctor.id)
                if (success) {
                    println("Admin đã xóa đơn đăng ký của: ${doctor.name}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}