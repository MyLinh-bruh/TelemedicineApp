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

    private val _doctors = MutableStateFlow<List<User>>(emptyList())
    val doctors: StateFlow<List<User>> = _doctors

    private val db = FirebaseFirestore.getInstance()

    init {
        fetchDoctors()
    }

    private fun fetchDoctors() {
        viewModelScope.launch {
            // Đảm bảo DoctorRepository.getDoctorsStream() của bạn
            // hiện tại đang query từ collection "Users" với điều kiện role == "DOCTOR"
            doctorRepository.getDoctorsStream()
                .catch { e ->
                    // Xử lý lỗi nếu cần
                    e.printStackTrace()
                }
                .collect { doctorList ->
                    _doctors.value = doctorList
                }
        }
    }

    // --- HÀM PHÊ DUYỆT BÁC SĨ (ĐÃ CẬP NHẬT: CHỈ DÙNG BẢNG USERS) ---
    fun approveDoctor(doctor: User) {
        viewModelScope.launch {
            try {
                // Vì không còn collection "Doctor", ta chỉ cần cập nhật trạng thái
                // của chính Document đó trong collection "Users"
                db.collection("Users")
                    .document(doctor.id)
                    .update("doctorStatus", "APPROVED")
                    .await()

                // Sau khi update thành công trên Firebase,
                // getDoctorsStream() sẽ tự động nhận diện thay đổi và cập nhật UI Admin
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}