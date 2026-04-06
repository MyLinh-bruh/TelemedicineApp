package com.example.telemedicineapp.presentation.screen.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.telemedicineapp.data.AuthRepository
import com.example.telemedicineapp.model.MedicalRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientMedicalViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _records = MutableStateFlow<List<MedicalRecord>>(emptyList())
    val records: StateFlow<List<MedicalRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchMyRecords(patientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            authRepo.getMedicalRecordsForPatient(patientId).collect {
                _records.value = it
                _isLoading.value = false
            }
        }
    }
}