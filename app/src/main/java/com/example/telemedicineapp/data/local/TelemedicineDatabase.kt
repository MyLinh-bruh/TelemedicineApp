package com.example.telemedicineapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// ĐÃ SỬA: Thêm DummyEntity::class vào danh sách entities
@Database(entities = [DummyEntity::class], version = 1, exportSchema = false)
abstract class TelemedicineDatabase : RoomDatabase() {
    // Nơi khai báo các bảng dữ liệu sau này
}