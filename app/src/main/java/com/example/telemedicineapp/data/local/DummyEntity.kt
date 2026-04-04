package com.example.telemedicineapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dummy_table")
data class DummyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)