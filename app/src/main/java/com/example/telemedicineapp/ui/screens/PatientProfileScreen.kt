package com.example.telemedicineapp.presentation.screen.ui.screens

import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.telemedicineapp.model.User
import com.example.telemedicineapp.presentation.screen.auth.ProfileViewModel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

fun uriToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        // Nén 50% để không quá nặng database
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        Base64.encodeToString(byteArray, Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }
}
// Hàm hỗ trợ hiển thị ảnh từ chuỗi Base64 hoặc URL thường
@Composable
fun rememberProfileImage(imageUrl: String): Any {
    return remember(imageUrl) {
        if (imageUrl.isEmpty()) {
            "https://cdn-icons-png.flaticon.com/512/149/149071.png"
        } else if (imageUrl.startsWith("http")) {
            imageUrl
        } else {
            // Nếu là chuỗi Base64, giải mã thành mảng byte để hiển thị
            try {
                Base64.decode(imageUrl, Base64.DEFAULT)
            } catch (e: Exception) {
                "https://cdn-icons-png.flaticon.com/512/149/149071.png"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(navController: NavController, viewModel: ProfileViewModel = hiltViewModel()) {
    var isEditing by remember { mutableStateOf(false) }
    val user = viewModel.userState
    val hasInfo = user != null && user.phone.isNotEmpty() && user.address.isNotEmpty()

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HỒ SƠ CÁ NHÂN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (!hasInfo && !isEditing) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(160.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Icon(Icons.Outlined.Person, null, modifier = Modifier.padding(40.dp).fillMaxSize(), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Thông tin còn trống", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Cập nhật hồ sơ để bác sĩ có thể hỗ trợ bạn tốt nhất",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { isEditing = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("CẬP NHẬT NGAY", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isEditing) {
                ProfileForm(user ?: User(), viewModel) { isEditing = false }
            } else {
                ProfileDetail(user!!, onEdit = { isEditing = true })
            }
        }
    }
}

@Composable
fun ProfileForm(user: User, viewModel: ProfileViewModel, onSave: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var address by remember { mutableStateOf(user.address) }
    var gender by remember { mutableStateOf(user.gender) }
    var bloodType by remember { mutableStateOf(user.bloodType) }
    var medicalHistory by remember { mutableStateOf(user.medicalHistory) }

    // imageUri tạm thời để hiển thị khi người dùng vừa chọn ảnh xong
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { tempImageUri = it }

    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box {
                AsyncImage(
                    // Ưu tiên hiển thị Uri vừa chọn, nếu không có thì hiển thị dữ liệu từ Database (Base64/Url)
                    model = tempImageUri ?: rememberProfileImage(user.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .shadow(10.dp, CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        CustomTextField(value = name, onValueChange = { name = it }, label = "Họ tên *", icon = Icons.Outlined.Person)
        CustomTextField(value = phone, onValueChange = { phone = it }, label = "Số điện thoại *", icon = Icons.Outlined.Phone)
        CustomTextField(value = address, onValueChange = { address = it }, label = "Địa chỉ *", icon = Icons.Outlined.LocationOn)
        CustomTextField(value = gender, onValueChange = { gender = it }, label = "Giới tính *", icon = Icons.Outlined.Face)
        CustomTextField(value = bloodType, onValueChange = { bloodType = it }, label = "Nhóm máu", icon = Icons.Outlined.WaterDrop)
        CustomTextField(value = medicalHistory, onValueChange = { medicalHistory = it }, label = "Tiểu sử bệnh lý", icon = Icons.Outlined.History, isSingleLine = false)

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                // Gọi thẳng hàm save, việc biến ảnh thành chuỗi đã có Repository lo
                viewModel.saveProfile(
                    user.copy(
                        name = name,
                        phone = phone,
                        address = address,
                        gender = gender,
                        bloodType = bloodType,
                        medicalHistory = medicalHistory
                    ),
                    tempImageUri // Truyền Uri tạm vào đây
                ) {
                    onSave() // Callback khi thành công
                }

            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = name.isNotBlank() && phone.isNotBlank() && !viewModel.isSaving
        ) {
            if (viewModel.isSaving) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("LƯU THÔNG TIN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun ProfileDetail(user: User, onEdit: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().background(
                brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            ).padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = rememberProfileImage(user.imageUrl), // Gọi hàm giải mã Base64
                        contentDescription = null,
                        modifier = Modifier.size(85.dp).clip(CircleShape).border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text(user.name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.White)
                        Text(user.email, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                        Surface(
                            modifier = Modifier.padding(top = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text("Bệnh nhân", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                InfoRow("Điện thoại", user.phone, Icons.Outlined.Phone)
                InfoRow("Địa chỉ", user.address, Icons.Outlined.LocationOn)
                InfoRow("Giới tính", user.gender, Icons.Outlined.Face)
                InfoRow("Nhóm máu", user.bloodType, Icons.Outlined.WaterDrop, Color.Red)
                InfoRow("Tiểu sử bệnh", user.medicalHistory, Icons.Outlined.History)
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("CHỈNH SỬA HỒ SƠ", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector, iconColor: Color = MaterialTheme.colorScheme.primary) {
    Row(Modifier.padding(vertical = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = iconColor.copy(alpha = 0.1f)) {
            Icon(icon, null, modifier = Modifier.padding(10.dp), tint = iconColor)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value.ifEmpty { "Chưa cập nhật" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, isSingleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        singleLine = isSingleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.LightGray,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
