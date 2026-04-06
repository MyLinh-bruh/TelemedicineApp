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

    private val _allDoctors = MutableStateFlow<List<User>>(emptyList())
    val allDoctors: StateFlow<List<User>> = _allDoctors

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
                .catch { error -> /* Xử lý lỗi nếu cần */ }
                .collect { doctorList -> _allDoctors.value = doctorList }
        }
    }

    private fun fetchApprovedDoctors() {
        viewModelScope.launch {
            doctorRepository.getApprovedDoctorsStream()
                .catch { error -> /* Xử lý lỗi nếu cần */ }
                .collect { doctorList -> _doctors.value = doctorList }
        }
    }

    fun approveDoctor(doctor: User) {
        viewModelScope.launch {
            try {
                db.collection("Users").document(doctor.id)
                    .update("doctorStatus", "APPROVED").await()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun rejectDoctor(doctor: User) {
        viewModelScope.launch {
            try {
                doctorRepository.rejectAndRemoveDoctor(doctor.id)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteBusySchedule(doctorId: String, date: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("Users")
                    .document(doctorId)
                    .collection("BusySchedules")
                    .whereEqualTo("date", date)
                    .get()
                    .await()

                for (document in snapshot.documents) {
                    db.collection("Users")
                        .document(doctorId)
                        .collection("BusySchedules")
                        .document(document.id)
                        .delete()
                        .await()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}