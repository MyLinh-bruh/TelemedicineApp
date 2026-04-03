package com.example.telemedicineapp.model

data class Doctor(
    val id: Int,
    val name: String,
    val specialty: String,
    val rating: Double,
    val imageUrl: String,
    val description: String,
    val address: String,
    val hospitalName: String
)
