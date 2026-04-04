package com.example.telemedicineapp.ui.screens
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telemedicineapp.model.Doctor
import com.example.telemedicineapp.ui.components.DoctorItem
data class PendingRequest(
    val id: String,
    val name: String,
    val specialty: String,
    val experience: String,
    val hospital: String,
    val phone: String,
    val certId: String
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    allDoctors: List<Doctor>,
    onDoctorClick: (Doctor) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedRequest by remember { mutableStateOf<PendingRequest?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("Toàn quốc") }
    var selectedSpecialty by remember { mutableStateOf("Tất cả") }
    var expandedLocation by remember { mutableStateOf(false) }
    var expandedSpecialty by remember { mutableStateOf(false) }
    val locations = listOf("Toàn quốc", "Hà Nội", "Đà Nẵng", "TP. HCM")
    val specialties = listOf("Tất cả", "Tim mạch", "Nhi khoa", "Da liễu")
    val requests = remember {
        listOf(
            PendingRequest(
                "1",
                "BS. Nguyễn Văn An",
                "Tim mạch",
                "10 năm",
                "BV Bạch Mai",
                "0901234567",
                "CCHN-123"
            ),
            PendingRequest(
                "2",
                "BS. Lê Thị Bình",
                "Nhi khoa",
                "8 năm",
                "BV Nhi TW",
                "0987654321",
                "CCHN-456"
            )
        )
    }
    val filteredDoctors = allDoctors.filter { doctor ->
        val matchLoc = selectedLocation == "Toàn quốc" || doctor.address.contains(selectedLocation)
        val matchSpec = selectedSpecialty == "Tất cả" || doctor.specialty == selectedSpecialty
        val matchName = doctor.name.contains(searchQuery, ignoreCase = true)
        matchLoc && matchSpec && matchName
    }
}