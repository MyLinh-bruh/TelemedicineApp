package com.example.telemedicineapp.ui.components

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentQRDialog(
    amount: String,
    appointmentId: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit, // 🌟 Gọi khi bấm Back thiết bị / Bấm ra ngoài (Giữ lại lịch chờ thanh toán)
    onCancelTransaction: () -> Unit // 🌟 Gọi khi bấm nút Hủy giao dịch (Xóa thẳng lịch)
) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var selectedBankText by remember { mutableStateOf("Chọn ngân hàng của bạn...") }

    var timeLeft by remember { mutableIntStateOf(10 * 60) }
    var isChecking by remember { mutableStateOf(false) }
    var attemptCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = timeLeft, key2 = isChecking) {
        if (!isChecking && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        } else if (timeLeft == 0) {
            Toast.makeText(context, "Đã hết thời gian thanh toán!", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    LaunchedEffect(key1 = isChecking) {
        if (isChecking) {
            if (attemptCount == 1) {
                delay(3000L)
                Toast.makeText(context, "Chưa tìm thấy giao dịch. Vui lòng đợi thêm vài giây và thử lại!", Toast.LENGTH_LONG).show()
                isChecking = false
            } else {
                delay(2000L)
                onConfirm()
            }
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val bankId = "MB"
    val accountNo = "0123456789"
    val accountName = "NGUYEN VAN A"

    val qrUrl = "https://img.vietqr.io/image/$bankId-$accountNo-compact.png" +
            "?amount=$amount" +
            "&addInfo=Thanh toan $appointmentId" +
            "&accountName=$accountName"

    val banks = listOf(
        "MB Bank" to "mbmobile://",
        "Vietcombank" to "vietcombankmobile://",
        "Techcombank" to "tcb://",
        "BIDV" to "bidvsmartbanking://",
        "VietinBank" to "vietinbankmobile://",
        "Agribank" to "agribankmobile://",
        "ACB" to "acbapp://",
        "TPBank" to "tpbankmobile://",
        "VPBank" to "vpbankneo://",
        "VIB" to "vibmobile://",
        "Sacombank" to "sacombankpay://"
    )

    AlertDialog(
        onDismissRequest = { if (!isChecking) onDismiss() }, // 🌟 Hành vi khi bấm ra ngoài hoặc nút Back
        title = {
            Text(
                "Thanh toán qua VietQR",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "Vui lòng thanh toán trong: $timeString",
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text("Quét mã hoặc chọn ứng dụng bên dưới", fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))

                AsyncImage(
                    model = qrUrl,
                    contentDescription = "Mã QR",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Số tiền: $amount VND", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD32F2F), fontSize = 18.sp)
                Text("Nội dung: Thanh toan $appointmentId", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { downloadQRImage(context, qrUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                    enabled = !isChecking
                ) {
                    Text("⬇️ Tải ảnh QR xuống máy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Hoặc tự động lưu & Mở ứng dụng:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                PaymentAppButton("Mở bằng MoMo", isEnabled = !isChecking) {
                    saveQRAndOpenApp(
                        context = context,
                        imageUrl = qrUrl,
                        target = "com.mservice.momotransfer",
                        isPackage = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isChecking) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedBankText,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isChecking,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        banks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.first, fontSize = 14.sp) },
                                onClick = {
                                    selectedBankText = bank.first
                                    expanded = false
                                    saveQRAndOpenApp(context, qrUrl, bank.second, isPackage = false)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    attemptCount++
                    isChecking = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isChecking,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                if (isChecking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang kiểm tra GD...")
                } else {
                    Text("Tôi đã chuyển khoản")
                }
            }
        },
        dismissButton = {
            if (!isChecking) {
                // 🌟 GỌI HÀM XÓA KHI BẤM NÚT HỦY GIAO DỊCH NÀY
                TextButton(onClick = onCancelTransaction, modifier = Modifier.fillMaxWidth()) {
                    Text("Hủy giao dịch", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun PaymentAppButton(appName: String, isEnabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp),
        enabled = isEnabled
    ) {
        Text(appName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

fun downloadQRImage(context: Context, imageUrl: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(imageUrl))
            .setTitle("Mã QR Thanh Toán")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "QR_${System.currentTimeMillis()}.png")
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
        Toast.makeText(context, "Đã tải mã QR vào thư viện!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi khi tải ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun saveQRAndOpenApp(
    context: Context,
    imageUrl: String,
    target: String,
    isPackage: Boolean
) {
    try {
        val intent: Intent? = if (isPackage) {
            context.packageManager.getLaunchIntentForPackage(target)
        } else {
            val schemeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
            if (schemeIntent.resolveActivity(context.packageManager) != null) {
                schemeIntent
            } else {
                null
            }
        }

        if (intent != null) {
            downloadQRImage(context, imageUrl)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Thiết bị chưa cài đặt ứng dụng này!", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}