package com.example.telemedicineapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.telemedicineapp.model.MedicalRecord
import com.example.telemedicineapp.presentation.screen.patient.PatientMedicalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientMedicalRecordScreen(
    patientId: String,
    onBack: () -> Unit,
    onRecordClick: (MedicalRecord) -> Unit,
    viewModel: PatientMedicalViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Tự động tải dữ liệu khi ID bệnh nhân thay đổi hoặc khi vào màn hình
    LaunchedEffect(patientId) {
        viewModel.fetchMyRecords(patientId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ sơ bệnh án", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            // Hiện vòng xoay khi đang tải dữ liệu
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (records.isEmpty()) {
            // Hiện thông báo nếu chưa có hồ sơ nào
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Bạn chưa có hồ sơ bệnh án nào.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            // Hiện danh sách hồ sơ
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Khoảng cách giữa các card
            ) {
                items(records) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecordClick(record) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Ngày khám: ${record.lastUpdated}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Chẩn đoán: ${record.diagnosis}", maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Nhấn để xem chi tiết đơn thuốc và lời dặn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}