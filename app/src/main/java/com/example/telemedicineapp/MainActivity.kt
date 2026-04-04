package com.example.telemedicineapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.telemedicineapp.model.Doctor
import com.example.telemedicineapp.ui.screens.DoctorDetailScreen
import com.example.telemedicineapp.ui.screens.DoctorListScreen
import com.example.telemedicineapp.ui.theme.TelemedicineAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TelemedicineAppTheme {
                var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = Color(0xFFF8FAFC)
                    ) {
                        if (selectedDoctor == null) {
                            DoctorListScreen(onDoctorClick = { doctor ->
                                selectedDoctor = doctor
                            })
                        } else {
                            DoctorDetailScreen(
                                doctor = selectedDoctor!!,
                                onBack = { selectedDoctor = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

