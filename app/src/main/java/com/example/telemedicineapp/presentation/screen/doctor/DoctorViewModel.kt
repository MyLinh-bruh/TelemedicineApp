package com.example.telemedicineapp.presentation.screen.doctor // Tạo package nếu chưa có

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.DoctorRepository
import com.example.telemedicineapp.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DoctorViewModel @Inject constructor(
    private val doctorRepository: DoctorRepository
) : ViewModel() {

    private val _doctors = MutableStateFlow<List<User>>(emptyList())
    val doctors: StateFlow<List<User>> = _doctors

    init {
        fetchDoctors()
    }

    private fun fetchDoctors() {
        viewModelScope.launch {
            // Lắng nghe luồng dữ liệu từ Repository
            doctorRepository.getDoctorsStream()
                .catch { error ->
                    // Xử lý log lỗi nếu cần
                }
                .collect { doctorList ->
                    // Mỗi khi có data mới từ Firebase, update lại state
                    _doctors.value = doctorList
                }
        }
    }
}