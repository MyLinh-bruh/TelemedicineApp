package com.example.telemedicineapp.data

import com.example.telemedicineapp.model.Appointment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor() {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createAppointment(appointment: Appointment): Boolean {
        return try {
            val docRef = db.collection("Appointments").document()
            val newAppointment = appointment.copy(id = docRef.id)

            docRef.set(newAppointment).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}