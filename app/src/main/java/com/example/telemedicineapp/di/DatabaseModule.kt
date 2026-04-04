package com.example.telemedicineapp.di

import android.content.Context
import androidx.room.Room
import com.example.telemedicineapp.data.local.TelemedicineDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TelemedicineDatabase {
        return Room.databaseBuilder(
            context,
            TelemedicineDatabase::class.java,
            "telemedicine_db"
        ).build()
    }
}