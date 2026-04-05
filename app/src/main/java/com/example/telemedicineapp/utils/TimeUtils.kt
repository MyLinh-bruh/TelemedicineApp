package com.example.telemedicineapp.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Data class dùng để hiển thị thanh chọn ngày (Horizontal Calendar)
data class DateItem(
    val fullDate: String,         // VD: "2026-04-06" (Dùng để query Firestore)
    val displayDate: String,      // VD: "06/04"
    val displayDayOfWeek: String  // VD: "Thứ 2"
)

object TimeUtils {

    // 1. Hàm lấy danh sách 7 ngày tới (Cho thanh chọn ngày)
    fun getNext7Days(): List<DateItem> {
        val list = mutableListOf<DateItem>()
        val today = LocalDate.now()

        for (i in 0..6) {
            val date = today.plusDays(i.toLong())

            val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM"))
            val fullDateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val dayOfWeek = when (date.dayOfWeek.value) {
                1 -> "Thứ 2"
                2 -> "Thứ 3"
                3 -> "Thứ 4"
                4 -> "Thứ 5"
                5 -> "Thứ 6"
                6 -> "Thứ 7"
                7 -> "Chủ nhật"
                else -> ""
            }

            val displayDay = if (i == 0) "Hôm nay" else dayOfWeek
            list.add(DateItem(fullDateStr, dateStr, displayDay))
        }
        return list
    }

    // 2. 🌟 HÀM ĐANG THIẾU: Chuyển giờ Local sang chuẩn UTC để lưu Firebase
    fun convertLocalToUtcString(date: String, time: String): String? {
        return try {
            // date: "2026-04-06", time: "07:30"
            val dateTimeStr = "$date $time"
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

            // Phân tích chuỗi thành LocalDateTime
            val localDateTime = LocalDateTime.parse(dateTimeStr, formatter)

            // Lấy múi giờ hiện tại của điện thoại (VD: GMT+7)
            val localZoneId = ZoneId.systemDefault()
            val zonedDateTime = localDateTime.atZone(localZoneId)

            // Chuyển sang múi giờ quốc tế UTC
            val utcDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)

            // Trả về định dạng ISO-8601 mà Firebase/Backend ưa chuộng
            utcDateTime.format(DateTimeFormatter.ISO_INSTANT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 3. Hàm bổ trợ: Chuyển UTC ngược lại Local để hiển thị (Nếu cần dùng sau này)
    fun convertUtcToLocalDisplay(utcString: String): String {
        return try {
            val instant = Instant.parse(utcString)
            val localZoneId = ZoneId.systemDefault()
            val localDateTime = instant.atZone(localZoneId)

            val formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")
            localDateTime.format(formatter)
        } catch (e: Exception) {
            e.printStackTrace()
            "Thời gian không hợp lệ"
        }
    }
    fun extractSlotFromUtc(utcString: String): String {
        // Giả sử utcString là "2026-04-06T07:30:00Z"
        // Trả về "07:30 - 08:30" (tùy theo logic độ dài ca khám của bạn)
        val instant = Instant.parse(utcString)
        val localDateTime = instant.atZone(ZoneId.systemDefault())
        val hour = localDateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

        // Ở đây bạn cần logic để biết ca đó kết thúc lúc nào,
        // ví dụ cộng thêm 1 tiếng:
        val endHour = localDateTime.plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"))
        return "$hour - $endHour"
    }
}
